from decimal import Decimal
from fastapi import HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import joinedload
from sqlalchemy import ColumnExpressionArgument, select
from src.orders.orders.services.data_service import OrdersDataService

from src.users.enums import UserRole
from src.orders.auto.service import AutoService
from src.users.service import UsersService
from src.util.time_helper import TimeHelper
from . import schemas, models


class TasksService:
    def __init__(
        self,
        users_service: UsersService,
        orders_data_service: OrdersDataService,
        auto_service: AutoService,
    ):
        self._users_service = users_service
        self._orders_service = orders_data_service
        self._auto_service = auto_service

    async def create_task(
        self,
        db: AsyncSession,
        authorization: str,
        create_request: schemas.CreateTask,
    ) -> schemas.TaskResponse:
        order = await self._orders_service.get_order_model_by_id(
            db,
            create_request.order_id,
            authorization,
        )
        auto = await self._auto_service.get_auto_by_id(
            db,
            create_request.auto_id,
        )

        authorized_user = await self._users_service.get_user_model_from_token(
            db,
            authorization,
        )

        if order.master_id != authorized_user.id:
            raise HTTPException(status.HTTP_403_FORBIDDEN)

        order_autos_ids = [auto.id for auto in order.autos]
        if auto.id not in order_autos_ids:
            raise HTTPException(status.HTTP_403_FORBIDDEN)

        task = models.Task()
        task.auto = auto
        task.order = order
        task.name = create_request.name
        task.description = create_request.description
        task.price = Decimal(create_request.price) if create_request.price else None
        task.created_at = TimeHelper.now_ms()
        task.updated_at = TimeHelper.now_ms()
        db.add(task)
        await db.commit()
        await db.refresh(task)
        return schemas.TaskResponse(**task.to_dict())

    async def update_task(
        self,
        db: AsyncSession,
        authorization: str,
        update_request: schemas.UpdateTask,
    ):
        authorized_user = await self._users_service.get_user_from_token(
            db,
            authorization,
        )
        task = await self.get_task_by_id(db, update_request.id)

        if task.order.master_id != authorized_user.id:
            raise HTTPException(status.HTTP_403_FORBIDDEN)

        task.name = update_request.name
        task.description = update_request.description
        task.price = Decimal(update_request.price) if update_request.price else None
        task.updated_at = TimeHelper.now_ms()
        await db.commit()

    async def delete_task(self, db: AsyncSession, authorization: str, task_id: int):
        authorized_user = await self._users_service.get_user_from_token(
            db,
            authorization,
        )
        task = await self.get_task_by_id(db, task_id)

        if task.order.master_id != authorized_user.id:
            raise HTTPException(status.HTTP_403_FORBIDDEN)

        await db.delete(task)
        await db.commit()

    async def get_order_auto_tasks(
        self,
        db: AsyncSession,
        authorization: str,
        order_id: int,
        auto_id: int | None = None,
    ) -> list[schemas.TaskResponse]:
        if not order_id and not auto_id:
            raise HTTPException(
                status.HTTP_400_BAD_REQUEST,
                "Either order_id or auto_id should be specified",
            )

        authorized_user = await self._users_service.get_user_from_token(
            db,
            authorization,
        )
        order = await self._orders_service.get_order_model_by_id(
            db,
            order_id,
            authorization,
        )

        if authorized_user.role != UserRole.ADMIN and authorized_user.id not in (
            order.driver_id,
            order.master_id,
            order.customer_id,
        ):
            raise HTTPException(status.HTTP_403_FORBIDDEN)

        fitler_clauses: list[ColumnExpressionArgument] = []
        fitler_clauses.append(models.Task.order_id == order_id)
        if auto_id:
            fitler_clauses.append(models.Task.auto_id == auto_id)

        query_result = await db.execute(
            select(models.Task).filter(*fitler_clauses).order_by(models.Task.id.desc()),
        )
        tasks = list(query_result.unique().scalars())
        return [schemas.TaskResponse(**task.to_dict()) for task in tasks]

    async def get_task_by_id(self, db: AsyncSession, task_id: int) -> models.Task:
        return (
            (
                await db.execute(
                    select(models.Task)
                    .filter(models.Task.id == task_id)
                    .options(joinedload("*"))
                    .limit(1),
                )
            )
            .unique()
            .scalar_one()
        )
