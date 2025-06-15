from sqlalchemy import BigInteger, String
from sqlalchemy.orm import Mapped, mapped_column
from src.tools.database.database import Base


class TelegramChat(Base):
    __tablename__ = "telegram_chats"

    chat_uuid: Mapped[str] = mapped_column(String, primary_key=True, index=True)
    telegram_chat_id: Mapped[int] = mapped_column(BigInteger, index=True)
    title: Mapped[str | None] = mapped_column(String, nullable=True)

    def to_dict(self):
        return {
            "chat_uuid": self.chat_uuid,
            "telegram_chat_id": self.telegram_chat_id,
            "title": self.title,
        }
