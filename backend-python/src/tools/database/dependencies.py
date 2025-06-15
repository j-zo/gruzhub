from src.tools.database.database import Database

database = Database()


class DatabaseDI:
    @staticmethod
    def get_database() -> Database:
        return database
