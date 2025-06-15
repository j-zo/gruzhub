from random import randint
import pytest
import uuid
from fastapi import status
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession
from faker import Faker

from src.users.enums import UserRole
from src.users.testing.helper import UserTestingHelper
from src.util.time_helper import TimeHelper
from src.orders.messages import schemas
from src.users import schemas as users_schemas
from tests.orders.orders.test_workflow_router import OrdersWorkflowRouterHelper

faker = Faker()


class TestOrderMessagesRouter:
    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("order_owner_role", [UserRole.DRIVER, UserRole.CUSTOMER])
    async def test_send_message(
        db_connection: AsyncSession,
        http_client: AsyncClient,
        order_owner_role: UserRole,
    ):
        order_with_users = (
            await OrdersWorkflowRouterHelper.create_order_with_attached_users(
                db_connection,
                http_client,
                order_owner_role,
            )
        )

        await OrderMessagesRouterHelper.send_message(
            http_client,
            order_with_users.order_id,
            order_with_users.order_owner_token,
        )
        await OrderMessagesRouterHelper.send_message(
            http_client,
            order_with_users.order_id,
            order_with_users.master_token,
        )

    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("order_owner_role", [UserRole.DRIVER, UserRole.CUSTOMER])
    async def test_get_last_messages_per_each_order(
        db_connection: AsyncSession,
        http_client: AsyncClient,
        order_owner_role: UserRole,
    ):
        user_testing_helper = UserTestingHelper()
        region_id = randint(20, 50)

        master = await user_testing_helper.sign_up(
            db_connection,
            UserRole.MASTER,
            region_id=region_id,
        )

        order_1_with_users = (
            await OrdersWorkflowRouterHelper.create_order_with_attached_users(
                db_connection,
                http_client,
                order_owner_role,
                master.access_token,
                region_id,
            )
        )
        order_2_with_users = (
            await OrdersWorkflowRouterHelper.create_order_with_attached_users(
                db_connection,
                http_client,
                order_owner_role,
                master.access_token,
                region_id,
            )
        )

        await OrderMessagesRouterHelper.send_message(
            http_client,
            order_1_with_users.order_id,
            order_1_with_users.master_token,
        )
        last_message_of_order_1 = await OrderMessagesRouterHelper.send_message(
            http_client,
            order_1_with_users.order_id,
            order_1_with_users.order_owner_token,
        )

        await OrderMessagesRouterHelper.send_message(
            http_client,
            order_2_with_users.order_id,
            order_2_with_users.order_owner_token,
        )
        last_message_of_order_2 = await OrderMessagesRouterHelper.send_message(
            http_client,
            order_2_with_users.order_id,
            order_2_with_users.master_token,
        )

        last_messages = await OrderMessagesRouterHelper.get_last_message_per_each_order(
            http_client,
            [
                order_1_with_users.order_id,
                order_2_with_users.order_id,
            ],
            master.access_token,
        )

        EXPECTED_MESSAGES_COUNT = 2
        assert len(last_messages) == EXPECTED_MESSAGES_COUNT

        assert last_messages[0].text == last_message_of_order_2.text
        assert last_messages[1].text == last_message_of_order_1.text

    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("order_owner_role", [UserRole.DRIVER, UserRole.CUSTOMER])
    async def test_get_order_messages(
        db_connection: AsyncSession,
        http_client: AsyncClient,
        order_owner_role: UserRole,
    ):
        order_with_users = (
            await OrdersWorkflowRouterHelper.create_order_with_attached_users(
                db_connection,
                http_client,
                order_owner_role,
            )
        )

        last_master_message_request_body: schemas.SendMessageRequest
        last_owner_message_request_body: schemas.SendMessageRequest

        for _i in range(3):
            last_master_message_request_body = (
                await OrderMessagesRouterHelper.send_message(
                    http_client,
                    order_with_users.order_id,
                    order_with_users.master_token,
                )
            )
            last_owner_message_request_body = (
                await OrderMessagesRouterHelper.send_message(
                    http_client,
                    order_with_users.order_id,
                    order_with_users.order_owner_token,
                )
            )

        master_messages = await OrderMessagesRouterHelper.get_order_messages(
            http_client,
            order_with_users.order_id,
            order_with_users.master_token,
        )
        order_owner_messages = await OrderMessagesRouterHelper.get_order_messages(
            http_client,
            order_with_users.order_id,
            order_with_users.order_owner_token,
        )

        EXPECTED_MESSAGES_COUNT = 6
        assert len(master_messages) == EXPECTED_MESSAGES_COUNT
        assert len(order_owner_messages) == EXPECTED_MESSAGES_COUNT

        assert (
            master_messages[len(master_messages) - 1].text
            == last_owner_message_request_body.text
        )
        assert (
            master_messages[len(master_messages) - 2].text
            == last_master_message_request_body.text
        )

    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("order_owner_role", [UserRole.DRIVER, UserRole.CUSTOMER])
    async def test_get_order_messages_users(
        db_connection: AsyncSession,
        http_client: AsyncClient,
        order_owner_role: UserRole,
    ):
        order_with_users = (
            await OrdersWorkflowRouterHelper.create_order_with_attached_users(
                db_connection,
                http_client,
                order_owner_role,
            )
        )

        await OrderMessagesRouterHelper.send_message(
            http_client,
            order_with_users.order_id,
            order_with_users.master_token,
        )
        await OrderMessagesRouterHelper.send_message(
            http_client,
            order_with_users.order_id,
            order_with_users.order_owner_token,
        )

        for access_token in [
            order_with_users.order_owner_token,
            order_with_users.master_token,
        ]:
            users = await OrderMessagesRouterHelper.get_order_messages_users(
                http_client,
                order_with_users.order_id,
                access_token,
            )
            EXPECTED_USERS_COUNT = 2
            assert len(users) == EXPECTED_USERS_COUNT

    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("order_owner_role", [UserRole.DRIVER, UserRole.CUSTOMER])
    async def test_set_messages_viewed_by_role(
        db_connection: AsyncSession,
        http_client: AsyncClient,
        order_owner_role: UserRole,
    ):
        order_with_users = (
            await OrdersWorkflowRouterHelper.create_order_with_attached_users(
                db_connection,
                http_client,
                order_owner_role,
            )
        )

        await OrderMessagesRouterHelper.send_message(
            http_client,
            order_with_users.order_id,
            order_with_users.master_token,
        )
        await OrderMessagesRouterHelper.send_message(
            http_client,
            order_with_users.order_id,
            order_with_users.order_owner_token,
        )

        await OrderMessagesRouterHelper.set_messages_viewed_by_role(
            http_client,
            order_with_users.order_id,
            order_with_users.master_token,
        )
        messages = await OrderMessagesRouterHelper.get_order_messages(
            http_client,
            order_with_users.order_id,
            order_with_users.master_token,
        )

        for message in messages:
            assert message.is_viewed_by_master is True

        await OrderMessagesRouterHelper.set_messages_viewed_by_role(
            http_client,
            order_with_users.order_id,
            order_with_users.order_owner_token,
        )
        messages = await OrderMessagesRouterHelper.get_order_messages(
            http_client,
            order_with_users.order_id,
            order_with_users.master_token,
        )

        for message in messages:
            assert message.is_viewed_by_driver or message.is_viewed_by_customer


class OrderMessagesRouterHelper:
    @staticmethod
    async def send_message(
        http_client: AsyncClient,
        order_id: int,
        access_token: str,
    ) -> schemas.SendMessageRequest:
        """
        @returns message body that has been sent to
        verify sent message with received message
        """
        body = schemas.SendMessageRequest(
            guarantee_id=f"{uuid.uuid4()}-{TimeHelper.now_ms()}",
            order_id=order_id,
            text=faker.text(),
        )

        response = await http_client.post(
            f"/api/orders/messages/send",
            headers={"Authorization": access_token},
            json=body.model_dump(),
        )
        assert response.status_code == status.HTTP_200_OK

        return body

    @staticmethod
    async def get_order_messages(
        http_client: AsyncClient,
        order_id: int,
        access_token: str,
    ) -> list[schemas.OrderMessage]:
        response = await http_client.get(
            f"/api/orders/messages/get-order-messages/{order_id}",
            headers={"Authorization": access_token},
        )
        assert response.status_code == status.HTTP_200_OK

        return [schemas.OrderMessage(**message) for message in response.json()]

    @staticmethod
    async def get_last_message_per_each_order(
        http_client: AsyncClient,
        orders_ids: list[int],
        access_token: str,
    ) -> list[schemas.OrderMessage]:
        response = await http_client.post(
            f"/api/orders/messages/last-messages-per-order",
            headers={"Authorization": access_token},
            json={"orders_ids": orders_ids},
        )
        assert response.status_code == status.HTTP_200_OK

        return [schemas.OrderMessage(**message) for message in response.json()]

    @staticmethod
    async def get_order_messages_users(
        http_client: AsyncClient,
        order_id: int,
        access_token: str,
    ) -> list[users_schemas.UserResponse]:
        response = await http_client.get(
            f"/api/orders/messages/get-order-messages-users/{order_id}",
            headers={"Authorization": access_token},
        )
        assert response.status_code == status.HTTP_200_OK

        return [users_schemas.UserResponse(**message) for message in response.json()]

    @staticmethod
    async def set_messages_viewed_by_role(
        http_client: AsyncClient,
        order_id: int,
        access_token: str,
    ) -> None:
        response = await http_client.get(
            f"/api/orders/messages/set-messages-viewed-by-role/{order_id}",
            headers={"Authorization": access_token},
        )
        assert response.status_code == status.HTTP_200_OK
