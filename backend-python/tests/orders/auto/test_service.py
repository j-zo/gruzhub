from sqlalchemy.ext.asyncio import AsyncSession
import pytest

from src.orders.auto.dependencies import AutoDI
from src.orders.auto.models import Auto
from src.orders.auto.enums import AutoType
from src.users.models import User
from src.users.dependencies import UsersDI
from src.users.enums import UserRole
from src.users.testing.helper import UserTestingHelper
from faker import Faker

faker = Faker()


class TestAutoService:
    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("auto_type", [AutoType.TRAILER, AutoType.TRUCK])
    async def test_create_auto(db_connection: AsyncSession, auto_type: AutoType):
        auto_to_create = await TestAutoServiceHelper.create_auto_model(
            db_connection,
            auto_type,
        )
        auto_service = AutoDI.get_auto_service()
        created_auto = await auto_service.create_auto(db_connection, auto_to_create)
        TestAutoServiceHelper.validate_models_same(auto_to_create, created_auto)

    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("auto_type", [AutoType.TRAILER, AutoType.TRUCK])
    async def test_update_auto(db_connection: AsyncSession, auto_type: AutoType):
        auto_service = AutoDI.get_auto_service()

        auto = await TestAutoServiceHelper.create_auto_model(db_connection, auto_type)
        await auto_service.create_auto(db_connection, auto)

        auto_to_update = await TestAutoServiceHelper.create_auto_model(
            db_connection,
            auto_type,
        )
        auto_to_update.id = auto.id
        updated_auto = await auto_service.update_auto(db_connection, auto_to_update)
        TestAutoServiceHelper.validate_models_same(auto_to_update, updated_auto)

    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("auto_type", [AutoType.TRAILER, AutoType.TRUCK])
    async def test_get_same_auto_on_same_vin_creation(
        db_connection: AsyncSession,
        auto_type: AutoType,
    ):
        auto_service = AutoDI.get_auto_service()

        first_auto = await TestAutoServiceHelper.create_auto_model(
            db_connection,
            auto_type,
        )
        created_first_auto = await auto_service.create_auto(db_connection, first_auto)

        second_auto = await TestAutoServiceHelper.create_auto_model(
            db_connection,
            auto_type,
        )
        second_auto.vin = first_auto.vin
        created_second_auto = await auto_service.create_auto(db_connection, second_auto)

        assert created_first_auto.id == created_second_auto.id
        assert created_second_auto.model == second_auto.model

    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("auto_type", [AutoType.TRAILER, AutoType.TRUCK])
    async def test_get_same_auto_on_same_number_creation(
        db_connection: AsyncSession,
        auto_type: AutoType,
    ):
        auto_service = AutoDI.get_auto_service()

        first_auto = await TestAutoServiceHelper.create_auto_model(
            db_connection,
            auto_type,
        )
        created_first_auto = await auto_service.create_auto(db_connection, first_auto)

        second_auto = await TestAutoServiceHelper.create_auto_model(
            db_connection,
            auto_type,
        )
        second_auto.number = first_auto.number
        created_second_auto = await auto_service.create_auto(db_connection, second_auto)

        assert created_first_auto.id == created_second_auto.id
        assert created_second_auto.model == second_auto.model


class TestAutoServiceHelper:
    @staticmethod
    def validate_models_same(model_1: Auto, model_2: Auto):
        assert model_1.customer
        assert model_1.driver
        assert model_2.customer
        assert model_2.driver

        assert model_2.customer.id == model_1.customer.id
        assert model_2.driver.id == model_1.driver.id

        assert model_2.brand == model_1.brand
        assert model_2.model == model_1.model
        assert model_2.vin == model_1.vin
        assert model_2.number == model_1.number
        assert model_2.type == model_1.type

    @staticmethod
    async def create_auto_model(
        db_connection: AsyncSession,
        auto_type: AutoType,
    ) -> Auto:
        customer = await TestAutoServiceHelper._create_user_by_role(
            db_connection,
            UserRole.CUSTOMER,
        )
        driver = await TestAutoServiceHelper._create_user_by_role(
            db_connection,
            UserRole.DRIVER,
        )

        auto = Auto()
        auto.customer = customer
        auto.driver = driver
        auto.brand = faker.text()
        auto.model = faker.text()
        auto.vin = faker.text()
        auto.number = faker.text()
        auto.type = auto_type

        return auto

    @staticmethod
    async def _create_user_by_role(db: AsyncSession, role: UserRole) -> User:
        users_service = UsersDI.get_users_service()
        customer_testing_helper = UserTestingHelper()

        auth_data = await customer_testing_helper.sign_up(db, UserRole.CUSTOMER)
        await customer_testing_helper.update_role(db, role)
        return await users_service.get_user_by_id(db, auth_data.user_id)
