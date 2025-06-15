import decimal
from sqlalchemy import Integer, String, ForeignKey, DECIMAL, BigInteger
from sqlalchemy.orm import relationship, mapped_column, Mapped

from src.tools.database.database import Base
from src.orders.auto.models import Auto
from src.orders.orders.models import Order


class Task(Base):
    __tablename__ = "tasks"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)

    auto_id: Mapped[int] = mapped_column(ForeignKey("auto.id"))
    auto: Mapped[Auto] = relationship(Auto)

    order_id: Mapped[int] = mapped_column(ForeignKey("orders.id"))
    order: Mapped[Order] = relationship(Order)

    name: Mapped[str] = mapped_column(String)
    description: Mapped[str | None] = mapped_column(String, nullable=True)
    price: Mapped[decimal.Decimal | None] = mapped_column(DECIMAL(12, 2), nullable=True)

    created_at: Mapped[int] = mapped_column(BigInteger)
    updated_at: Mapped[int] = mapped_column(BigInteger)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "auto_id": self.auto_id,
            "order_id": self.order_id,
            "name": self.name,
            "description": self.description,
            "price": self.price,
            "created_at": self.created_at,
            "updated_at": self.updated_at,
        }
