from src.telegram.listener import TelegramListener
from src.telegram.services.listener_service import TelegramListenerService
from src.telegram.services.telegram_service import TelegramService
from src.tools.database.dependencies import DatabaseDI
from src.tools.logger.dependencies import LoggerDI

telegram_service = TelegramService(
    LoggerDI.get_logger_service("TelegramModule"),
    DatabaseDI.get_database(),
)
telegram_listener_service = TelegramListenerService(telegram_service)
telegram_listener = TelegramListener(telegram_listener_service)


class TelegramDI:
    @staticmethod
    def get_telegram_listener() -> TelegramListener:
        return telegram_listener

    @staticmethod
    def get_telegram_listener_service() -> TelegramListenerService:
        return telegram_listener_service

    @staticmethod
    def get_telegram_service() -> TelegramService:
        return telegram_service
