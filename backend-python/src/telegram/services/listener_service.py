import uuid

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from telegram import InlineKeyboardButton, InlineKeyboardMarkup, WebAppInfo
from src.telegram import models

from src.telegram.services.telegram_service import TelegramService


class TelegramListenerService:
    def __init__(self, telegram_service: TelegramService):
        self._telegram_service = telegram_service

    async def create_chat_if_needed_and_send_code(
        self,
        db: AsyncSession,
        telegram_chat_id: int,
        chat_title: str,
        *,
        is_ignore_send_errors=False,  # used for tests
    ) -> str:
        """
        Returns UUID of created chat
        """

        telegram_chat_query = (
            select(models.TelegramChat)
            .filter(models.TelegramChat.telegram_chat_id == telegram_chat_id)
            .limit(1)
        )
        telegram_chat = (await db.execute(telegram_chat_query)).scalar()

        if not telegram_chat:
            telegram_chat = await self._create_chat(db, telegram_chat_id)
        elif telegram_chat.title != chat_title:
            telegram_chat.title = chat_title
            await db.commit()

        try:
            await self._telegram_service.send_message(
                telegram_chat_id=telegram_chat_id,
                message="Код чата для подключения уведомлений о новых заказах в вашем регионе (для вставки на сайте https://gruzhub.ru/):",
            )
            await self._telegram_service.send_message(
                telegram_chat_id=telegram_chat_id,
                message=f"{telegram_chat.chat_uuid}",
            )

            # send web app button
            await self._telegram_service.send_message(
                telegram_chat_id=telegram_chat_id,
                message='Для откытия приложения "ГрузХаб" нажмите на кнопку:',
                reply_markup=InlineKeyboardMarkup(
                    [
                        [
                            InlineKeyboardButton(
                                text="Открыть ГрузХаб",
                                web_app=WebAppInfo(
                                    url=f"https://app.gruzhub.ru/",
                                ),
                            ),
                        ],
                    ],
                ),
            )
        except Exception as e:
            if not is_ignore_send_errors:
                raise e

        return telegram_chat.chat_uuid

    async def migrate_chat(
        self,
        db: AsyncSession,
        migration_from_chat_id: int,
        migrate_to_chat_id: int,
    ):
        old_chat = (
            await db.execute(
                select(models.TelegramChat)
                .filter(models.TelegramChat.telegram_chat_id == migration_from_chat_id)
                .limit(1),
            )
        ).scalar()

        if old_chat:
            old_chat.telegram_chat_id = migrate_to_chat_id
            await db.commit()

    async def update_chat_title(
        self,
        db: AsyncSession,
        telegram_chat_id: int,
        title: str,
    ):
        chat = (
            await db.execute(
                select(models.TelegramChat)
                .filter(models.TelegramChat.telegram_chat_id == telegram_chat_id)
                .limit(1),
            )
        ).scalar()

        if chat:
            chat.title = title
            await db.commit()

    async def _create_chat(
        self,
        db: AsyncSession,
        telegram_chat_id: int,
    ) -> models.TelegramChat:
        telegram_chat = models.TelegramChat()
        telegram_chat.telegram_chat_id = telegram_chat_id
        telegram_chat.chat_uuid = await self._generate_unique_chat_uuid(db)

        db.add(telegram_chat)
        await db.commit()
        await db.refresh(telegram_chat)

        return telegram_chat

    async def _generate_unique_chat_uuid(self, db: AsyncSession) -> str:
        new_uuid = str(uuid.uuid4())

        while (
            await db.execute(
                select(models.TelegramChat)
                .filter(models.TelegramChat.chat_uuid == new_uuid)
                .limit(1),
            )
        ).scalar() is not None:
            new_uuid = str(uuid.uuid4())

        return new_uuid
