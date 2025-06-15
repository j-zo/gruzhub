import hashlib
import hmac

from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from src.constants import MODE, TELEGRAM_BOT_TOKEN
from src.telegram import models, schemas
from src.tools.database.database import Database
from src.tools.logger.service import LoggerService
from telegram import InlineKeyboardMarkup
from telegram.ext import AIORateLimiter, Application


class TelegramService:
    def __init__(
        self,
        logger_service: LoggerService,
        database: Database,
    ):
        self._logger_service = logger_service
        self._database = database

        self._bot = (
            Application.builder()
            .token(TELEGRAM_BOT_TOKEN)
            .rate_limiter(AIORateLimiter(overall_max_rate=10))
            .build()
        )

    async def get_telegram_chat_by_uuid(
        self,
        db: AsyncSession,
        chat_uuid: str,
    ) -> models.TelegramChat | None:
        return (
            await db.execute(
                select(models.TelegramChat)
                .filter(models.TelegramChat.chat_uuid == chat_uuid)
                .limit(1),
            )
        ).scalar_one_or_none()

    async def get_telegram_chat_by_id(
        self,
        db: AsyncSession,
        tg_chat_id: int,
    ) -> models.TelegramChat:
        return (
            await db.execute(
                select(models.TelegramChat)
                .filter(models.TelegramChat.telegram_chat_id == tg_chat_id)
                .limit(1),
            )
        ).scalar_one()

    async def get_telegram_chat_or_error(
        self,
        db: AsyncSession,
        chat_uuid: str,
    ) -> models.TelegramChat:
        telegram_chat = await self.get_telegram_chat_by_uuid(db, chat_uuid)

        if not telegram_chat:
            raise Exception("Chat not found")

        return telegram_chat

    async def send_message(
        self,
        *,
        telegram_chat_id: int,
        message: str,
        reply_markup: InlineKeyboardMarkup | None = None,
    ):
        if MODE != "test":
            await self._bot.bot.send_message(
                telegram_chat_id,
                text=message,
                reply_markup=reply_markup,
                disable_web_page_preview=True,
            )

    def validate_telegram_authority(
        self,
        telegram_request: schemas.ConnectTelegramRequest,
    ):
        auth_data = telegram_request.model_dump()

        if not auth_data["first_name"]:
            del auth_data["first_name"]
        if not auth_data["last_name"]:
            del auth_data["last_name"]
        if not auth_data["username"]:
            del auth_data["username"]
        if not auth_data["photo_url"]:
            del auth_data["photo_url"]

        check_hash = auth_data["hash"]
        del auth_data["hash"]

        data_check_arr = []
        for key, value in auth_data.items():
            data_check_arr.append(f"{key}={value}")

        data_check_arr.sort()
        data_check_string = "\n".join(data_check_arr)

        secret_key = hashlib.sha256(TELEGRAM_BOT_TOKEN.encode()).digest()
        hash_value = hmac.new(
            secret_key,
            data_check_string.encode(),
            hashlib.sha256,
        ).hexdigest()

        if hash_value != check_hash:
            raise HTTPException(
                status.HTTP_403_FORBIDDEN,
                detail="Data hash is not valid",
            )

        return auth_data
