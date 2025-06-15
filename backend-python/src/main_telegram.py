from src.constants import TELEGRAM_BOT_TOKEN
from src.telegram.dependencies import TelegramDI

telegram_listener = TelegramDI.get_telegram_listener()
telegram_listener.start_listening(TELEGRAM_BOT_TOKEN)
