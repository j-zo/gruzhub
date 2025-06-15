from src.orders.tasks.service import TasksService
from src.orders.orders.dependencies import OrdersDI
from src.orders.auto.dependencies import AutoDI
from src.users.dependencies import UsersDI

tasks_service = TasksService(
    UsersDI.get_users_service(),
    OrdersDI.get_orders_data_service(),
    AutoDI.get_auto_service(),
)


class TasksDI:
    @staticmethod
    def get_tasks_service() -> TasksService:
        return tasks_service
