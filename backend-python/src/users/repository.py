from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import ColumnExpressionArgument, select
from sqlalchemy.orm import joinedload

from src.addresses.addresses.models import Address

from . import models, schemas
from .enums import UserRole


class UsersRepository:
    async def create(
        self,
        db: AsyncSession,
        user: models.User,
    ) -> models.User:
        db.add(user)
        await db.commit()
        await db.refresh(user)

        reloaded_user = await self.get_user_by_id(db, user.id)
        assert reloaded_user
        return reloaded_user

    async def update(
        self,
        db: AsyncSession,
        user: models.User,
    ) -> models.User:
        existing_user = await self.get_user_by_id(db, user.id)
        assert existing_user

        existing_user.email = user.email
        existing_user.phone = user.phone

        existing_user.balance = user.balance

        existing_user.name = user.name
        existing_user.inn = user.inn
        existing_user.trip_radius_km = user.trip_radius_km

        existing_user.address = user.address

        existing_user.registration_date = user.registration_date
        existing_user.password_hash = user.password_hash
        existing_user.password_creation_time = user.password_creation_time
        existing_user.user_reset_code = user.user_reset_code

        await db.commit()
        await db.refresh(existing_user)

        reloaded_user = await self.get_user_by_id(db, existing_user.id)
        assert reloaded_user
        return reloaded_user

    async def get_user_by_id(
        self,
        db: AsyncSession,
        id: int,
        *,
        is_for_update=False,
    ) -> models.User | None:
        statement = select(models.User).where(models.User.id == id).limit(1)

        if is_for_update:
            statement = statement.with_for_update(nowait=False)
        else:
            statement = statement.options(joinedload("*"))

        query_result = await db.execute(statement)
        return query_result.scalar()

    async def get_user_by_email(
        self,
        session: AsyncSession,
        email: str,
        role: UserRole,
    ) -> models.User | None:
        statement = (
            select(models.User)
            .where(
                models.User.email == email,
                models.User.role == role,
            )
            .options(joinedload("*"))
            .limit(1)
        )

        query_result = await session.execute(statement)
        return query_result.scalar()

    async def get_user_by_phone(
        self,
        db: AsyncSession,
        phone: str,
        role: UserRole,
    ) -> models.User | None:
        statement = (
            select(models.User)
            .where(
                models.User.phone == phone,
                models.User.role == role,
            )
            .options(joinedload("*"))
            .limit(1)
        )

        query_result = await db.execute(statement)
        return query_result.scalar()

    async def get_masters_in_region_with_telegram(
        self,
        db: AsyncSession,
        region_id: int,
    ) -> list[models.User]:
        query_result = await db.execute(
            select(models.User).filter(
                models.User.role == UserRole.MASTER,
                models.User.telegram_id.is_not(None),
                models.User.address.has(Address.region_id == region_id),
            ),
        )
        return list(query_result.scalars())

    async def get_users_by_ids(
        self,
        db: AsyncSession,
        users_ids: list[int],
    ) -> list[models.User]:
        return list(
            (
                await db.execute(
                    select(models.User).filter(models.User.id.in_(users_ids)),
                )
            ).scalars(),
        )

    async def get_admins(self, db: AsyncSession) -> list[models.User]:
        query_result = await db.execute(
            select(models.User)
            .filter(models.User.role == UserRole.ADMIN)
            .options(joinedload("*"))
            .order_by(models.User.id.desc()),
        )
        return list(query_result.scalars())

    async def get_users(
        self,
        db: AsyncSession,
        *,
        regions_ids: list[int] | None = None,
        roles: list[models.UserRole] | None = None,
    ) -> list[models.User]:
        fitler_clauses: list[ColumnExpressionArgument] = []

        if regions_ids:
            fitler_clauses.append(
                models.User.address.has(Address.region_id.in_(regions_ids)),
            )
        if roles:
            fitler_clauses.append(models.User.role.in_(roles))

        query_result = await db.execute(
            select(models.User)
            .filter(*fitler_clauses)
            .options(joinedload("*"))
            .order_by(models.User.id.desc()),
        )
        return list(query_result.scalars())
