from src.users.dependencies import UsersDI
from . import service

auto_service = service.AutoService(UsersDI.get_users_service())


class AutoDI:
    @staticmethod
    def get_auto_service() -> service.AutoService:
        return auto_service
