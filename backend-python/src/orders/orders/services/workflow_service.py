import asyncio
from fastapi import HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from telegram import InlineKeyboardButton, InlineKeyboardMarkup
from src.constants import APPLICATION_SERVER

from src.orders.orders.constants import TAKE_ORDER_PRICE_RUB
from src.orders.orders.enums import OrderStatus
from src.telegram.services.telegram_service import TelegramService
from src.tools.database.database import Database
from src.tools.database.dependencies import DatabaseDI
from src.tools.logger.enums import LogLevel
from src.tools.logger.service import LoggerService
from src.users.models import User
from src.util.time_helper import TimeHelper
from ..repository import OrdersRepository
from src.users.service import UsersService
from .. import models, schemas
from src.addresses.addresses.service import AddressesService
from src.orders.auto.service import AutoService
from src.orders.orders.commands.create_order_command import CreateOrderCommand
from src.users.enums import UserRole


class OrdersWorkflowService:
    def __init__(
        self,
        orders_repository: OrdersRepository,
        users_service: UsersService,
        addresses_service: AddressesService,
        auto_service: AutoService,
        telegram_service: TelegramService,
        database: Database,
        logger_service: LoggerService,
    ):
        self._orders_repository = orders_repository
        self._users_service = users_service
        self._addresses_service = addresses_service
        self._auto_service = auto_service
        self._telegram_service = telegram_service
        self._database = database
        self._logger_service = logger_service

    async def create_order(
        self,
        db: AsyncSession,
        authorization: str | None,
        request_id: str,
        create_order: schemas.CreateOrder,
    ) -> schemas.CreateOrderResponse | None:
        create_order_command = CreateOrderCommand(
            self._orders_repository,
            self._users_service,
            self._addresses_service,
            self._auto_service,
            self._telegram_service,
            self._database,
            self._logger_service,
        )
        return await create_order_command.create_order(
            db,
            authorization,
            request_id,
            create_order,
        )

    async def start_calculation_by_master(
        self,
        db: AsyncSession,
        authorization: str,
        request_id: str,
        order_id: int,
    ):
        authorized_user = await self._users_service.get_user_model_from_token(
            db,
            authorization,
        )

        assert authorized_user.address
        if authorized_user.role != UserRole.MASTER:
            raise HTTPException(
                status.HTTP_403_FORBIDDEN,
                detail="Order can be taken into work only by MASTER",
            )

        try:
            order_with_relations = await self._orders_repository.get_order_by_id(
                db,
                order_id,
            )
            order_for_update = await self._orders_repository.get_order_by_id(
                db,
                order_id,
                is_for_update=True,
                is_load_relations=False,
            )

            if (
                authorized_user.address.region_id
                != order_with_relations.address.region_id
            ):
                raise HTTPException(
                    status.HTTP_403_FORBIDDEN,
                    detail="Access to foreign region order",
                )

            if order_with_relations.status != OrderStatus.CREATED:
                raise HTTPException(
                    status.HTTP_400_BAD_REQUEST,
                    detail="Заказ уже взят в работу другим автосервисом",
                )

            if authorized_user.balance < TAKE_ORDER_PRICE_RUB:
                raise HTTPException(
                    status.HTTP_400_BAD_REQUEST,
                    detail="На балансе недостаточно средств, чтобы взять заказ в работу",
                )

            if authorized_user.id in [
                int(master_id)
                for master_id in order_for_update.declined_masters_ids.split(",")
                if master_id
            ]:
                raise HTTPException(
                    status.HTTP_400_BAD_REQUEST,
                    detail="Текущий автосервис не может взять этот заказ",
                )

            order_for_update.status = OrderStatus.CALCULATING
            order_for_update.last_status_update_time = TimeHelper.now_ms()
            order_for_update.master = authorized_user
            await self._users_service.decrease_user_balance_no_commit(
                db,
                authorized_user.id,
                TAKE_ORDER_PRICE_RUB,
            )

            order_status_change = models.OrderStatusChange()
            order_status_change.order = order_for_update
            order_status_change.new_status = OrderStatus.CALCULATING
            order_status_change.updated_at = TimeHelper.now_ms()
            order_status_change.updated_by = authorized_user
            order_status_change.master = order_with_relations.master
            await self._orders_repository.add_order_status_change(
                db,
                order_status_change,
                is_commit=False,
            )

            await db.commit()

        except Exception as e:
            await db.rollback()
            raise e

        asyncio.create_task(
            self._send_message_to_order_owners(
                request_id,
                order_for_update,
                f'Заказ #{order_for_update.id} принять в работу автосервисом "{authorized_user.name}" (номер +{authorized_user.phone})',
            ),
        )

    async def decline_order_master(
        self,
        db: AsyncSession,
        authorization: str,
        request_id: str,
        order_id: int,
        comment: str,
    ):
        authorized_user = await self._users_service.get_user_model_from_token(
            db,
            authorization,
        )

        await self._change_order_status_with_master_remove_and_refund(
            db,
            authorized_user,
            request_id,
            order_id,
            OrderStatus.CREATED,
            comment=comment,
        )

    async def send_for_confirmation_by_master(
        self,
        db: AsyncSession,
        authorization: str,
        request_id: str,
        order_id: int,
    ):
        authorized_user = await self._users_service.get_user_model_from_token(
            db,
            authorization,
        )
        order = await self._orders_repository.get_order_by_id(
            db,
            order_id,
        )

        if (
            authorized_user.id != order.master_id
            or order.status != OrderStatus.CALCULATING
        ):
            raise HTTPException(status.HTTP_403_FORBIDDEN)

        order.status = OrderStatus.REVIEWING
        order.last_status_update_time = TimeHelper.now_ms()

        order_status_change = models.OrderStatusChange()
        order_status_change.order = order
        order_status_change.new_status = OrderStatus.REVIEWING
        order_status_change.updated_at = TimeHelper.now_ms()
        order_status_change.updated_by = authorized_user
        order_status_change.master = order.master
        await self._orders_repository.add_order_status_change(
            db,
            order_status_change,
            is_commit=False,
        )

        await db.commit()

        asyncio.create_task(
            self._send_message_to_order_owners(
                request_id,
                order,
                f"Заказ #{order.id} отправлен на согласование. Подтвердите, что можно начинать работу",
            ),
        )

    async def accept_by_customer(
        self,
        db: AsyncSession,
        authorization: str,
        request_id: str,
        order_id: int,
    ):
        authorized_user = await self._users_service.get_user_model_from_token(
            db,
            authorization,
        )
        order = await self._orders_repository.get_order_by_id(
            db,
            order_id,
        )

        if (
            authorized_user.id not in (order.customer_id, order.driver_id)
            or order.status != OrderStatus.REVIEWING
        ):
            raise HTTPException(status.HTTP_403_FORBIDDEN)

        if not order.master:
            raise HTTPException(
                status.HTTP_500_INTERNAL_SERVER_ERROR,
                "Order does not have master",
            )

        order.status = OrderStatus.ACCEPTED
        order.last_status_update_time = TimeHelper.now_ms()

        order_status_change = models.OrderStatusChange()
        order_status_change.order = order
        order_status_change.new_status = OrderStatus.ACCEPTED
        order_status_change.updated_at = TimeHelper.now_ms()
        order_status_change.updated_by = authorized_user
        order_status_change.master = order.master
        await self._orders_repository.add_order_status_change(
            db,
            order_status_change,
            is_commit=False,
        )

        await db.commit()

        asyncio.create_task(
            self._send_telegram_message(
                request_id,
                order.id,
                order.master,
                f"Заказ #{order.id} согласован заказчиком. Можете приступать к работе",
            ),
        )

    async def complete_order(
        self,
        db: AsyncSession,
        authorization: str,
        request_id: str,
        order_id: int,
    ):
        authorized_user = await self._users_service.get_user_model_from_token(
            db,
            authorization,
        )
        order = await self._orders_repository.get_order_by_id(
            db,
            order_id,
        )

        if (
            authorized_user.id
            not in (
                order.customer_id,
                order.driver_id,
                order.master_id,
            )
            and authorized_user.role != UserRole.ADMIN
        ):
            raise HTTPException(status.HTTP_403_FORBIDDEN)

        order.status = OrderStatus.COMPLETED
        order.last_status_update_time = TimeHelper.now_ms()

        order_status_change = models.OrderStatusChange()
        order_status_change.order = order
        order_status_change.new_status = OrderStatus.COMPLETED
        order_status_change.updated_at = TimeHelper.now_ms()
        order_status_change.updated_by = authorized_user
        order_status_change.master = order.master
        await self._orders_repository.add_order_status_change(
            db,
            order_status_change,
            is_commit=False,
        )

        await db.commit()

        if authorized_user.role == UserRole.MASTER:
            asyncio.create_task(
                self._send_message_to_order_owners(
                    request_id,
                    order,
                    f"Заказ #{order.id} завершён",
                ),
            )

        if (
            authorized_user.role in (UserRole.CUSTOMER, UserRole.DRIVER)
            and order.master
        ):
            asyncio.create_task(
                self._send_telegram_message(
                    request_id,
                    order.id,
                    order.master,
                    f"Заказ #{order.id} завершён",
                ),
            )

    async def cancel_order(
        self,
        db: AsyncSession,
        authorization: str,
        request_id: str,
        order_id: int,
        comment: str,
    ):
        authorized_user = await self._users_service.get_user_model_from_token(
            db,
            authorization,
        )
        if authorized_user.role == UserRole.MASTER:
            raise HTTPException(status.HTTP_403_FORBIDDEN)

        await self._change_order_status_with_master_remove_and_refund(
            db,
            authorized_user,
            request_id,
            order_id,
            OrderStatus.CANCEL,
            comment=comment,
        )

    async def update_order_status_for_testing(
        self,
        db_conenction: AsyncSession,
        order_id: int,
        status: OrderStatus,
    ):
        await self._orders_repository.update_order_status_for_testing(
            db_conenction,
            order_id,
            status,
        )

    async def _change_order_status_with_master_remove_and_refund(
        self,
        db: AsyncSession,
        authorized_user: User,
        request_id: str,
        order_id: int,
        new_status: OrderStatus,
        *,
        comment: str,
    ):
        if new_status not in (OrderStatus.CREATED, OrderStatus.CANCEL):
            raise HTTPException(
                status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Unsupported status: {str(new_status)}",
            )

        try:
            order_for_update = await self._orders_repository.get_order_by_id(
                db,
                order_id,
                is_for_update=True,
                is_load_relations=False,
            )
            order_master_id = order_for_update.master_id

            if (
                authorized_user.id
                not in (order_for_update.customer_id, order_for_update.driver_id)
                and authorized_user.role != UserRole.ADMIN
            ):
                raise HTTPException(status.HTTP_403_FORBIDDEN)

            # Customer decided to change master
            if new_status == OrderStatus.CREATED:
                if order_for_update.master_id is None:
                    raise HTTPException(
                        status.HTTP_400_BAD_REQUEST,
                        detail="У заказа нет исполнителя",
                    )

                declined_masters_ids = [
                    int(master_id)
                    for master_id in order_for_update.declined_masters_ids.split(",")
                    if master_id
                ]

                if order_for_update.master_id not in declined_masters_ids:
                    declined_masters_ids.append(order_for_update.master_id)
                    order_for_update.declined_masters_ids = ",".join(
                        str(master_id)
                        for master_id in declined_masters_ids
                        if master_id
                    )

            order_for_update.master = None
            order_for_update.status = new_status
            order_for_update.last_status_update_time = TimeHelper.now_ms()

            if order_master_id:
                await self._users_service.increase_user_balance_no_commit(
                    db,
                    order_master_id,
                    TAKE_ORDER_PRICE_RUB,
                )

            order_status_change = models.OrderStatusChange()
            order_status_change.order = order_for_update
            order_status_change.new_status = new_status
            order_status_change.updated_at = TimeHelper.now_ms()
            order_status_change.updated_by = authorized_user
            order_status_change.master = None
            order_status_change.comment = comment
            await self._orders_repository.add_order_status_change(
                db,
                order_status_change,
                is_commit=False,
            )

            await db.commit()
        except Exception as e:
            await db.rollback()
            raise e

        if order_master_id:
            order_master = await self._users_service.get_user_by_id(
                db,
                order_master_id,
            )

            asyncio.create_task(
                self._send_telegram_message(
                    request_id,
                    order_for_update.id,
                    order_master,
                    f"Заказ #{order_for_update.id} отменен. Средства возвращены вам на баланс",
                ),
            )

        admin_message = (
            f"Заказ #{order_for_update.id} вернулся в статус поиск автосервиса"
            if new_status == OrderStatus.CREATED
            else f"Заказ #{order_for_update.id} отменён"
        )
        admins = await self._users_service.get_admins(db)
        for admin in admins:
            if admin.telegram_id:
                try:
                    await self._telegram_service.send_message(
                        telegram_chat_id=admin.telegram_id,
                        message=admin_message,
                    )
                except Exception as e:
                    await self._logger_service.log(
                        db,
                        request_id,
                        None,
                        LogLevel.ERROR,
                        str(e),
                    )

    async def _send_message_to_order_owners(
        self,
        request_id: str,
        order: models.Order,
        message: str,
    ):
        if order.driver:
            await self._send_telegram_message(
                request_id,
                order.id,
                order.driver,
                message,
            )
        if order.customer:
            await self._send_telegram_message(
                request_id,
                order.id,
                order.customer,
                message,
            )

    async def _send_telegram_message(
        self,
        request_id: str,
        order_id: int,
        user: User,
        message: str,
    ):
        if user.telegram_id or user.user_chats_codes:
            db = DatabaseDI.get_database().get_async_session()

            try:
                auth = await self._users_service.get_user_access_no_auth(
                    db,
                    user.id,
                )
                auth_link = f"{APPLICATION_SERVER}?userId={auth.id}&accessToken={auth.access_token}&orderId={order_id}"

                if user.telegram_id:
                    await self._telegram_service.send_message(
                        telegram_chat_id=user.telegram_id,
                        message=message,
                        reply_markup=InlineKeyboardMarkup(
                            [
                                [
                                    InlineKeyboardButton(
                                        text="Посмотреть",
                                        url=auth_link,
                                    ),
                                ],
                            ],
                        ),
                    )

                if user.user_chats_codes:
                    for chat_code in user.user_chats_codes.split(","):
                        chat = await self._telegram_service.get_telegram_chat_by_uuid(
                            db,
                            chat_code,
                        )

                        if chat:
                            await self._telegram_service.send_message(
                                telegram_chat_id=chat.telegram_chat_id,
                                message=message,
                                reply_markup=InlineKeyboardMarkup(
                                    [
                                        [
                                            InlineKeyboardButton(
                                                text="Посмотреть",
                                                url=auth_link,
                                            ),
                                        ],
                                    ],
                                ),
                            )
            except Exception as e:
                await self._logger_service.log(
                    db,
                    request_id,
                    None,
                    LogLevel.ERROR,
                    str(e),
                )

            await db.close()
