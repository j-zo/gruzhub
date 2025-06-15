from src.orders.messages.repository import OrderMessagesRepository
from src.orders.messages.service import OrderMessagesService
from src.orders.orders.dependencies import OrdersDI
from src.tools.files.dependencies import FilesDI
from src.users.dependencies import UsersDI


order_messages_repository = OrderMessagesRepository()
order_messages_service = OrderMessagesService(
    UsersDI.get_users_service(),
    OrdersDI.get_orders_data_service(),
    order_messages_repository,
    FilesDI.get_files_service(),
)


class OrderMessagesDI:
    @staticmethod
    def get_order_messages_service() -> OrderMessagesService:
        return order_messages_service
