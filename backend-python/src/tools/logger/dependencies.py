from src.tools.database.dependencies import DatabaseDI
from src.tools.logger.service import LoggerService, LoggerServiceCleaner


class LoggerDI:
    @staticmethod
    def get_logger_service(prefix: str):
        return LoggerService(prefix)

    @staticmethod
    def get_logger_service_cleaner():
        return LoggerServiceCleaner(DatabaseDI.get_database().get_sessionmaker())
