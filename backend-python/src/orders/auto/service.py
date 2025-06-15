from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import joinedload
from src.users.enums import UserRole
from fastapi import HTTPException, status

from src.users.service import UsersService
from src.users import schemas as users_schemas
from . import models, schemas


class AutoService:
    def __init__(self, user_service: UsersService):
        self._user_service = user_service

    async def create_auto(self, db: AsyncSession, auto: models.Auto) -> models.Auto:
        if auto.vin:
            same_vin_auto = await self._get_auto_by_vin(db, exclude_id=-1, vin=auto.vin)
            if same_vin_auto:
                auto.id = same_vin_auto.id
                return await self.update_auto(db, auto)

        if auto.number:
            same_number_auto = await self._get_auto_by_number(
                db,
                exclude_id=-1,
                number=auto.number,
            )
            if same_number_auto:
                auto.id = same_number_auto.id
                return await self.update_auto(db, auto)

        auto.is_merged = False
        db.add(auto)
        await db.commit()
        await db.refresh(auto)
        return await self.get_auto_by_id(db, auto.id)

    async def update_auto(self, db: AsyncSession, auto: models.Auto) -> models.Auto:
        existing_auto = await self.get_auto_by_id(db, auto.id)

        original_auto_with_same_vin_or_number = (
            await self._get_original_auto_by_vin_or_number(
                db,
                exclude_id=auto.id,
                vin=auto.vin,
                number=auto.number,
            )
        )

        # this means that the car existed before, but we
        # identified its number for dublicated auto and
        # here we merge it with previous
        if original_auto_with_same_vin_or_number:
            existing_auto.is_merged = True
            existing_auto.merged_to_id = original_auto_with_same_vin_or_number.id

            self._merge_dublicated_auto_fields_to_original_auto(
                existing_auto,
                original_auto_with_same_vin_or_number,
            )
            existing_auto = original_auto_with_same_vin_or_number

        if auto.customer:
            existing_auto.customer = auto.customer
        if auto.driver:
            existing_auto.driver = auto.driver
        if auto.brand:
            existing_auto.brand = auto.brand
        if auto.model:
            existing_auto.model = auto.model
        if auto.vin:
            existing_auto.vin = auto.vin
        if auto.number:
            existing_auto.number = auto.number
        if auto.type:
            existing_auto.type = auto.type

        await db.commit()
        return await self.get_auto_by_id(db, existing_auto.id)

    async def get_auto_by_id(
        self,
        db: AsyncSession,
        auto_id: int,
    ) -> models.Auto:
        query_result = await db.execute(
            select(models.Auto)
            .filter(models.Auto.id == auto_id)
            .options(joinedload("*"))
            .limit(1),
        )
        return query_result.scalar_one()

    async def get_auto_by_id_with_auth(
        self,
        db: AsyncSession,
        authorization: str,
        auto_id: int,
    ) -> schemas.Auto:
        user = await self._user_service.get_user_from_token(db, authorization)
        auto = await self.get_auto_by_id(db, auto_id)

        if (
            (auto.driver and auto.driver.id != user.id)
            and (auto.customer and auto.customer.id != user.id)
            and user.role != UserRole.ADMIN
        ):
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN)

        auto_response = schemas.Auto(**auto.to_dict())
        if auto.customer:
            auto_response.customer = users_schemas.UserResponse(
                **auto.customer.to_dict(),
            )
        if auto.driver:
            auto_response.driver = users_schemas.UserResponse(**auto.driver.to_dict())
        return auto_response

    async def _get_auto_by_vin(
        self,
        db: AsyncSession,
        exclude_id: int,
        vin: str,
    ) -> models.Auto | None:
        query_result = await db.execute(
            select(models.Auto)
            .filter(
                models.Auto.vin == vin,
                models.Auto.id != exclude_id,
            )
            .options(joinedload("*"))
            .limit(1),
        )
        return query_result.scalar_one_or_none()

    async def _get_auto_by_number(
        self,
        db: AsyncSession,
        exclude_id: int,
        number: str,
    ) -> models.Auto | None:
        query_result = await db.execute(
            select(models.Auto)
            .filter(
                models.Auto.number == number,
                models.Auto.id != exclude_id,
            )
            .options(joinedload("*"))
            .limit(1),
        )
        return query_result.scalar_one_or_none()

    async def _get_original_auto_by_vin_or_number(
        self,
        db: AsyncSession,
        exclude_id: int,
        vin: str | None = None,
        number: str | None = None,
    ) -> models.Auto | None:
        original_auto_with_same_vin_or_number: models.Auto | None = None

        if vin:
            original_auto_with_same_vin_or_number = await self._get_auto_by_vin(
                db,
                exclude_id,
                vin,
            )

        if number and not original_auto_with_same_vin_or_number:
            original_auto_with_same_vin_or_number = await self._get_auto_by_number(
                db,
                exclude_id,
                number,
            )

        return original_auto_with_same_vin_or_number

    def _merge_dublicated_auto_fields_to_original_auto(
        self,
        new_auto: models.Auto,
        original_auto: models.Auto,
    ):
        if new_auto.driver and not original_auto.driver:
            original_auto.driver = new_auto.driver

        if new_auto.customer and not original_auto.customer:
            original_auto.customer = new_auto.customer
