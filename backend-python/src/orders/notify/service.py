from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from src.orders.orders.models import Order
from src.telegram.services.telegram_service import TelegramService
from src.tools.logger.enums import LogLevel
from src.users.service import UsersService
from . import models
from src.orders.orders.repository import OrdersRepository
from src.tools.database.database import Database
from src.tools.logger.service import LoggerService


class OrdersNotificationsService:
    def __init__(
        self,
        logger_service: LoggerService,
        orders_repository: OrdersRepository,
        database: Database,
        telegram_service: TelegramService,
        users_service: UsersService,
    ):
        self._logger_service = logger_service
        self._orders_repository = orders_repository
        self._database = database
        self._telegram_service = telegram_service
        self._users_service = users_service

    async def send_notifications(self, db: AsyncSession):
        try:
            await self._notify_about_old_created_orders(db)
        except Exception as e:
            print(str(e))

    async def _notify_about_old_created_orders(self, db: AsyncSession):
        try:
            MINS_15_AGO_MS = 15 * 60 * 1000
            pending_orders = (
                await self._orders_repository.get_orders_created_longer_than(
                    db,
                    MINS_15_AGO_MS,
                )
            )

            admins = await self._users_service.get_admins(db)

            for order in pending_orders:
                try:
                    notification = await self._get_order_status_notification(db, order)
                    if not notification:
                        old_order_notification = models.OldOrderNotification()
                        old_order_notification.order = order
                        old_order_notification.order_last_status_change_time = (
                            order.last_status_update_time
                        )
                        db.add(old_order_notification)
                        await db.commit()

                        for admin in admins:
                            if admin.telegram_id:
                                await self._telegram_service.send_message(
                                    telegram_chat_id=admin.telegram_id,
                                    message=f'Заказ #{order.id} находится в статусе "новая заявка" дольше 15 минут',
                                )
                except Exception as e:
                    print(str(e))
                    await self._logger_service.log(
                        db,
                        "no_request_id",
                        None,
                        LogLevel.ERROR,
                        str(e),
                    )
                    await db.rollback()
        except Exception as e:
            print(str(e))
            await self._logger_service.log(
                db,
                "no_request_id",
                None,
                LogLevel.ERROR,
                str(e),
            )
            await db.rollback()

    async def _get_order_status_notification(
        self,
        db: AsyncSession,
        order: Order,
    ) -> models.OldOrderNotification | None:
        return (
            await db.execute(
                select(
                    models.OldOrderNotification,
                )
                .filter(
                    models.OldOrderNotification.order_id == order.id,
                    models.OldOrderNotification.order_last_status_change_time
                    == order.last_status_update_time,
                )
                .limit(1),
            )
        ).scalar_one_or_none()
