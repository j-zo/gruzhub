import asyncio
from typing import Callable
from fastapi import HTTPException, status
from sqlalchemy import ColumnExpressionArgument, select, or_
from sqlalchemy.ext.asyncio import AsyncSession
import aiohttp
from urllib.parse import quote

from src.constants import MODE

from .enums import LogLevel
from . import models, schemas
from src.util.time_helper import TimeHelper
from .models import LogItem


class LoggerService:
    _prefix: str

    def __init__(self, prefix: str):
        self._prefix = prefix

    async def get_log_items(
        self,
        db: AsyncSession,
        get_log_items_request: schemas.GetLogItemsRequest,
    ) -> list[schemas.LogItem]:
        where_statements = self._create_where_statements_for_get_items(
            get_log_items_request,
        )

        statement = (
            select(models.LogItem)
            .where(*where_statements)
            .limit(get_log_items_request.limit)
            .offset(get_log_items_request.offset)
        )

        result = await db.execute(statement)
        log_items = list(result.scalars())
        return [schemas.LogItem(**vars(item)) for item in log_items]

    async def log(
        self,
        db: AsyncSession,
        request_id: str,
        user_id: int | None,
        log_level: LogLevel,
        text: str,
        data: str | None = None,
    ):
        log_item = models.LogItem(
            level=log_level,
            request_id=request_id,
            user_id=user_id,
            text=f'[{self._prefix}] {text}\n{data if data else ""}',
            time=TimeHelper.now_ms(),
        )
        db.add(log_item)
        await db.commit()

        if log_level == LogLevel.ERROR:
            await self._send_error_message_to_admins(
                f"Backend error happened: {text[0:3500]}",
            )

    async def log_error(
        self,
        db: AsyncSession,
        request_id: str,
        user_id: int | None,
        text: str,
        error: Exception,
    ):
        await self.log(db, request_id, user_id, LogLevel.ERROR, text, str(error))

    def _create_where_statements_for_get_items(
        self,
        get_log_items_request: schemas.GetLogItemsRequest,
    ):
        where_statements: list[ColumnExpressionArgument] = []

        if get_log_items_request.text_query:
            where_statements.append(
                models.LogItem.text.ilike(f"%{get_log_items_request.text_query}%"),
            )

        if get_log_items_request.request_id_query:
            where_statements.append(
                models.LogItem.request_id.ilike(
                    f"%{get_log_items_request.request_id_query}%",
                ),
            )

        if get_log_items_request.user_id:
            where_statements.append(
                models.LogItem.user_id == get_log_items_request.user_id,
            )

        log_level_statements: list[ColumnExpressionArgument] = []

        match get_log_items_request.log_level:
            case LogLevel.DEBUG:
                log_level_statements.append(models.LogItem.level == LogLevel.DEBUG)
                log_level_statements.append(models.LogItem.level == LogLevel.INFO)
                log_level_statements.append(models.LogItem.level == LogLevel.WARN)
                log_level_statements.append(models.LogItem.level == LogLevel.ERROR)
            case LogLevel.INFO:
                log_level_statements.append(models.LogItem.level == LogLevel.INFO)
                log_level_statements.append(models.LogItem.level == LogLevel.WARN)
                log_level_statements.append(models.LogItem.level == LogLevel.ERROR)
            case LogLevel.WARN:
                log_level_statements.append(models.LogItem.level == LogLevel.WARN)
                log_level_statements.append(models.LogItem.level == LogLevel.ERROR)
            case LogLevel.ERROR:
                log_level_statements.append(models.LogItem.level == LogLevel.ERROR)
            case _:
                raise HTTPException(
                    detail="Unsupported LogLevel type",
                    status_code=status.HTTP_400_BAD_REQUEST,
                )

        where_statements.append(or_(*log_level_statements))

        return where_statements

    async def _send_error_message_to_admins(self, message: str):
        if MODE == "test":
            return

        request_url = f"https://api.click-chat.ru/api/dialog/send-direct-text-message?websiteUuid=4c895200-0726-4cac-bf67-04b1b93cd39a&chatUuid=dca10766-23d2-43c8-82c2-b77a2bfa101a&text={quote(message)}&isMarkdown=false"
        async with aiohttp.ClientSession() as session, await session.get(request_url):
            pass


class LoggerServiceCleaner:
    _ONE_HOUR_IN_SECONDS = 3600
    _ONE_MINUTE_IN_SECONDS = 60

    _LOG_LIMIT_ITEMS_COUNT = 1000000

    _session_maker: Callable[[], AsyncSession]

    def __init__(self, session_maker: Callable[[], AsyncSession]):
        self._session_maker = session_maker

    async def start_cleaning(self):
        asyncio.create_task(self._clean_old_logs())
        asyncio.create_task(self._clean_too_much_logs())

    async def _clean_old_logs(self):
        db_session = self._session_maker()
        ago_30_days_ms = TimeHelper.now_ms() - 30 * 24 * 60 * 60 * 1000

        statement = select(LogItem).where(LogItem.time < ago_30_days_ms)
        items_to_delete = list(await db_session.execute(statement))
        [item.delete() for item in items_to_delete]

        await db_session.commit()
        await db_session.close()

        await asyncio.sleep(self._ONE_HOUR_IN_SECONDS)
        asyncio.create_task(self._clean_old_logs())

    async def _clean_too_much_logs(self):
        db_session = self._session_maker()

        statement = (
            select(LogItem)
            .order_by(LogItem.id.desc())
            .offset(self._LOG_LIMIT_ITEMS_COUNT)
        )
        items_to_delete = list(await db_session.execute(statement))
        [item.delete() for item in items_to_delete]

        await db_session.commit()
        await db_session.close()

        await asyncio.sleep(self._ONE_MINUTE_IN_SECONDS)
        asyncio.create_task(self._clean_too_much_logs())
