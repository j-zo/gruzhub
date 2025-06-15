from sqlalchemy.ext.asyncio import AsyncSession
from src.orders.orders.repository import OrdersRepository
from src.orders.orders.services.workflow_service import OrdersWorkflowService
from src.tools.logger.service import LoggerService
from src.users.constants import ADMIN_EMAIL
from src.users.enums import UserRole
from src.users.service import UsersService


class OrdersBackgroundService:
    def __init__(
        self,
        orders_repository: OrdersRepository,
        orders_workflow_service: OrdersWorkflowService,
        users_service: UsersService,
        logger_service: LoggerService,
    ):
        self._orders_repository = orders_repository
        self._orders_workflow_service = orders_workflow_service
        self._users_service = users_service
        self._logger_service = logger_service

    async def initialize(self, db: AsyncSession):
        await self._users_service.create_admin_user_if_not_exist(db)

    async def cancel_old_new_orders(self, db: AsyncSession):
        try:
            DAYS_2_AGO_MS = 2 * 24 * 60 * 60 * 1000
            outdated_orders = (
                await self._orders_repository.get_orders_created_longer_than(
                    db,
                    DAYS_2_AGO_MS,
                )
            )

            admin_user = await self._users_service.get_user_by_email(
                db,
                ADMIN_EMAIL,
                UserRole.ADMIN,
            )
            if not admin_user:
                raise Exception("Admin user is not initialized")

            admin_access = await self._users_service.get_user_access_no_auth(
                db,
                admin_user.id,
            )

            for outdated_order in outdated_orders:
                try:
                    await self._orders_workflow_service.cancel_order(
                        db,
                        admin_access.access_token,
                        "background",
                        outdated_order.id,
                        "Заказ не был взят в работу за 48 часов",
                    )
                except Exception as e:
                    await self._logger_service.log_error(
                        db,
                        "background",
                        None,
                        f"Canceling outdated order #{outdated_order.id}",
                        e,
                    )
                    await db.rollback()
        except Exception as e:
            await self._logger_service.log_error(
                db,
                "background",
                None,
                "Canceling outdated orders",
                e,
            )
            await db.rollback()

    async def completed_old_orders(self, db: AsyncSession):
        try:
            MONTH_AGO = 31 * 24 * 60 * 60 * 1000
            outdated_orders = (
                await self._orders_repository.get_orders_changed_longer_than(
                    db,
                    MONTH_AGO,
                )
            )

            admin_user = await self._users_service.get_user_by_email(
                db,
                ADMIN_EMAIL,
                UserRole.ADMIN,
            )
            if not admin_user:
                raise Exception("Admin user is not initialized")

            admin_access = await self._users_service.get_user_access_no_auth(
                db,
                admin_user.id,
            )

            for outdated_order in outdated_orders:
                try:
                    await self._orders_workflow_service.complete_order(
                        db,
                        authorization=admin_access.access_token,
                        request_id="no_request_id",
                        order_id=outdated_order.id,
                    )
                except Exception as e:
                    await self._logger_service.log_error(
                        db,
                        "background",
                        None,
                        f"Completing outdated order #{outdated_order.id}",
                        e,
                    )
                    await db.rollback()
        except Exception as e:
            await self._logger_service.log_error(
                db,
                "background",
                None,
                "Completing outdated orders",
                e,
            )
            await db.rollback()