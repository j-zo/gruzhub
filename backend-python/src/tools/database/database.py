from typing import AsyncIterator, Callable
from sqlalchemy import Engine, create_engine
from sqlalchemy.ext.asyncio import (
    create_async_engine,
    AsyncSession,
    AsyncAttrs,
    async_sessionmaker,
    AsyncEngine,
)
from sqlalchemy.orm import DeclarativeBase, sessionmaker, Session

from src.tools.database.schemas import DatabaseParams
from src.tools.database.constants import (
    DATABASE_ADDRESS,
    DATABASE_NAME,
    DATABASE_PASSWORD,
    DATABASE_PORT,
    DATABASE_USERNAME,
    MODE,
)


class Base(AsyncAttrs, DeclarativeBase):
    pass


class Database:
    _async_engine: AsyncEngine
    _sync_engine: Engine

    _async_sessionmaker: async_sessionmaker[AsyncSession]
    _sync_sessionmaker: Callable[[], Session]

    def __init__(self, params: DatabaseParams | None = None) -> None:
        if not params:
            params = self._get_db_params_from_env()

        async_database_url = f"postgresql+asyncpg://{params.username}:{params.password}@{params.address}:{params.port}/{params.db_name}"
        sync_database_url = f"postgresql+psycopg://{params.username}:{params.password}@{params.address}:{params.port}/{params.db_name}"

        self._async_engine = create_async_engine(
            async_database_url,
            echo=False,
            pool_size=50,
            max_overflow=10,
        )
        self._sync_engine = create_engine(
            sync_database_url,
            echo=False,
            pool_size=50,
            max_overflow=10,
        )

        self._async_sessionmaker = async_sessionmaker(
            self._async_engine,
            expire_on_commit=False,
        )
        self._sync_sessionmaker = sessionmaker(
            self._sync_engine,
            expire_on_commit=False,
        )

    async def get_connection_per_request(self) -> AsyncIterator[AsyncSession]:
        async with self._async_sessionmaker() as session:
            yield session

    def get_sessionmaker(self) -> async_sessionmaker[AsyncSession]:
        return self._async_sessionmaker

    def get_async_session(self) -> AsyncSession:
        return self._async_sessionmaker()

    def get_sync_session(self) -> Session:
        return self._sync_sessionmaker()

    def get_async_engine(self):
        return self._async_engine

    def _get_db_params_from_env(self) -> DatabaseParams:
        return DatabaseParams(
            mode=MODE,
            username=DATABASE_USERNAME,
            password=DATABASE_PASSWORD,
            address=DATABASE_ADDRESS,
            port=DATABASE_PORT,
            db_name=DATABASE_NAME,
        )
