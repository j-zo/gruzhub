from sqlalchemy.ext.asyncio import AsyncSession
from httpx import AsyncClient
import pytest
import uuid
from fastapi import status
from faker import Faker

from src.orders.orders import schemas
from src.orders.orders.dependencies import OrdersDI
from src.orders.orders.enums import OrderStatus
from src.users.enums import UserRole
from src.users.testing.helper import UserTestingHelper
from src.users.dependencies import UsersDI
from src.users import schemas as users_schemas
from src.util.time_helper import TimeHelper
from tests.orders.orders.test_workflow_router import OrdersWorkflowRouterHelper

faker = Faker()


class TestOrdersDataRouter:
    @staticmethod
    @pytest.mark.asyncio()
    async def test_get_driver_orders(http_client: AsyncClient):
        # prepare schemas
        order_to_create_1_by_same_driver = (
            OrdersWorkflowRouterHelper.create_order_schema()
        )
        order_to_create_2_by_same_driver = (
            OrdersWorkflowRouterHelper.create_order_schema()
        )
        order_to_create_2_by_same_driver.driver_phone = (
            order_to_create_1_by_same_driver.driver_phone
        )

        order_to_create_by_different_driver = (
            OrdersWorkflowRouterHelper.create_order_schema()
        )

        # create orders
        order_1_by_same_driver = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            order_to_create=order_to_create_1_by_same_driver,
        )
        order_2_by_same_driver = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            order_to_create=order_to_create_2_by_same_driver,
        )
        await OrdersWorkflowRouterHelper.create_order(
            http_client,
            order_to_create=order_to_create_by_different_driver,
        )

        # get orders
        assert order_1_by_same_driver.access_token
        orders = await OrdersDataRouterHelper.get_orders(
            http_client,
            access_token=order_1_by_same_driver.access_token,
        )

        # check orders
        EXCPECTED_ORDERS_COUNT = 2
        assert len(orders) == EXCPECTED_ORDERS_COUNT
        assert orders[0].driver_id == order_2_by_same_driver.driver_id
        assert orders[1].driver_id == order_1_by_same_driver.driver_id

    @staticmethod
    @pytest.mark.asyncio()
    async def test_get_order_status_changes(http_client: AsyncClient):
        order_response = await OrdersWorkflowRouterHelper.create_order(http_client)
        assert order_response.access_token

        status_changes = await OrdersWorkflowRouterHelper.get_order_status_changes(
            http_client,
            order_response.order_id,
            order_response.access_token,
        )

        assert len(status_changes) == 1

    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize(
        "order_status",
        [OrderStatus.CREATED, OrderStatus.COMPLETED],
    )
    async def test_get_customer_orders(
        db_connection: AsyncSession,
        http_client: AsyncClient,
        order_status: OrderStatus,
    ):
        orders_workflow_service = OrdersDI.get_orders_workflow_service()
        customer_auth_data = await UserTestingHelper().sign_up(
            db_connection,
            UserRole.CUSTOMER,
        )

        created_orders: list[schemas.CreateOrderResponse] = []
        for _ in range(2):
            created_order_response = await OrdersWorkflowRouterHelper.create_order(
                http_client,
                access_token=customer_auth_data.access_token,
            )
            created_orders.append(created_order_response)

            await orders_workflow_service.update_order_status_for_testing(
                db_connection,
                created_order_response.order_id,
                order_status,
            )

        orders = await OrdersDataRouterHelper.get_orders(
            http_client,
            access_token=customer_auth_data.access_token,
            statuses=[order_status],
        )

        EXCPECTED_ORDERS_COUNT = 2
        assert len(orders) == EXCPECTED_ORDERS_COUNT
        assert orders[1].id == created_orders[0].order_id
        assert orders[0].id == created_orders[1].order_id

        for order in orders:
            assert order.status == order_status

    @staticmethod
    @pytest.mark.asyncio()
    async def test_master_gets_new_orders_in_region(
        db_connection: AsyncSession,
        http_client: AsyncClient,
    ):
        (
            master_auth_data,
            created_order,
        ) = await OrdersWorkflowRouterHelper.create_master_and_order_in_region(
            db_connection,
            http_client,
            is_same_region=True,
        )

        orders = await OrdersDataRouterHelper.get_orders(
            http_client,
            access_token=master_auth_data.access_token,
        )

        orders_ids = [order.id for order in orders]
        assert created_order.order_id in orders_ids

    @staticmethod
    @pytest.mark.asyncio()
    async def test_get_auto_orders(http_client: AsyncClient):
        order_to_create = OrdersWorkflowRouterHelper.create_order_schema()

        create_response = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            order_to_create=order_to_create,
        )
        assert create_response.access_token

        order_response = await OrdersWorkflowRouterHelper.get_order(
            http_client,
            create_response.order_id,
            create_response.access_token,
        )
        auto_id = order_response.autos[0].id

        auto_orders_response = await http_client.get(
            f"/api/orders/auto/{auto_id}/",
            headers={"Authorization": create_response.access_token},
        )
        assert auto_orders_response.status_code == status.HTTP_200_OK
        assert len(auto_orders_response.json()) == 1

    @staticmethod
    @pytest.mark.asyncio()
    async def test_get_order_auto(
        db_connection: AsyncSession,
        http_client: AsyncClient,
    ):
        auth_data = await UserTestingHelper().sign_up(
            db_connection,
            UserRole.CUSTOMER,
        )
        created_order = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            access_token=auth_data.access_token,
        )
        order = await OrdersWorkflowRouterHelper.get_order(
            http_client,
            created_order.order_id,
            auth_data.access_token,
        )
        created_order_auto = order.autos[0]

        auto = await OrdersDataRouterHelper.get_order_auto(
            http_client,
            access_token=auth_data.access_token,
            order_id=created_order.order_id,
            auto_id=created_order_auto.id,
        )

        assert auto.brand == created_order_auto.brand
        assert auto.model == created_order_auto.model
        assert auto.vin == created_order_auto.vin
        assert auto.number == created_order_auto.number

    @staticmethod
    @pytest.mark.asyncio()
    async def test_update_order_auto(
        db_connection: AsyncSession,
        http_client: AsyncClient,
    ):
        auth_data = await UserTestingHelper().sign_up(
            db_connection,
            UserRole.CUSTOMER,
        )
        created_order = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            access_token=auth_data.access_token,
        )
        order = await OrdersWorkflowRouterHelper.get_order(
            http_client,
            created_order.order_id,
            auth_data.access_token,
        )
        created_order_auto = order.autos[0]

        auto_to_update = schemas.UpdateOrderAutoRequest(
            order_id=order.id,
            auto_id=created_order_auto.id,
            brand=faker.text()[1:10],
            model=faker.text()[1:10],
            vin=str(TimeHelper.now_ms()) + str(uuid.uuid4()),
            number=str(TimeHelper.now_ms()) + str(uuid.uuid4()),
        )
        update_auto_response = await http_client.post(
            "/api/orders/auto",
            headers={"Authorization": auth_data.access_token},
            json=auto_to_update.model_dump(),
        )
        assert update_auto_response.status_code == status.HTTP_200_OK

        updated_auto = await OrdersDataRouterHelper.get_order_auto(
            http_client,
            access_token=auth_data.access_token,
            order_id=created_order.order_id,
            auto_id=created_order_auto.id,
        )

        assert updated_auto.brand == auto_to_update.brand
        assert updated_auto.model == auto_to_update.model
        assert updated_auto.vin == auto_to_update.vin
        assert updated_auto.number == auto_to_update.number

    @staticmethod
    @pytest.mark.asyncio()
    async def test_orders_merged_to_elder_auto_if_same_vin_on_auto_update(
        db_connection: AsyncSession,
        http_client: AsyncClient,
    ):
        user_testing_helper = UserTestingHelper()
        customer = await user_testing_helper.sign_up(db_connection, UserRole.CUSTOMER)

        created_order_1 = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            access_token=customer.access_token,
        )
        created_order_2 = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            access_token=customer.access_token,
        )

        order_1 = await OrdersWorkflowRouterHelper.get_order(
            http_client,
            created_order_1.order_id,
            customer.access_token,
        )
        order_2 = await OrdersWorkflowRouterHelper.get_order(
            http_client,
            created_order_2.order_id,
            customer.access_token,
        )

        order_1_auto = order_1.autos[0]
        order_2_auto = order_2.autos[0]
        order_2_auto.vin = order_1_auto.vin
        order_2_auto.number = order_2_auto.vin

        update_auto_response = await http_client.post(
            "/api/orders/auto",
            headers={"Authorization": customer.access_token},
            json={
                "order_id": order_2.id,
                "auto_id": order_2_auto.id,
                "brand": order_2_auto.brand,
                "model": order_2_auto.model,
                "vin": order_2_auto.vin,
                "number": order_2_auto.number,
            },
        )
        assert update_auto_response.status_code == status.HTTP_200_OK

        auto_2_response = await http_client.get(
            f"/api/orders/auto?auto_id={order_2_auto.id}&order_id={order_2.id}",
            headers={"Authorization": customer.access_token},
        )
        assert auto_2_response.status_code == status.HTTP_200_OK
        auto_2 = schemas.AutoResponse(**auto_2_response.json())
        assert auto_2.is_merged == True
        assert auto_2.merge_to_id == order_1_auto.id

        auto_orders_response = await http_client.get(
            f"/api/orders/auto/{order_1_auto.id}/",
            headers={"Authorization": customer.access_token},
        )
        EXPECTED_AUTO_ORDERS = 2
        assert len(auto_orders_response.json()) == EXPECTED_AUTO_ORDERS

    @staticmethod
    @pytest.mark.asyncio()
    async def test_make_and_get_user_info_changes_though_order(
        db_connection: AsyncSession,
        http_client: AsyncClient,
    ):
        user_testing_helper = UserTestingHelper()
        users_service = UsersDI.get_users_service()

        driver_auth = await user_testing_helper.sign_up(
            db_connection,
            UserRole.CUSTOMER,
        )
        await user_testing_helper.update_role(db_connection, UserRole.DRIVER)

        driver = await users_service.get_user_by_id(db_connection, driver_auth.user_id)
        driver_schema = users_schemas.UserResponse(**driver.to_dict())

        order_to_create = OrdersWorkflowRouterHelper.create_order_schema()
        order_to_create.driver_phone = driver.phone
        order_to_create.driver_name = driver.name

        created_order = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            access_token=driver_auth.access_token,
            order_to_create=order_to_create,
        )

        updated_driver = users_schemas.UpdateUserRequest(**driver.to_dict())
        updated_driver.name = faker.name()
        updated_driver.email = str(uuid.uuid4()) + faker.email()
        updated_driver.phone = str(uuid.uuid4())
        updated_driver.inn = faker.text()[0:12]

        await users_service.update(db_connection, driver, updated_driver)

        user_changes_response = await http_client.get(
            f"/api/orders/user-changes?user_id={driver.id}&order_id={created_order.order_id}",
            headers={"Authorization": driver_auth.access_token},
        )
        assert user_changes_response.status_code == status.HTTP_200_OK

        user_changes = [
            users_schemas.UserInfoChange(**info)
            for info in user_changes_response.json()
        ]
        assert len(user_changes) == 1

        user_change = user_changes[0]

        assert user_change.user_id == driver.id

        assert user_change.new_name == updated_driver.name
        assert user_change.previous_name == driver_schema.name

        assert user_change.new_email == updated_driver.email
        assert user_change.previous_email == driver_schema.email

        assert user_change.new_inn == updated_driver.inn
        assert user_change.previous_inn == driver_schema.inn

        assert user_change.new_phone == updated_driver.phone
        assert user_change.previous_phone == driver_schema.phone


class OrdersDataRouterHelper:
    @staticmethod
    async def get_order_auto(
        http_client: AsyncClient,
        *,
        access_token: str,
        order_id: int,
        auto_id: int,
    ) -> schemas.AutoResponse:
        auto_response = await http_client.get(
            f"/api/orders/auto?order_id={order_id}&auto_id={auto_id}",
            headers={"Authorization": access_token},
        )
        assert auto_response.status_code == status.HTTP_200_OK
        return schemas.AutoResponse(**auto_response.json())

    @staticmethod
    async def get_orders(
        http_client: AsyncClient,
        access_token: str,
        statuses: list[OrderStatus] | None = None,
    ) -> list[schemas.OrderResponse]:
        orders_response = await http_client.post(
            "/api/orders/orders/",
            json={"statuses": statuses},
            headers={"Authorization": access_token},
        )
        assert orders_response.status_code == status.HTTP_200_OK
        orders = orders_response.json()
        return [schemas.OrderResponse(**order) for order in orders]
