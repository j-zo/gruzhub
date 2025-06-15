from sqlalchemy import String, Integer, Float, ForeignKey
from sqlalchemy.orm import mapped_column, Mapped, relationship

from src.tools.database.database import Base
from src.addresses.regions import models as region_models


class Address(Base):
    __tablename__ = "address"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)

    region_id: Mapped[int] = mapped_column(ForeignKey("region.id"), index=True)
    region: Mapped[region_models.Region] = relationship(region_models.Region)

    city: Mapped[str | None] = mapped_column(String, nullable=True)
    street: Mapped[str | None] = mapped_column(String, nullable=True)
    latitude: Mapped[float | None] = mapped_column(Float, nullable=True)
    longtitude: Mapped[float | None] = mapped_column(Float, nullable=True)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "region_id": self.region_id,
            "region_name": self.region.name,
            "city": self.city,
            "street": self.street,
            "latitude": self.latitude,
            "longtitude": self.longtitude,
        }
