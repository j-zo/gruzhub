from sqlalchemy import Integer, String, BigInteger, Enum
from sqlalchemy.orm import Mapped, mapped_column

from src.tools.database.database import Base
from .enums import LogLevel


class LogItem(Base):
    __tablename__ = "log_items"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    level: Mapped[LogLevel] = mapped_column(Enum(LogLevel))
    request_id: Mapped[str] = mapped_column(String, index=True)
    user_id: Mapped[int | None] = mapped_column(Integer, nullable=True, index=True)
    text: Mapped[str] = mapped_column(String)
    time: Mapped[int] = mapped_column(BigInteger)
