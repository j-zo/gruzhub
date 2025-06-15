from decimal import Decimal
import pytest
import random
from fastapi import status
from faker import Faker
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from tests.orders.orders.test_workflow_router import OrdersWorkflowRouterHelper
from src.orders.tasks import schemas

faker = Faker()


class TestTasksRouter:
    @staticmethod
    @pytest.mark.asyncio()
    async def test_create_task(db_connection: AsyncSession, http_client: AsyncClient):
        (
            master_data,
            order,
        ) = await OrdersWorkflowRouterHelper.create_master_with_order_and_take_into_work(
            http_client,
            db_connection,
        )
        order_auto = order.autos[0]

        task_to_create = TasksRouterHelper.create_task_schema(
            order.id,
            order_auto.id,
        )
        created_task = await TasksRouterHelper.create_task(
            http_client,
            task_schema=task_to_create,
            access_token=master_data.access_token,
        )
        assert created_task.id
        assert created_task.order_id == order.id
        assert created_task.auto_id == order_auto.id
        assert created_task.name == task_to_create.name
        assert created_task.description == task_to_create.description

        assert created_task.price
        assert task_to_create.price
        assert Decimal(created_task.price) == Decimal(task_to_create.price)

    @staticmethod
    @pytest.mark.asyncio()
    async def test_get_order_auto_tasks(
        db_connection: AsyncSession,
        http_client: AsyncClient,
    ):
        (
            master_data,
            order,
        ) = await OrdersWorkflowRouterHelper.create_master_with_order_and_take_into_work(
            http_client,
            db_connection,
        )
        order_auto = order.autos[0]

        await TasksRouterHelper.create_task(
            http_client,
            order_id=order.id,
            auto_id=order_auto.id,
            access_token=master_data.access_token,
        )
        await TasksRouterHelper.create_task(
            http_client,
            order_id=order.id,
            auto_id=order_auto.id,
            access_token=master_data.access_token,
        )

        tasks = await TasksRouterHelper.get_order_auto_tasks(
            http_client,
            order_id=order.id,
            auto_id=order_auto.id,
            access_token=master_data.access_token,
        )

        EXPECTED_TASKS_AMOUNT = 2
        assert len(tasks) == EXPECTED_TASKS_AMOUNT

    @staticmethod
    @pytest.mark.asyncio()
    async def test_update_task(db_connection: AsyncSession, http_client: AsyncClient):
        (
            master_data,
            order,
        ) = await OrdersWorkflowRouterHelper.create_master_with_order_and_take_into_work(
            http_client,
            db_connection,
        )
        order_auto = order.autos[0]

        created_task = await TasksRouterHelper.create_task(
            http_client,
            order_id=order.id,
            auto_id=order_auto.id,
            access_token=master_data.access_token,
        )

        task_to_update = schemas.UpdateTask(
            id=created_task.id,
            **TasksRouterHelper.create_task_schema(
                order.id,
                order_auto.id,
            ).model_dump(),
        )
        update_response = await http_client.post(
            "/api/tasks/update",
            headers={"Authorization": master_data.access_token},
            json=task_to_update.model_dump(),
        )
        assert update_response.status_code == status.HTTP_200_OK

        tasks = await TasksRouterHelper.get_order_auto_tasks(
            http_client,
            order_id=order.id,
            auto_id=order_auto.id,
            access_token=master_data.access_token,
        )
        updated_task = tasks[0]

        assert updated_task.name == task_to_update.name
        assert updated_task.description == task_to_update.description
        assert updated_task.price
        assert task_to_update.price
        assert Decimal(updated_task.price) == Decimal(task_to_update.price)

    @staticmethod
    @pytest.mark.asyncio()
    async def test_delete_task(db_connection: AsyncSession, http_client: AsyncClient):
        (
            master_data,
            order,
        ) = await OrdersWorkflowRouterHelper.create_master_with_order_and_take_into_work(
            http_client,
            db_connection,
        )
        order_auto = order.autos[0]

        created_task = await TasksRouterHelper.create_task(
            http_client,
            order_id=order.id,
            auto_id=order_auto.id,
            access_token=master_data.access_token,
        )

        delete_response = await http_client.delete(
            f"/api/tasks/delete/{created_task.id}",
            headers={"Authorization": master_data.access_token},
        )
        assert delete_response.status_code == status.HTTP_200_OK

        tasks = await TasksRouterHelper.get_order_auto_tasks(
            http_client,
            order_id=order.id,
            auto_id=order_auto.id,
            access_token=master_data.access_token,
        )
        assert len(tasks) == 0


class TasksRouterHelper:
    @staticmethod
    async def get_order_auto_tasks(
        http_client: AsyncClient,
        *,
        order_id: int,
        auto_id: int,
        access_token: str,
    ) -> list[schemas.TaskResponse]:
        tasks_response = await http_client.get(
            f"/api/tasks/order_auto_tasks?order_id={order_id}&auto_id={auto_id}",
            headers={"Authorization": access_token},
        )
        assert tasks_response.status_code == status.HTTP_200_OK
        return [schemas.TaskResponse(**task) for task in tasks_response.json()]

    @staticmethod
    async def create_task(
        http_client: AsyncClient,
        *,
        task_schema: schemas.CreateTask | None = None,
        order_id: int | None = None,
        auto_id: int | None = None,
        access_token: str,
    ) -> schemas.TaskResponse:
        if not task_schema and order_id and auto_id:
            task_schema = TasksRouterHelper.create_task_schema(
                order_id,
                auto_id,
            )

        if not task_schema:
            raise Exception("Task schema is None")
        if not access_token:
            raise Exception("access_token is None")

        create_task_response = await http_client.post(
            "/api/tasks/create",
            headers={"Authorization": access_token},
            json=task_schema.model_dump(),
        )
        assert create_task_response.status_code == status.HTTP_200_OK

        return schemas.TaskResponse(**create_task_response.json())

    @staticmethod
    def create_task_schema(
        order_id: int,
        auto_id: int,
    ) -> schemas.CreateTask:
        return schemas.CreateTask(
            auto_id=auto_id,
            order_id=order_id,
            name=faker.text(),
            description=faker.text(),
            price=f"{random.randint(1, 100)}.{random.randint(0, 99)}",
        )
