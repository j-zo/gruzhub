from sqlalchemy import (
    Integer,
    String,
    ForeignKey,
    BigInteger,
    Boolean,
    Table,
    Column,
    ARRAY,
)
from sqlalchemy.ext.mutable import MutableList
from sqlalchemy.orm import relationship, mapped_column, Mapped

from src.tools.database.database import Base
from src.users.models import User
from src.orders.orders.enums import OrderStatus
from src.addresses.addresses.models import Address
from src.orders.auto.models import Auto

order_to_auto_assosiation_table = Table(
    "order_to_auto_assosiation",
    Base.metadata,
    Column("order_id", ForeignKey("orders.id"), index=True),
    Column("auto_id", ForeignKey("auto.id"), index=True),
)


class Order(Base):
    __tablename__ = "orders"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    guarantee_uuid: Mapped[str] = mapped_column(String, index=True)

    customer_id: Mapped[int | None] = mapped_column(
        ForeignKey("users.id"),
        nullable=True,
    )
    customer: Mapped[User | None] = relationship(User, foreign_keys=[customer_id])

    master_id: Mapped[int | None] = mapped_column(ForeignKey("users.id"), nullable=True)
    master: Mapped[User | None] = relationship(User, foreign_keys=[master_id])
    declined_masters_ids: Mapped[str] = mapped_column(String, nullable=True)

    driver_id: Mapped[int | None] = mapped_column(ForeignKey("users.id"), nullable=True)
    driver: Mapped[User | None] = relationship(User, foreign_keys=[driver_id])

    autos: Mapped[list[Auto]] = relationship(
        Auto,
        secondary=order_to_auto_assosiation_table,
    )

    created_at: Mapped[int] = mapped_column(BigInteger)
    updated_at: Mapped[int] = mapped_column(BigInteger)

    last_status_update_time: Mapped[int] = mapped_column(
        BigInteger,
        index=True,
    )
    status: Mapped[str] = mapped_column(String, index=True)
    description: Mapped[str] = mapped_column(String, nullable=True)
    notes: Mapped[str | None] = mapped_column(String, nullable=True)

    address_id: Mapped[int] = mapped_column(ForeignKey("address.id"))
    address: Mapped[Address] = relationship(Address)

    is_need_evacuator: Mapped[bool] = mapped_column(Boolean)
    is_need_mobile_team: Mapped[bool] = mapped_column(Boolean)

    urgency: Mapped[str | None] = mapped_column(String, nullable=True)

    def to_dict(self):
        order = {
            "id": self.id,
            "guarantee_uuid": self.guarantee_uuid,
            "customer_id": self.customer_id,
            "master_id": self.master_id,
            "driver_id": self.driver_id,
            "autos": [auto.to_dict() for auto in self.autos],
            "created_at": self.created_at,
            "updated_at": self.updated_at,
            "status": self.status,
            "description": self.description,
            "notes": self.notes,
            "address": self.address.to_dict(),
            "is_need_evacuator": self.is_need_evacuator,
            "is_need_mobile_team": self.is_need_mobile_team,
            "declined_masters_ids": [
                int(master_id)
                for master_id in self.declined_masters_ids.split(",")
                if master_id
            ],
            "last_status_update_time": self.last_status_update_time,
            "urgency": self.urgency,
        }

        if self.master_id and self.master:
            order["master"] = self.master.to_dict()

        if self.customer_id and self.customer:
            order["customer"] = self.customer.to_dict()

        if self.driver_id and self.driver:
            order["driver"] = self.driver.to_dict()

        return order


class OrderStatusChange(Base):
    __tablename__ = "orders_status_changes"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    updated_at: Mapped[int] = mapped_column(BigInteger)

    order_id: Mapped[int] = mapped_column(ForeignKey("orders.id"), index=True)
    order: Mapped[Order] = relationship(Order)

    new_status: Mapped[str] = mapped_column(String)

    updated_by_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    updated_by: Mapped[User] = relationship(User, foreign_keys=[updated_by_id])

    master_id: Mapped[int | None] = mapped_column(
        ForeignKey("users.id"),
        nullable=True,
    )
    master: Mapped[User | None] = relationship(User, foreign_keys=[master_id])

    comment: Mapped[str | None] = mapped_column(String, nullable=True)

    def to_dict(self) -> dict:
        status_change = {
            "id": self.id,
            "updated_at": self.updated_at,
            "order_id": self.order_id,
            "new_status": self.new_status,
            "updated_by": self.updated_by.to_dict(),
            "comment": self.comment,
        }

        if self.master:
            status_change["master"] = self.master.to_dict()

        return status_change
