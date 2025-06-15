from src.users.repository import UsersRepository
from src.users.service import UsersService
from src.addresses.addresses.dependencies import AddressesDI
from src.telegram.dependencies import TelegramDI

users_repository = UsersRepository()
users_service = UsersService(
    users_repository,
    AddressesDI.get_addresses_service(),
    TelegramDI.get_telegram_service(),
)


class UsersDI:
    @staticmethod
    def get_users_repository() -> UsersRepository:
        return users_repository

    @staticmethod
    def get_users_service() -> UsersService:
        return users_service
