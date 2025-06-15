from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import ColumnExpressionArgument, select, or_, update
from sqlalchemy.orm import joinedload

from src.orders.orders.models import Order
from src.users.enums import UserRole
from . import models


class OrderMessagesRepository:
    async def get_message_by_guarantee_id(
        self,
        db: AsyncSession,
        order_id: int,
        guarantee_id: str,
    ) -> models.OrderMessage | None:
        return (
            await db.execute(
                select(models.OrderMessage).filter(
                    models.OrderMessage.order_id == order_id,
                    models.OrderMessage.guarantee_id == guarantee_id,
                ),
            )
        ).scalar_one_or_none()

    async def get_last_message_per_each_order(
        self,
        db: AsyncSession,
        user_id: int,
        orders_ids: list[int],
    ) -> list[models.OrderMessage]:
        return list(
            (
                await db.execute(
                    select(models.OrderMessage)
                    .filter(
                        models.OrderMessage.order_id.in_(orders_ids),
                        or_(
                            models.OrderMessage.order.has(Order.master_id == user_id),
                            models.OrderMessage.order.has(Order.driver_id == user_id),
                            models.OrderMessage.order.has(Order.customer_id == user_id),
                        ),
                    )
                    .distinct(models.OrderMessage.order_id)
                    .order_by(
                        models.OrderMessage.order_id.desc(),
                        models.OrderMessage.date.desc(),
                    )
                    .options(joinedload(models.OrderMessage.order))
                    .options(joinedload(models.OrderMessage.file)),
                )
            ).scalars(),
        )

    async def get_order_messages(
        self,
        db: AsyncSession,
        order_id: int,
    ) -> list[models.OrderMessage]:
        return list(
            (
                await db.execute(
                    select(models.OrderMessage)
                    .filter(models.OrderMessage.order_id == order_id)
                    .order_by(models.OrderMessage.date.asc())
                    .options(joinedload(models.OrderMessage.file)),
                )
            ).scalars(),
        )

    async def get_order_messages_user_ids(
        self,
        db: AsyncSession,
        order_id: int,
    ) -> list[int]:
        return list(
            (
                await db.execute(
                    select(models.OrderMessage.user_id)
                    .filter(models.OrderMessage.order_id == order_id)
                    .group_by(models.OrderMessage.user_id),
                )
            ).scalars(),
        )

    async def set_messages_viewed_by_role(
        self,
        db: AsyncSession,
        order_id: int,
        role: UserRole,
    ) -> None:
        fitler_clauses: list[ColumnExpressionArgument] = []
        fitler_clauses.append(models.OrderMessage.order_id == order_id)

        update_dict: dict = {}

        if role == UserRole.CUSTOMER:
            fitler_clauses.append(models.OrderMessage.is_viewed_by_customer == False)
            update_dict = {"is_viewed_by_customer": True}

        if role == UserRole.MASTER:
            fitler_clauses.append(models.OrderMessage.is_viewed_by_master == False)
            update_dict = {"is_viewed_by_master": True}

        if role == UserRole.DRIVER:
            fitler_clauses.append(models.OrderMessage.is_viewed_by_driver == False)
            update_dict = {"is_viewed_by_driver": True}

        await db.execute(
            update(models.OrderMessage)
            .where(
                *fitler_clauses,
            )
            .values(update_dict),
        )
        await db.commit()
