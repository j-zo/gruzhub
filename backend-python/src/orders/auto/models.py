from __future__ import annotations
from sqlalchemy import Boolean, Integer, String, ForeignKey, Enum
from sqlalchemy.orm import relationship, mapped_column, Mapped

from src.orders.auto.enums import AutoType
from src.tools.database.database import Base
from src.users.models import User


class Auto(Base):
    __tablename__ = "auto"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)

    customer_id: Mapped[int | None] = mapped_column(
        ForeignKey("users.id"),
        nullable=True,
    )
    driver_id: Mapped[int | None] = mapped_column(
        ForeignKey("users.id"),
        nullable=True,
    )

    customer: Mapped[User | None] = relationship(User, foreign_keys=[customer_id])
    driver: Mapped[User | None] = relationship(User, foreign_keys=[driver_id])

    brand: Mapped[str | None] = mapped_column(String, nullable=True, index=True)
    model: Mapped[str | None] = mapped_column(String, nullable=True)
    vin: Mapped[str | None] = mapped_column(String, nullable=True, index=True)
    number: Mapped[str | None] = mapped_column(String, nullable=True, index=True)

    is_merged: Mapped[bool] = mapped_column(Boolean)
    merged_to_id: Mapped[int | None] = mapped_column(Integer, nullable=True)

    type: Mapped[AutoType] = mapped_column(Enum(AutoType))

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "customer_id": self.customer_id,
            "driver_id": self.driver_id,
            "brand": self.brand,
            "model": self.model,
            "vin": self.vin,
            "number": self.number,
            "type": self.type,
            "is_merged": self.is_merged,
            "merge_to_id": self.merged_to_id,
        }
