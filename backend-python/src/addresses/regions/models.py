from sqlalchemy import ForeignKey, String, Integer
from sqlalchemy.orm import mapped_column, Mapped, relationship

from src.tools.database.database import Base


class Country(Base):
    __tablename__ = "country"

    code: Mapped[str] = mapped_column(String, primary_key=True, index=True)
    name: Mapped[str] = mapped_column(String)


class Region(Base):
    __tablename__ = "region"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String)
    codes: Mapped[str] = mapped_column(String)

    country_code: Mapped[str] = mapped_column(ForeignKey("country.code"))
    country: Mapped[Country] = relationship(Country)
