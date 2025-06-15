from http import HTTPStatus
from fastapi import HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import joinedload

from src.addresses.regions import service
from . import models
from . import schemas


class AddressesService:
    def __init__(self, regions_service: service.RegionsService):
        self._regions_service = regions_service

    async def create_address(
        self,
        db: AsyncSession,
        address: schemas.Address,
    ) -> models.Address:
        region = await self._regions_service.get_region_by_id(db, address.region_id)

        new_address = models.Address()
        new_address.city = address.city
        new_address.street = address.street
        new_address.latitude = address.latitude
        new_address.longtitude = address.longtitude
        new_address.region = region

        db.add(new_address)
        await db.commit()
        await db.refresh(new_address)

        return new_address

    async def update_address(
        self,
        db: AsyncSession,
        address: schemas.Address,
    ) -> models.Address:
        if not address.id:
            raise HTTPException(
                status_code=HTTPStatus.BAD_REQUEST,
                detail="ID was not provided",
            )

        region = await self._regions_service.get_region_by_id(db, address.region_id)

        updating_address = await self._get_address_model_by_id(db, address.id)
        updating_address.city = address.city
        updating_address.street = address.street
        updating_address.latitude = address.latitude
        updating_address.longtitude = address.longtitude
        updating_address.region = region
        await db.commit()
        await db.refresh(updating_address)
        return updating_address

    async def get_address_by_id(
        self,
        db: AsyncSession,
        address_id: int,
    ) -> schemas.Address:
        address = await self._get_address_model_by_id(db, address_id)
        return schemas.Address(
            id=address.id,
            region_id=address.region_id,
            region_name=address.region.name,
            city=address.city,
            street=address.street,
            latitude=address.latitude,
            longtitude=address.longtitude,
        )

    async def _get_address_model_by_id(
        self,
        db: AsyncSession,
        address_id: int,
    ) -> models.Address:
        return (
            await db.execute(
                select(models.Address)
                .where(models.Address.id == address_id)
                .limit(1)
                .options(joinedload("*")),
            )
        ).scalar_one()
