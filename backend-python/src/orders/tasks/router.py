from sqlalchemy.ext.asyncio import AsyncSession
from typing import Annotated
from fastapi import APIRouter, Depends, Header

from src.orders.tasks.dependencies import TasksDI
from src.orders.tasks.service import TasksService
from src.tools.database.dependencies import DatabaseDI
from src.orders.tasks import schemas

router = APIRouter(prefix="/api")
database = DatabaseDI.get_database()


@router.post("/tasks/create")
async def create_task(
    authorization: Annotated[str, Header()],
    create_task: schemas.CreateTask,
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: TasksService = Depends(TasksDI.get_tasks_service),
):
    return await service.create_task(db, authorization, create_task)


@router.post("/tasks/update")
async def update_task(
    authorization: Annotated[str, Header()],
    update_task: schemas.UpdateTask,
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: TasksService = Depends(TasksDI.get_tasks_service),
):
    await service.update_task(db, authorization, update_task)


@router.delete("/tasks/delete/{task_id}")
async def delete_task(
    authorization: Annotated[str, Header()],
    task_id: int,
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: TasksService = Depends(TasksDI.get_tasks_service),
):
    await service.delete_task(db, authorization, task_id)


@router.get("/tasks/order_auto_tasks")
async def get_order_auto_tasks(
    authorization: Annotated[str, Header()],
    order_id: int,
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: TasksService = Depends(TasksDI.get_tasks_service),
    auto_id: int | None = None,
):
    return await service.get_order_auto_tasks(db, authorization, order_id, auto_id)
