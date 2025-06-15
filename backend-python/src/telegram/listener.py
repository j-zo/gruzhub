from src.telegram.services.listener_service import TelegramListenerService
from src.tools.database.dependencies import DatabaseDI
from telegram import Update
from telegram.ext import ApplicationBuilder, MessageHandler, filters

database = DatabaseDI.get_database()


class TelegramListener:
    def __init__(
        self,
        telegram_listener_service: TelegramListenerService,
    ):
        self._telegram_listener_service = telegram_listener_service

    def start_listening(self, bot_token: str):
        app = ApplicationBuilder().token(bot_token).build()

        app.add_handler(
            MessageHandler(
                filters.StatusUpdate.MIGRATE,
                lambda update, *_args: self.handle_chat_migration(update),  # type: ignore
            ),
        )
        app.add_handler(
            MessageHandler(
                filters.TEXT,
                lambda update, *_args: self.handle_message(update),  # type: ignore
            ),
        )
        app.add_handler(
            MessageHandler(
                filters.COMMAND,
                lambda update, *_args: self.handle_message(update),  # type: ignore
            ),
        )

        app.add_handler(
            MessageHandler(
                filters.StatusUpdate.NEW_CHAT_TITLE,
                lambda update, *_args: self.handle_chat_title_change(update),  # type: ignore
            ),
        )

        print("Starting listening...")
        app.run_polling()

    async def handle_message(self, update: Update):
        if update.effective_chat and update.effective_chat.id:
            db = database.get_async_session()

            try:
                chat_title = ""

                if update.effective_chat.type == "private":
                    chat_title = "Проверятор (бот)"
                elif update.effective_chat.title:
                    chat_title = update.effective_chat.title

                    if "group" in update.effective_chat.type:
                        chat_title += " (группа)"

                    if "channel" in update.effective_chat.type:
                        chat_title += " (канал)"

                await self._telegram_listener_service.create_chat_if_needed_and_send_code(
                    db,
                    update.effective_chat.id,
                    chat_title,
                )
            finally:
                await db.close()

        else:
            raise Exception("Update does not have effective chat")

    async def handle_chat_migration(self, update: Update):
        if (
            update.message
            and update.message.chat.id
            and update.message.migrate_to_chat_id
        ):
            db = database.get_async_session()

            try:
                await self._telegram_listener_service.migrate_chat(
                    db,
                    update.message.chat.id,
                    update.message.migrate_to_chat_id,
                )
            finally:
                await db.close()

    async def handle_chat_title_change(self, update: Update):
        if (
            update.effective_chat
            and update.effective_chat.id
            and update.effective_chat.title
        ):
            db = database.get_async_session()
            try:
                await self._telegram_listener_service.update_chat_title(
                    db,
                    update.effective_chat.id,
                    update.effective_chat.title,
                )
            finally:
                await db.close()
