from src.orders.notify.service import OrdersNotificationsService
from src.orders.orders.dependencies import OrdersDI
from src.telegram.dependencies import TelegramDI
from src.tools.database.dependencies import DatabaseDI
from src.tools.logger.dependencies import LoggerDI
from src.users.dependencies import UsersDI


orders_notifications_service = OrdersNotificationsService(
    LoggerDI.get_logger_service("OrdersNotificationService"),
    OrdersDI.get_orders_repository(),
    DatabaseDI.get_database(),
    TelegramDI.get_telegram_service(),
    UsersDI.get_users_service(),
)


class OrdersNotificationDI:
    @staticmethod
    def get_orders_notifications_service() -> OrdersNotificationsService:
        return orders_notifications_service
