from sqlalchemy import ForeignKey, BigInteger
from sqlalchemy.orm import relationship, mapped_column, Mapped
from src.orders.orders.models import Order

from src.tools.database.database import Base


class OldOrderNotification(Base):
    __tablename__ = "orders_notifications"

    order_id: Mapped[int] = mapped_column(
        ForeignKey("orders.id"),
        primary_key=True,
        index=True,
    )
    order: Mapped[Order] = relationship()
    order_last_status_change_time: Mapped[int] = mapped_column(
        BigInteger,
        primary_key=True,
        index=True,
    )
