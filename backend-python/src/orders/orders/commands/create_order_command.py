import asyncio
from sqlalchemy.ext.asyncio import AsyncSession
from telegram import InlineKeyboardButton, InlineKeyboardMarkup
from src.constants import APPLICATION_SERVER
from src.orders.auto.enums import AutoType

from src.orders.orders.repository import OrdersRepository
from src.telegram.services.telegram_service import TelegramService
from src.tools.database.database import Database
from src.tools.logger.enums import LogLevel
from src.tools.logger.service import LoggerService
from src.users.service import UsersService
from src.orders.orders import models, schemas
from src.users.models import User
from src.users.enums import UserRole
from src.addresses.addresses.service import AddressesService
from src.addresses.addresses import models as address_models, schemas as address_schemas
from src.orders.auto.models import Auto
from src.util.time_helper import TimeHelper
from src.orders.auto.service import AutoService
from src.orders.orders.enums import OrderStatus
from src.users import schemas as users_schemas


class CreateOrderCommand:
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
        authorized_user: User | None = None
        if authorization:
            authorized_user = await self._users_service.get_user_model_from_token(
                db,
                authorization,
            )

        dublicated_order_response = await self._get_response_if_dublicated_order(
            db,
            create_order,
        )
        if dublicated_order_response:
            return dublicated_order_response

        driver = await self._get_or_create_and_validate_driver_if_present_in_order(
            db,
            create_order,
            authorized_user,
        )
        customer = await self._get_customer_if_authorized(authorized_user)
        address = await self._create_address(db, create_order)
        autos = await self._get_or_create_autos(
            db,
            create_order,
            driver=driver,
            customer=customer,
        )

        # add order
        order = models.Order()
        order.guarantee_uuid = create_order.guarantee_uuid
        order.customer = customer
        order.driver = driver
        order.created_at = TimeHelper.now_ms()
        order.updated_at = TimeHelper.now_ms()
        order.address = address
        order.autos = autos
        order.status = OrderStatus.CREATED
        order.last_status_update_time = TimeHelper.now_ms()
        order.description = create_order.description
        order.notes = create_order.notes
        order.declined_masters_ids = ""
        order.is_need_evacuator = create_order.is_need_evacuator
        order.is_need_mobile_team = create_order.is_need_mobile_team
        order.urgency = create_order.urgency
        order = await self._orders_repository.create_order(db, order)

        # add order status change
        order_status_change = models.OrderStatusChange()
        order_status_change.order = order
        order_status_change.new_status = OrderStatus.CREATED
        order_status_change.updated_at = TimeHelper.now_ms()
        order_status_change.updated_by = authorized_user if authorized_user else driver
        await self._orders_repository.add_order_status_change(
            db,
            order_status_change,
            is_commit=True,
        )

        asyncio.create_task(
            self._send_notification_to_region_masters(request_id, order.id),
        )

        if driver and not authorized_user:
            return self._get_response_with_driver_access_token(order, driver)

        return schemas.CreateOrderResponse(order_id=order.id)

    async def _get_response_if_dublicated_order(
        self,
        db: AsyncSession,
        create_order: schemas.CreateOrder,
    ) -> schemas.CreateOrderResponse | None:
        dublicated_order = await self._orders_repository.get_order_by_guarantee_id(
            db,
            create_order.guarantee_uuid,
        )

        if dublicated_order and dublicated_order.driver_id:
            driver = await self._users_service.get_user_by_id(
                db,
                dublicated_order.driver_id,
            )
            return self._get_response_with_driver_access_token(dublicated_order, driver)

        return None

    def _get_response_with_driver_access_token(
        self,
        order: models.Order,
        driver: User,
    ) -> schemas.CreateOrderResponse:
        return schemas.CreateOrderResponse(
            order_id=order.id,
            driver_id=driver.id,
            access_token=self._users_service.generate_access_token(driver),
        )

    async def _get_or_create_and_validate_driver_if_present_in_order(
        self,
        db: AsyncSession,
        create_order: schemas.CreateOrder,
        authorized_user: User | None = None,
    ) -> User:
        # case if driver creates second order
        if authorized_user and authorized_user.role == UserRole.DRIVER:
            if (
                authorized_user.phone != create_order.driver_phone
                or authorized_user.name != create_order.driver_name
            ):
                authorized_user.phone = create_order.driver_phone
                if create_order.driver_name:
                    authorized_user.name = create_order.driver_name

                await self._users_service.update(
                    db,
                    authorized_user,
                    users_schemas.UpdateUserRequest(**authorized_user.to_dict()),
                )

            return await self._users_service.get_user_by_id(db, authorized_user.id)

        # case if anonymous driver
        if create_order.driver_phone and create_order.driver_name:
            driver_by_phone = await self._users_service.get_user_by_phone(
                db,
                create_order.driver_phone,
                UserRole.DRIVER,
            )

            if driver_by_phone:
                return driver_by_phone

            return await self._users_service.create_user(
                db,
                create_order.driver_name,
                create_order.driver_phone,
                create_order.driver_email,
                UserRole.DRIVER,
            )

        raise Exception("Driver should be present in any order")

    async def _get_customer_if_authorized(
        self,
        authorized_user: User | None,
    ) -> User | None:
        if authorized_user and authorized_user.role == UserRole.CUSTOMER:
            return authorized_user

        return None

    async def _create_address(
        self,
        db: AsyncSession,
        create_order: schemas.CreateOrder,
    ) -> address_models.Address:
        address = address_schemas.Address(
            region_id=create_order.region_id,
            city=create_order.city,
            street=create_order.street,
        )
        return await self._addresses_service.create_address(db, address)

    async def _get_or_create_autos(
        self,
        db: AsyncSession,
        create_order: schemas.CreateOrder,
        *,
        driver: User | None = None,
        customer: User | None = None,
    ) -> list[Auto]:
        autos: list[Auto] = []

        for order_auto in create_order.autos:
            if order_auto.auto_id:
                auto_model = await self._auto_service.get_auto_by_id(
                    db,
                    order_auto.auto_id,
                )
            else:
                auto_model = Auto()
                auto_model.brand = order_auto.brand
                auto_model.model = order_auto.model
                auto_model.vin = order_auto.vin
                auto_model.number = order_auto.number
                auto_model.type = order_auto.type
                auto_model.driver = driver
                auto_model.customer = customer
                auto_model = await self._auto_service.create_auto(
                    db,
                    auto_model,
                )

            autos.append(auto_model)

        return autos

    async def _send_notification_to_region_masters(
        self,
        request_id: str,
        order_id: int,
    ):
        db = self._database.get_async_session()

        order = await self._orders_repository.get_order_by_id(db, order_id)
        masters = await self._users_service.get_masters_with_telegram_in_region(
            db,
            order.address.region_id,
        )
        admins = await self._users_service.get_admins(db)

        message = self._get_created_order_message(order)

        for master_or_admin in masters + admins:
            if master_or_admin.telegram_id:
                master_auth = await self._users_service.get_user_access_no_auth(
                    db,
                    master_or_admin.id,
                )
                auth_link = f"{APPLICATION_SERVER}?userId={master_auth.id}&accessToken={master_auth.access_token}&orderId={order.id}"

                try:
                    await self._telegram_service.send_message(
                        telegram_chat_id=master_or_admin.telegram_id,
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

                    if master_or_admin.user_chats_codes:
                        for chat_code in master_or_admin.user_chats_codes.split(","):
                            chat = (
                                await self._telegram_service.get_telegram_chat_by_uuid(
                                    db,
                                    chat_code,
                                )
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

    def _get_created_order_message(self, order: models.Order) -> str:
        message = f"Создан заказ #{order.id} в регионе {order.address.region.name}"

        for auto in order.autos:
            if auto.type == AutoType.TRUCK:
                message += "\n\nГрузовик"
                message += f"\nМарка: {auto.brand}"

                if {auto.model}:
                    message += f"\nМодель: {auto.model if auto.model else '-'}"

            if auto.type == AutoType.TRAILER and auto.model:
                message += "\n\nПрицеп"
                message += f"\nТип: {auto.model if auto.model else '-'}"

        message += f"\n\n{order.description}"

        if order.is_need_evacuator or order.is_need_mobile_team:
            message += "\n"

            if order.is_need_evacuator:
                message += "\n- Требуется эвакуатор"
            if order.is_need_evacuator:
                message += "\n- Требуется выездная бригада"

        message += f"\n\nСрочность: {order.urgency}"

        message += f"\n\nДля просмотра - https://app.gruzhub.ru"

        return message
