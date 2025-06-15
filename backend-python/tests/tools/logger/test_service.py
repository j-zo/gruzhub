from sqlalchemy.ext.asyncio import AsyncSession
import random
import pytest
from faker import Faker
from uuid import uuid4

from src.tools.logger.dependencies import LoggerDI
from src.tools.logger.enums import LogLevel
from src.tools.logger import schemas


fake = Faker()

log_request_id = str(uuid4())
error_log_request_id = str(uuid4())


class TestLoggerService:
    @staticmethod
    @pytest.mark.asyncio()
    async def test_create_log_item(db_connection: AsyncSession):
        logger_service = LoggerDI.get_logger_service("test_create_log_item")
        await TestLoggerService._create_three_log_items(db_connection, logger_service)

    @staticmethod
    @pytest.mark.asyncio()
    async def test_create_error_item(db_connection: AsyncSession):
        logger_service = LoggerDI.get_logger_service("test_create_log_item")

        await logger_service.log_error(
            db_connection,
            request_id=error_log_request_id,
            user_id=random.randint(0, 1000000),
            text=fake.text(),
            error=Exception("test exception"),
        )

    @staticmethod
    @pytest.mark.asyncio()
    async def test_get_log_items(db_connection: AsyncSession):
        logger_service = LoggerDI.get_logger_service("test_create_log_item")

        await TestLoggerService._create_three_log_items(db_connection, logger_service)

        log_items = await logger_service.get_log_items(
            db_connection,
            schemas.GetLogItemsRequest(
                limit=100,
                offset=0,
                log_level=LogLevel.DEBUG.value,
                text_query="test",
            ),
        )

        EXPECTED_LOG_ITEMS_COUNT = 3
        assert len(log_items) >= EXPECTED_LOG_ITEMS_COUNT

        for log_item in log_items:
            assert log_item.id
            assert log_item.level
            assert log_item.request_id
            assert log_item.user_id
            assert log_item.text
            assert log_item.time

        # Valid type
        error_log_items = await logger_service.get_log_items(
            db_connection,
            schemas.GetLogItemsRequest(
                limit=100,
                offset=0,
                log_level=LogLevel.ERROR.value,
            ),
        )
        assert len(
            [item for item in error_log_items if item.level == LogLevel.ERROR],
        ) == len(error_log_items)

    @staticmethod
    async def _create_three_log_items(db_connection: AsyncSession, logger_service):
        await logger_service.log(
            db_connection,
            request_id=log_request_id,
            user_id=random.randint(0, 1000000),
            log_level=LogLevel.DEBUG,
            text=fake.text(),
            data=str({"test": fake.text()}),
        )

        await logger_service.log(
            db_connection,
            request_id=log_request_id,
            user_id=random.randint(0, 1000000),
            log_level=LogLevel.INFO,
            text=fake.text(),
            data=str({"test": fake.text()}),
        )

        await logger_service.log(
            db_connection,
            request_id=log_request_id,
            user_id=random.randint(0, 1000000),
            log_level=LogLevel.WARN,
            text=fake.text(),
            data=str({"test": fake.text()}),
        )
