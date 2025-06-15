from sqlalchemy import ColumnExpressionArgument, select, or_, and_, case
from sqlalchemy.orm import joinedload
from sqlalchemy.ext.asyncio import AsyncSession
from src.addresses.addresses.models import Address

from src.orders.orders.enums import OrderStatus
from src.util.time_helper import TimeHelper

from . import models
from src.orders.auto.models import Auto


class OrdersRepository:
    async def get_order_by_guarantee_id(
        self,
        db: AsyncSession,
        guarantee_id: str,
    ) -> models.Order | None:
        query_result = await db.execute(
            select(models.Order)
            .filter(models.Order.guarantee_uuid == guarantee_id)
            .options(joinedload("*"))
            .limit(1),
        )
        return query_result.scalar()

    async def get_master_orders(
        self,
        db: AsyncSession,
        *,
        master_id: int,
        region_id: int,
        limit: int | None = None,
        statuses: list[OrderStatus] | None = None,
    ):
        fitler_clauses: list[ColumnExpressionArgument] = []

        if not statuses:
            fitler_clauses.append(
                or_(
                    models.Order.master_id == master_id,
                    and_(
                        models.Order.address.has(Address.region_id == region_id),
                        models.Order.status == OrderStatus.CREATED,
                    ),
                ),
            )
        elif OrderStatus.CREATED in statuses:
            fitler_clauses.append(
                or_(
                    and_(
                        models.Order.master_id == master_id,
                        models.Order.status.in_(statuses),
                    ),
                    and_(
                        models.Order.address.has(Address.region_id == region_id),
                        models.Order.status == OrderStatus.CREATED,
                    ),
                ),
            )
        else:
            fitler_clauses.append(
                and_(
                    models.Order.master_id == master_id,
                    models.Order.status.in_(statuses),
                ),
            )

        query_result = await db.execute(
            select(models.Order)
            .filter(*fitler_clauses)
            .order_by(
                case(
                    {
                        OrderStatus.CREATED: 1,
                        OrderStatus.CALCULATING: 2,
                        OrderStatus.REVIEWING: 3,
                        OrderStatus.ACCEPTED: 4,
                        OrderStatus.COMPLETED: 5,
                        OrderStatus.CANCEL: 6,
                    },
                    value=models.Order.status,
                ),
                models.Order.id.desc(),
            )
            .limit(limit)
            .options(joinedload("*")),
        )
        return list(query_result.unique().scalars())

    async def get_orders(
        self,
        db: AsyncSession,
        *,
        master_id: int | None = None,
        customer_id: int | None = None,
        driver_id: int | None = None,
        auto_id: int | None = None,
        user_id: int | None = None,
        regions_ids: list[int] | None = None,
        statuses: list[OrderStatus] | None = None,
        limit: int | None = None,
    ) -> list[models.Order]:
        fitler_clauses: list[ColumnExpressionArgument] = []

        if statuses:
            fitler_clauses.append(models.Order.status.in_(statuses))

        if auto_id:
            fitler_clauses.append(models.Order.autos.any(Auto.id == auto_id))

        if customer_id:
            fitler_clauses.append(models.Order.customer_id == customer_id)

        if driver_id:
            fitler_clauses.append(models.Order.driver_id == driver_id)

        if user_id:
            fitler_clauses.append(
                or_(
                    models.Order.customer_id == user_id,
                    models.Order.driver_id == user_id,
                    models.Order.master_id == user_id,
                ),
            )

        if regions_ids:
            fitler_clauses.append(
                models.Order.address.has(models.Address.region_id.in_(regions_ids)),
            )
        if master_id:
            fitler_clauses.append(
                models.Order.address.has(models.Order.master_id == master_id),
            )

        query_result = await db.execute(
            select(models.Order)
            .filter(*fitler_clauses)
            .order_by(
                case(
                    {
                        OrderStatus.CREATED: 1,
                        OrderStatus.CALCULATING: 2,
                        OrderStatus.REVIEWING: 3,
                        OrderStatus.ACCEPTED: 4,
                        OrderStatus.COMPLETED: 5,
                        OrderStatus.CANCEL: 6,
                    },
                    value=models.Order.status,
                ),
                models.Order.id.desc(),
            )
            .limit(limit)
            .options(joinedload("*")),
        )
        return list(query_result.unique().scalars())

    async def get_order_by_id(
        self,
        db: AsyncSession,
        order_id: int,
        *,
        is_for_update=False,
        is_load_relations=True,
    ) -> models.Order:
        query = select(models.Order).filter(models.Order.id == order_id)

        if is_load_relations and is_for_update:
            raise Exception("Cannot JOIN and SELECT FOR UPDATE in same time")

        if is_load_relations:
            query = query.options(joinedload("*"))

        if is_for_update:
            query = query.with_for_update(nowait=False)

        query_result = await db.execute(query)

        return query_result.unique().scalar_one()

    async def update_order_status_for_testing(
        self,
        db: AsyncSession,
        order_id: int,
        status: OrderStatus,
    ):
        query_result = await db.execute(
            select(models.Order).filter(models.Order.id == order_id).with_for_update(),
        )
        order = query_result.unique().scalar_one()
        order.status = status
        await db.commit()

    async def create_order(
        self,
        db: AsyncSession,
        order: models.Order,
    ):
        db.add(order)
        await db.commit()
        await db.refresh(order)
        return order

    async def add_order_status_change(
        self,
        db: AsyncSession,
        order_status_change: models.OrderStatusChange,
        *,
        is_commit: bool,
    ):
        db.add(order_status_change)

        if is_commit:
            await db.commit()

    async def get_order_status_changes(
        self,
        db: AsyncSession,
        order_id: int,
    ) -> list[models.OrderStatusChange]:
        query_result = await db.execute(
            select(models.OrderStatusChange)
            .filter(
                models.OrderStatusChange.order_id == order_id,
            )
            .order_by(models.OrderStatusChange.id.asc())
            .options(joinedload("*")),
        )
        return list(query_result.unique().scalars())

    async def get_orders_created_longer_than(
        self,
        db: AsyncSession,
        time: int,
    ) -> list[models.Order]:
        now = TimeHelper.now_ms()

        query_result = await db.execute(
            select(models.Order)
            .filter(
                models.Order.status == OrderStatus.CREATED,
                models.Order.last_status_update_time < now - time,
            )
            .order_by(models.Order.id.desc())
            .options(joinedload("*")),
        )
        return list(query_result.unique().scalars())

    async def get_orders_changed_longer_than(
        self,
        db: AsyncSession,
        time: int,
    ) -> list[models.Order]:
        now = TimeHelper.now_ms()

        query_result = await db.execute(
            select(models.Order)
            .filter(
                models.Order.status.in_(
                    [
                        OrderStatus.CALCULATING,
                        OrderStatus.REVIEWING,
                        OrderStatus.ACCEPTED,
                    ],
                ),
                models.Order.last_status_update_time < now - time,
            )
            .order_by(models.Order.id.desc())
            .options(joinedload("*")),
        )
        return list(query_result.unique().scalars())
