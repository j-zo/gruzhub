from __future__ import annotations
from sqlalchemy import BigInteger, Boolean, Integer, String, ForeignKey
from sqlalchemy.orm import mapped_column, Mapped, relationship

from src.orders.orders.models import Order
from src.tools.database.database import Base
from src.tools.files.models import File
from src.users.models import User


class OrderMessage(Base):
    __tablename__ = "order_messages"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    guarantee_id: Mapped[str] = mapped_column(String)

    order_id: Mapped[int] = mapped_column(ForeignKey("orders.id"), index=True)
    order: Mapped[Order] = relationship(Order)

    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    user: Mapped[User] = relationship(User)
    # we attach role to show it on UI without joins
    user_role: Mapped[str] = mapped_column(String)

    text: Mapped[str | None] = mapped_column(String, nullable=True)
    date: Mapped[int] = mapped_column(BigInteger)

    file_code: Mapped[str | None] = mapped_column(
        ForeignKey("files.code"),
        nullable=True,
    )
    file: Mapped[File | None] = relationship(File, foreign_keys=[file_code])

    is_viewed_by_master: Mapped[bool] = mapped_column(Boolean)
    is_viewed_by_driver: Mapped[bool] = mapped_column(Boolean)
    is_viewed_by_customer: Mapped[bool] = mapped_column(Boolean)

    def to_dict(self) -> dict:
        data = {
            "id": self.id,
            "guarantee_id": self.guarantee_id,
            "order_id": self.order_id,
            "user_role": self.user_role,
            "user_id": self.user_id,
            "text": self.text,
            "date": self.date,
            "is_viewed_by_master": self.is_viewed_by_master,
            "is_viewed_by_driver": self.is_viewed_by_driver,
            "is_viewed_by_customer": self.is_viewed_by_customer,
        }

        if self.file:
            data["file_code"] = self.file_code
            data["file"] = self.file.to_dict()

        return data
