import asyncio
import pytest_asyncio
from httpx import AsyncClient

from src.main_app import create_app
from src.tools.database.dependencies import DatabaseDI


@pytest_asyncio.fixture(scope="session", autouse=True)
def event_loop():
    """
    Create an instance of the default event loop for each test case.
    Otherwise SQLAchemy will throw error in tests "attached to a different loop"
    """
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


@pytest_asyncio.fixture()
async def http_client():
    async with AsyncClient(app=create_app(), base_url="http://localhost") as client:
        yield client


@pytest_asyncio.fixture()
async def db_connection():
    database = DatabaseDI.get_database()

    db_connection = database.get_async_session()

    try:
        yield db_connection
    finally:
        await db_connection.close()
