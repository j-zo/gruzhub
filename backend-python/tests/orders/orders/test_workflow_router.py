from sqlalchemy.ext.asyncio import AsyncSession
from httpx import AsyncClient
import pytest
import uuid
import random
from fastapi import status
from faker import Faker

from src.orders.orders import schemas as orders_schemas
from src.orders.auto.enums import AutoType
from src.orders.orders.enums import OrderStatus
from src.users.enums import UserRole
from src.users.testing.helper import UserTestingHelper
from src.users.testing.schemas import AuthData
from src.users.dependencies import UsersDI
from src.util.time_helper import TimeHelper
from . import schemas

faker = Faker()


class TestWorkflowOrdersRouter:
    @staticmethod
    @pytest.mark.asyncio()
    async def test_create_anonymous_order(http_client: AsyncClient):
        create_response = await OrdersWorkflowRouterHelper.create_order(http_client)

        assert create_response.access_token
        assert create_response.driver_id
        assert create_response.order_id

    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("user_role", [UserRole.DRIVER, UserRole.CUSTOMER])
    async def test_create_order(
        http_client: AsyncClient,
        db_connection: AsyncSession,
        user_role: UserRole,
    ):
        user_testing_helper = UserTestingHelper()
        customer_auth = await user_testing_helper.sign_up(
            db_connection,
            UserRole.CUSTOMER,
        )
        await user_testing_helper.update_role(db_connection, user_role)

        response = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            access_token=customer_auth.access_token,
        )
        assert response.order_id

    @staticmethod
    @pytest.mark.asyncio()
    async def test_create_order_for_existing_auto(http_client: AsyncClient):
        autos = [OrdersWorkflowRouterHelper.create_auto(AutoType.TRAILER)]
        create_first_order_response = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            autos=autos,
        )
        create_second_order_response = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            autos=autos,
        )

        assert create_first_order_response.access_token
        assert create_second_order_response.access_token

        first_order_response = await OrdersWorkflowRouterHelper.get_order(
            http_client,
            create_first_order_response.order_id,
            create_first_order_response.access_token,
        )
        second_order_response = await OrdersWorkflowRouterHelper.get_order(
            http_client,
            create_second_order_response.order_id,
            create_second_order_response.access_token,
        )

        assert first_order_response.autos[0].id == second_order_response.autos[0].id

    @staticmethod
    @pytest.mark.asyncio()
    async def test_get_order(http_client: AsyncClient):
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

        assert order_to_create.description == order_response.description
        assert order_to_create.is_need_evacuator == order_response.is_need_evacuator
        assert order_to_create.is_need_mobile_team == order_response.is_need_mobile_team
        assert order_response.status == OrderStatus.CREATED
        assert order_response.driver_id
        assert order_response.created_at
        assert order_response.updated_at
        assert order_to_create.region_id == order_response.address.region_id
        assert order_to_create.city == order_response.address.city
        assert order_to_create.street == order_response.address.street
        assert order_to_create.urgency == order_response.urgency

        EXPECTED_ORDERS_AUTO_COUNT = 2
        assert len(order_response.autos) == EXPECTED_ORDERS_AUTO_COUNT

        def get_auto_brand(
            auto: orders_schemas.OrderAuto | orders_schemas.AutoResponse,
        ) -> str:
            if auto.brand:
                return auto.brand
            return ""

        order_to_create.autos.sort(key=get_auto_brand)
        order_response.autos.sort(key=get_auto_brand)

        for index in range(1):
            assert (
                order_response.autos[index].brand == order_to_create.autos[index].brand
            )
            assert (
                order_response.autos[index].model == order_to_create.autos[index].model
            )
            assert order_response.autos[index].vin == order_to_create.autos[index].vin
            assert (
                order_response.autos[index].number
                == order_to_create.autos[index].number
            )
            assert order_response.autos[index].type == order_to_create.autos[index].type

    @staticmethod
    @pytest.mark.asyncio()
    async def test_get_same_order_on_same_guarantee_uuid(http_client: AsyncClient):
        schema_to_create_1 = OrdersWorkflowRouterHelper.create_order_schema()
        schema_to_create_2 = OrdersWorkflowRouterHelper.create_order_schema()
        schema_to_create_2.guarantee_uuid = schema_to_create_1.guarantee_uuid

        create_order_1_response = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            order_to_create=schema_to_create_1,
        )
        create_order_2_response = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            order_to_create=schema_to_create_2,
        )

        assert create_order_1_response.order_id == create_order_2_response.order_id

    @staticmethod
    @pytest.mark.asyncio()
    async def test_master_cannot_start_another_region_order_calculation(
        db_connection: AsyncSession,
        http_client: AsyncClient,
    ):
        (
            master_auth_data,
            created_order,
        ) = await OrdersWorkflowRouterHelper.create_master_and_order_in_region(
            db_connection,
            http_client,
            is_same_region=False,
        )

        calculating_order_response = await http_client.get(
            f"/api/orders/{created_order.order_id}/start_calculation_by_master",
            headers={"Authorization": master_auth_data.access_token},
        )
        assert calculating_order_response.status_code != status.HTTP_200_OK

    @staticmethod
    @pytest.mark.asyncio()
    async def test_master_cannot_start_already_started_order(
        db_connection: AsyncSession,
        http_client: AsyncClient,
    ):
        region_id = random.randint(20, 50)

        master_1_auth_data = await UserTestingHelper().sign_up(
            db_connection,
            UserRole.MASTER,
            region_id=region_id,
        )
        master_2_auth_data = await UserTestingHelper().sign_up(
            db_connection,
            UserRole.MASTER,
            region_id=region_id,
        )

        order = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            region_id=region_id,
        )

        calculating_order_response = await http_client.get(
            f"/api/orders/{order.order_id}/start_calculation_by_master",
            headers={"Authorization": master_1_auth_data.access_token},
        )
        assert calculating_order_response.status_code == status.HTTP_200_OK

        calculating_order_response = await http_client.get(
            f"/api/orders/{order.order_id}/start_calculation_by_master",
            headers={"Authorization": master_2_auth_data.access_token},
        )
        assert calculating_order_response.status_code != status.HTTP_200_OK

    @staticmethod
    @pytest.mark.asyncio()
    async def test_master_cannot_get_foreign_master_order(
        db_connection: AsyncSession,
        http_client: AsyncClient,
    ):
        region_id = random.randint(20, 50)

        master_1_auth_data = await UserTestingHelper().sign_up(
            db_connection,
            UserRole.MASTER,
            region_id=region_id,
        )
        master_2_auth_data = await UserTestingHelper().sign_up(
            db_connection,
            UserRole.MASTER,
            region_id=region_id,
        )

        order = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            region_id=region_id,
        )

        await http_client.get(
            f"/api/orders/{order.order_id}/start_calculation_by_master",
            headers={"Authorization": master_1_auth_data.access_token},
        )

        foregin_master_order_response = await http_client.get(
            f"/api/orders/{order.order_id}/",
            headers={"Authorization": master_2_auth_data.access_token},
        )
        assert foregin_master_order_response.status_code != status.HTTP_200_OK

    @staticmethod
    @pytest.mark.asyncio()
    async def test_decline_order_master_with_refund(
        db_connection: AsyncSession,
        http_client: AsyncClient,
    ):
        region_id = random.randint(20, 50)

        users_service = UsersDI.get_users_service()
        user_testing_helper = UserTestingHelper()
        customer = await user_testing_helper.sign_up(
            db_connection,
            UserRole.CUSTOMER,
            region_id,
        )
        (
            master,
            created_order,
        ) = await OrdersWorkflowRouterHelper.create_master_and_order_in_region(
            db_connection,
            http_client,
            is_same_region=True,
            region_id=region_id,
            customer_access_token=customer.access_token,
        )
        master_in_start = await users_service.get_user_by_id(
            db_connection,
            master.user_id,
        )

        await http_client.get(
            f"/api/orders/{created_order.order_id}/start_calculation_by_master",
            headers={"Authorization": master.access_token},
        )
        decine_response = await http_client.post(
            f"/api/orders/{created_order.order_id}/decline_order_master",
            headers={"Authorization": customer.access_token},
            json={"comment": faker.text()[0:100]},
        )
        assert decine_response.status_code == status.HTTP_200_OK

        order = await OrdersWorkflowRouterHelper.get_order(
            http_client,
            created_order.order_id,
            customer.access_token,
        )
        assert order.master == None
        assert order.master_id == None
        assert order.status == OrderStatus.CREATED

        master_response = await http_client.get(
            f"/api/users/{master.user_id}",
            headers={"Authorization": master.access_token},
        )
        assert master_response.json()["balance"] == master_in_start.balance

        orders_statuses = await OrdersWorkflowRouterHelper.get_order_status_changes(
            http_client,
            created_order.order_id,
            customer.access_token,
        )
        EXPECTED_ORDER_STATUS_CHANGES_COUNT = 3
        assert len(orders_statuses) == EXPECTED_ORDER_STATUS_CHANGES_COUNT

        last_order_status_change = orders_statuses[len(orders_statuses) - 1]
        assert last_order_status_change.master is None
        assert last_order_status_change.comment is not None

    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("user_role", [UserRole.CUSTOMER, UserRole.ADMIN])
    async def test_cancel_order_with_refund(
        db_connection: AsyncSession,
        http_client: AsyncClient,
        user_role: UserRole,
    ):
        users_service = UsersDI.get_users_service()
        customer_user_testing_helper = UserTestingHelper()
        admin_user_testing_helper = UserTestingHelper()

        # create accounts & order
        region_id = random.randint(20, 50)
        customer = await customer_user_testing_helper.sign_up(
            db_connection,
            UserRole.CUSTOMER,
            region_id,
        )
        (
            master,
            created_order,
        ) = await OrdersWorkflowRouterHelper.create_master_and_order_in_region(
            db_connection,
            http_client,
            is_same_region=True,
            region_id=region_id,
            customer_access_token=customer.access_token,
        )
        master_in_start = await users_service.get_user_by_id(
            db_connection,
            master.user_id,
        )
        admin = await admin_user_testing_helper.sign_up(
            db_connection,
            UserRole.CUSTOMER,
        )
        await admin_user_testing_helper.update_role(db_connection, UserRole.ADMIN)

        await http_client.get(
            f"/api/orders/{created_order.order_id}/start_calculation_by_master",
            headers={"Authorization": master.access_token},
        )
        decine_response = await http_client.post(
            f"/api/orders/{created_order.order_id}/cancel_order",
            headers={
                "Authorization": customer.access_token
                if user_role == UserRole.CUSTOMER
                else admin.access_token,
            },
            json={"comment": faker.text()[0:100]},
        )
        assert decine_response.status_code == status.HTTP_200_OK

        order = await OrdersWorkflowRouterHelper.get_order(
            http_client,
            created_order.order_id,
            customer.access_token,
        )
        assert order.master == None
        assert order.master_id == None
        assert order.status == OrderStatus.CANCEL

        master_response = await http_client.get(
            f"/api/users/{master.user_id}",
            headers={"Authorization": master.access_token},
        )
        assert master_response.json()["balance"] == master_in_start.balance

    @staticmethod
    @pytest.mark.asyncio()
    async def test_expected_ordering_workflow(  # noqa: PLR0915
        db_connection: AsyncSession,
        http_client: AsyncClient,
    ):
        region_id = random.randint(20, 50)

        user_testing_helper = UserTestingHelper()
        master = await user_testing_helper.sign_up(
            db_connection,
            UserRole.MASTER,
            region_id,
        )
        customer = await user_testing_helper.sign_up(
            db_connection,
            UserRole.CUSTOMER,
            region_id,
        )

        # create order
        created_order = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            access_token=customer.access_token,
            region_id=region_id,
        )

        orders_statuses = await OrdersWorkflowRouterHelper.get_order_status_changes(
            http_client,
            created_order.order_id,
            customer.access_token,
        )

        assert len(orders_statuses) == 1
        last_order_status_change = orders_statuses[len(orders_statuses) - 1]
        assert last_order_status_change.new_status == OrderStatus.CREATED

        # start calculation
        start_calculation_response = await http_client.get(
            f"/api/orders/{created_order.order_id}/start_calculation_by_master",
            headers={"Authorization": master.access_token},
        )
        assert start_calculation_response.status_code == status.HTTP_200_OK

        orders_statuses = await OrdersWorkflowRouterHelper.get_order_status_changes(
            http_client,
            created_order.order_id,
            customer.access_token,
        )

        EXPECTED_ORDER_STATUS_CHANGES_COUNT = 2
        assert len(orders_statuses) == EXPECTED_ORDER_STATUS_CHANGES_COUNT

        last_order_status_change = orders_statuses[len(orders_statuses) - 1]
        assert last_order_status_change.new_status == OrderStatus.CALCULATING
        assert last_order_status_change.master
        assert last_order_status_change.master.id == master.user_id

        # send for confirmation
        send_for_confirmation_response = await http_client.get(
            f"/api/orders/{created_order.order_id}/send_for_confirmation_by_master",
            headers={"Authorization": master.access_token},
        )
        assert send_for_confirmation_response.status_code == status.HTTP_200_OK

        get_order_response = await OrdersWorkflowRouterHelper.get_order(
            http_client,
            created_order.order_id,
            customer.access_token,
        )
        assert get_order_response.status == OrderStatus.REVIEWING

        orders_statuses = await OrdersWorkflowRouterHelper.get_order_status_changes(
            http_client,
            created_order.order_id,
            customer.access_token,
        )
        EXPECTED_ORDER_STATUS_CHANGES_COUNT = 3
        assert len(orders_statuses) == EXPECTED_ORDER_STATUS_CHANGES_COUNT

        last_order_status_change = orders_statuses[len(orders_statuses) - 1]
        assert last_order_status_change.new_status == OrderStatus.REVIEWING
        assert last_order_status_change.master
        assert last_order_status_change.master.id == master.user_id

        # accept by customer
        send_for_confirmation_response = await http_client.get(
            f"/api/orders/{created_order.order_id}/accept_by_customer",
            headers={"Authorization": customer.access_token},
        )
        assert send_for_confirmation_response.status_code == status.HTTP_200_OK

        get_order_response = await OrdersWorkflowRouterHelper.get_order(
            http_client,
            created_order.order_id,
            customer.access_token,
        )
        assert get_order_response.status == OrderStatus.ACCEPTED

        orders_statuses = await OrdersWorkflowRouterHelper.get_order_status_changes(
            http_client,
            created_order.order_id,
            customer.access_token,
        )
        EXPECTED_ORDER_STATUS_CHANGES_COUNT = 4
        assert len(orders_statuses) == EXPECTED_ORDER_STATUS_CHANGES_COUNT

        last_order_status_change = orders_statuses[len(orders_statuses) - 1]
        assert last_order_status_change.new_status == OrderStatus.ACCEPTED
        assert last_order_status_change.master
        assert last_order_status_change.master.id == master.user_id

        # complete order
        send_for_confirmation_response = await http_client.get(
            f"/api/orders/{created_order.order_id}/complete_order",
            headers={"Authorization": master.access_token},
        )
        assert send_for_confirmation_response.status_code == status.HTTP_200_OK

        get_order_response = await OrdersWorkflowRouterHelper.get_order(
            http_client,
            created_order.order_id,
            customer.access_token,
        )
        assert get_order_response.status == OrderStatus.COMPLETED

        orders_statuses = await OrdersWorkflowRouterHelper.get_order_status_changes(
            http_client,
            created_order.order_id,
            customer.access_token,
        )
        EXPECTED_ORDER_STATUS_CHANGES_COUNT = 5
        assert len(orders_statuses) == EXPECTED_ORDER_STATUS_CHANGES_COUNT

        last_order_status_change = orders_statuses[len(orders_statuses) - 1]
        assert last_order_status_change.new_status == OrderStatus.COMPLETED
        assert last_order_status_change.master
        assert last_order_status_change.master.id == master.user_id

    @staticmethod
    @pytest.mark.asyncio()
    async def test_driver_phone_and_name_updates_on_new_number_in_driver_order(
        db_connection: AsyncSession,
        http_client: AsyncClient,
    ):
        user_testing_helper = UserTestingHelper()
        driver = await user_testing_helper.sign_up(db_connection, UserRole.CUSTOMER)
        await user_testing_helper.update_role(db_connection, UserRole.DRIVER)

        order_to_create = OrdersWorkflowRouterHelper.create_order_schema()
        order_to_create.driver_phone = faker.text()[0:12]
        order_to_create.driver_name = faker.text()[0:12]

        await OrdersWorkflowRouterHelper.create_order(
            http_client,
            access_token=driver.access_token,
            order_to_create=order_to_create,
        )

        users_service = UsersDI.get_users_service()
        updated_driver = await users_service.get_user_by_id(
            db_connection,
            driver.user_id,
        )
        assert updated_driver.phone == order_to_create.driver_phone
        assert updated_driver.name == order_to_create.driver_name


class OrdersWorkflowRouterHelper:
    @staticmethod
    async def create_master_with_order_and_take_into_work(
        http_client: AsyncClient,
        db_connection: AsyncSession,
    ) -> tuple[AuthData, orders_schemas.OrderResponse]:
        region_id = random.randint(20, 50)

        master_auth_data = await UserTestingHelper().sign_up(
            db_connection,
            UserRole.MASTER,
            region_id=region_id,
        )

        created_order = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            region_id=region_id,
        )

        await http_client.get(
            f"/api/orders/{created_order.order_id}/start_calculation_by_master",
            headers={"Authorization": master_auth_data.access_token},
        )

        order = await OrdersWorkflowRouterHelper.get_order(
            http_client,
            created_order.order_id,
            master_auth_data.access_token,
        )

        return master_auth_data, order

    @staticmethod
    async def create_master_and_order_in_region(
        db_connection: AsyncSession,
        http_client: AsyncClient,
        *,
        is_same_region: bool,
        customer_access_token: str | None = None,
        region_id: int | None = None,
    ) -> tuple[AuthData, orders_schemas.CreateOrderResponse]:
        if not region_id:
            region_id = random.randint(20, 50)

        master_auth_data = await UserTestingHelper().sign_up(
            db_connection,
            UserRole.MASTER,
            region_id=region_id,
        )

        if not is_same_region:
            region_id = region_id + 1

        created_order = await OrdersWorkflowRouterHelper.create_order(
            http_client,
            region_id=region_id,
            access_token=customer_access_token,
        )

        return master_auth_data, created_order

    @staticmethod
    async def get_order(
        http_client: AsyncClient,
        order_id: int,
        access_token: str,
    ) -> orders_schemas.OrderResponse:
        response = await http_client.get(
            f"/api/orders/{order_id}/",
            headers={"Authorization": access_token},
        )
        assert response.status_code == status.HTTP_200_OK
        return orders_schemas.OrderResponse(**response.json())

    @staticmethod
    async def create_order(
        http_client: AsyncClient,
        *,
        region_id: int | None = None,
        access_token: str | None = None,
        autos: list[orders_schemas.OrderAuto] | None = None,
        order_to_create: orders_schemas.CreateOrder | None = None,
    ) -> orders_schemas.CreateOrderResponse:
        if not order_to_create:
            order_to_create = OrdersWorkflowRouterHelper.create_order_schema(
                autos,
                region_id=region_id,
            )

        response = await http_client.post(
            "/api/orders/create/",
            headers={"Authorization": access_token} if access_token else None,
            json=order_to_create.model_dump(),
        )
        if response.status_code != status.HTTP_200_OK:
            raise Exception(f"Request returned {response.status_code} code")

        return orders_schemas.CreateOrderResponse(**response.json())

    @staticmethod
    def create_order_schema(
        autos: list[orders_schemas.OrderAuto] | None = None,
        region_id: int | None = None,
    ) -> orders_schemas.CreateOrder:
        if not autos:
            autos = [
                OrdersWorkflowRouterHelper.create_auto(AutoType.TRAILER),
                OrdersWorkflowRouterHelper.create_auto(AutoType.TRUCK),
            ]

        if not region_id:
            region_id = random.randint(20, 50)

        return orders_schemas.CreateOrder(
            guarantee_uuid=str(TimeHelper.now_ms()) + str(uuid.uuid4()),
            driver_name=faker.text()[0:12],
            driver_phone=str(TimeHelper.now_ms()) + str(uuid.uuid4()),
            driver_email=str(TimeHelper.now_ms()) + str(uuid.uuid4()) + faker.email(),
            city=faker.text()[0:12],
            street=faker.text()[0:25],
            autos=autos,
            region_id=region_id,
            description=faker.text()[0:150],
            notes=faker.text()[0:150],
            is_need_evacuator=random.randint(1, 2) % 2 == 0,
            is_need_mobile_team=random.randint(1, 2) % 2 == 0,
            urgency=faker.text(),
        )

    @staticmethod
    def create_auto(
        auto_type: AutoType,
        auto_id: int | None = None,
    ) -> orders_schemas.OrderAuto:
        return orders_schemas.OrderAuto(
            auto_id=auto_id,
            brand=faker.text()[1:10],
            model=faker.text()[1:10],
            vin=str(TimeHelper.now_ms()) + str(uuid.uuid4()),
            number=str(TimeHelper.now_ms()) + str(uuid.uuid4()),
            type=auto_type,
        )

    @staticmethod
    async def get_order_status_changes(
        http_client: AsyncClient,
        order_id: int,
        access_token: str,
    ) -> list[orders_schemas.OrderStatusChange]:
        order_status_changes_response = await http_client.get(
            f"/api/orders/order-status-changes/{order_id}/",
            headers={"Authorization": access_token},
        )
        assert order_status_changes_response.status_code == status.HTTP_200_OK

        status_changes_json = order_status_changes_response.json()
        return [
            orders_schemas.OrderStatusChange(**change) for change in status_changes_json
        ]

    @staticmethod
    async def take_order_into_work(
        http_client: AsyncClient,
        master_access_token: str,
        order_id: int,
    ):
        response = await http_client.get(
            f"/api/orders/{order_id}/start_calculation_by_master",
            headers={"Authorization": master_access_token},
        )
        assert response.status_code == status.HTTP_200_OK

    @staticmethod
    async def create_order_with_attached_users(
        db_connection: AsyncSession,
        http_client: AsyncClient,
        order_owner_role: UserRole,
        master_token: str | None = None,
        region_id: int | None = None,
    ):
        user_testing_helper = UserTestingHelper()

        if not region_id:
            region_id = random.randint(20, 50)

        if not master_token:
            master = await user_testing_helper.sign_up(
                db_connection,
                UserRole.MASTER,
                region_id,
            )
            master_token = master.access_token

        if order_owner_role == UserRole.CUSTOMER:
            customer = await user_testing_helper.sign_up(
                db_connection,
                UserRole.CUSTOMER,
            )
            order = await OrdersWorkflowRouterHelper.create_order(
                http_client,
                region_id=region_id,
                access_token=customer.access_token,
            )

            await OrdersWorkflowRouterHelper.take_order_into_work(
                http_client,
                master_token,
                order.order_id,
            )

            return schemas.OrderWithUsers(
                order_id=order.order_id,
                order_owner_token=customer.access_token,
                master_token=master_token,
            )

        if order_owner_role == UserRole.DRIVER:
            order = await OrdersWorkflowRouterHelper.create_order(
                http_client,
                region_id=region_id,
            )
            assert order.access_token

            await OrdersWorkflowRouterHelper.take_order_into_work(
                http_client,
                master_token,
                order.order_id,
            )

            return schemas.OrderWithUsers(
                order_id=order.order_id,
                order_owner_token=order.access_token,
                master_token=master_token,
            )

        raise Exception("Unsupported user role")
