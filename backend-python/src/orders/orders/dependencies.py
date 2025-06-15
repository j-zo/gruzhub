from src.orders.orders.services.background_service import OrdersBackgroundService
from src.tools.database.dependencies import DatabaseDI
from src.tools.logger.dependencies import LoggerDI
from src.users.dependencies import UsersDI
from src.addresses.addresses.dependencies import AddressesDI
from src.orders.auto.dependencies import AutoDI
from src.orders.orders.services.data_service import OrdersDataService
from src.orders.orders.services.workflow_service import OrdersWorkflowService
from src.orders.orders.repository import OrdersRepository
from src.telegram.dependencies import TelegramDI

orders_repository = OrdersRepository()
orders_data_service = OrdersDataService(
    orders_repository,
    UsersDI.get_users_service(),
    AutoDI.get_auto_service(),
)
orders_workflow_service = OrdersWorkflowService(
    orders_repository,
    UsersDI.get_users_service(),
    AddressesDI.get_addresses_service(),
    AutoDI.get_auto_service(),
    TelegramDI.get_telegram_service(),
    DatabaseDI.get_database(),
    LoggerDI.get_logger_service("OrdersWorkflowService"),
)
orders_background_service = OrdersBackgroundService(
    orders_repository,
    orders_workflow_service,
    UsersDI.get_users_service(),
    LoggerDI.get_logger_service("OrdersBackgroundService"),
)


class OrdersDI:
    @staticmethod
    def get_orders_repository() -> OrdersRepository:
        return orders_repository

    @staticmethod
    def get_orders_data_service() -> OrdersDataService:
        return orders_data_service

    @staticmethod
    def get_orders_workflow_service() -> OrdersWorkflowService:
        return orders_workflow_service

    @staticmethod
    def get_orders_background_service() -> OrdersBackgroundService:
        return orders_background_service
