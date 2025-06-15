from sqlalchemy import (
    ARRAY,
    Integer,
    String,
    BigInteger,
    Enum,
    DECIMAL,
    ForeignKey,
    Boolean,
)
from sqlalchemy.orm import mapped_column, Mapped, relationship
from sqlalchemy.ext.mutable import MutableList
import decimal

from src.addresses.addresses import models as addresses_models
from src.tools.database.database import Base
from .enums import UserRole


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    role: Mapped[str] = mapped_column(String)

    email: Mapped[str | None] = mapped_column(String, nullable=True)
    phone: Mapped[str | None] = mapped_column(String, nullable=True)

    balance: Mapped[decimal.Decimal] = mapped_column(DECIMAL(12, 2))

    name: Mapped[str] = mapped_column(String)
    inn: Mapped[str | None] = mapped_column(String, nullable=True)
    trip_radius_km: Mapped[int | None] = mapped_column(Integer, nullable=True)

    address_id: Mapped[int | None] = mapped_column(
        ForeignKey("address.id"),
        nullable=True,
    )
    address: Mapped[addresses_models.Address | None] = relationship(
        addresses_models.Address,
    )

    registration_date: Mapped[int] = mapped_column(BigInteger)
    password_hash: Mapped[str | None] = mapped_column(String, nullable=True)
    password_creation_time: Mapped[int | None] = mapped_column(
        BigInteger,
        nullable=True,
    )
    user_reset_code: Mapped[str | None] = mapped_column(String, nullable=True)

    telegram_id: Mapped[int | None] = mapped_column(
        BigInteger,
        nullable=True,
        index=True,
    )
    telegram_access_error: Mapped[str | None] = mapped_column(String, nullable=True)
    telegram_access_error_shown: Mapped[bool] = mapped_column(Boolean, nullable=True)

    user_chats_codes: Mapped[str | None] = mapped_column(String, nullable=True)

    def to_dict(self) -> dict:
        user = {
            "id": self.id,
            "role": self.role,
            "email": self.email,
            "phone": self.phone,
            "balance": float(self.balance),
            "name": self.name,
            "inn": self.inn,
            "trip_radius_km": self.trip_radius_km,
            "registration_date": self.registration_date,
            "password_creation_time": self.password_creation_time,
            "telegram_id": self.telegram_id,
            "telegram_access_error": self.telegram_access_error,
            "telegram_access_error_shown": self.telegram_access_error_shown,
            "user_chats_codes": self.user_chats_codes.split(",")
            if self.user_chats_codes
            else [],
        }

        if self.address:
            user["address"] = self.address.to_dict()

        return user


class UserInfoChange(Base):
    __tablename__ = "user_info_changes"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)

    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    user: Mapped[User] = relationship(User)

    previous_name: Mapped[str] = mapped_column(String)
    new_name: Mapped[str] = mapped_column(String)

    previous_phone: Mapped[str | None] = mapped_column(String, nullable=True)
    new_phone: Mapped[str | None] = mapped_column(String, nullable=True)

    previous_email: Mapped[str | None] = mapped_column(String, nullable=True)
    new_email: Mapped[str | None] = mapped_column(String, nullable=True)

    previous_inn: Mapped[str | None] = mapped_column(String, nullable=True)
    new_inn: Mapped[str | None] = mapped_column(String, nullable=True)

    date: Mapped[int] = mapped_column(BigInteger)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "user_id": self.user_id,
            "previous_name": self.previous_name,
            "new_name": self.new_name,
            "previous_phone": self.previous_phone,
            "new_phone": self.new_phone,
            "previous_email": self.previous_email,
            "new_email": self.new_email,
            "previous_inn": self.previous_inn,
            "new_inn": self.new_inn,
            "date": self.date,
        }
