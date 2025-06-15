from httpx import AsyncClient
from faker import Faker
import pytest
import random
from fastapi import status
import uuid

from src.users import schemas
from src.users.constants import MASTER_START_BALANCE
from src.users.enums import UserRole
from src.util.time_helper import TimeHelper

faker = Faker()


class TestUsersRouter:
    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("user_role", [UserRole.CUSTOMER, UserRole.MASTER])
    async def test_create_user(
        http_client: AsyncClient,
        user_role: UserRole,
    ):
        user_to_create = TestUsersUserHelper.create_user_schema(user_role)
        await TestUsersUserHelper.signup_user(http_client, user_to_create)

    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("user_role", [UserRole.DRIVER, UserRole.ADMIN])
    async def test_disallowed_roles_cannot_register(
        http_client: AsyncClient,
        user_role: UserRole,
    ):
        user_to_create = TestUsersUserHelper.create_user_schema(user_role)
        is_unreachable_code_runned = False

        try:
            await TestUsersUserHelper.signup_user(http_client, user_to_create)
            is_unreachable_code_runned = True
        except Exception:
            # expected
            pass

        if is_unreachable_code_runned:
            raise Exception("Disallowed role has been registered")

    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("user_role", [UserRole.CUSTOMER, UserRole.MASTER])
    async def test_sign_in_by_email_and_phone(
        http_client: AsyncClient,
        user_role: UserRole,
    ):
        user_to_create = TestUsersUserHelper.create_user_schema(user_role)
        await TestUsersUserHelper.signup_user(http_client, user_to_create)

        response = await TestUsersUserHelper.sign_in(
            http_client,
            email=user_to_create.email,
            password=user_to_create.password,
            role=user_to_create.role,
        )
        assert response.id
        assert response.access_token

        response = await TestUsersUserHelper.sign_in(
            http_client,
            phone=user_to_create.phone,
            password=user_to_create.password,
            role=user_to_create.role,
        )
        assert response.id
        assert response.access_token

    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("user_role", [UserRole.CUSTOMER, UserRole.MASTER])
    async def test_cannot_login_with_wrong_credentials(
        http_client: AsyncClient,
        user_role: UserRole,
    ):
        is_unreachable_code_runned = False

        user_to_create = TestUsersUserHelper.create_user_schema(user_role)
        await TestUsersUserHelper.signup_user(http_client, user_to_create)

        try:
            await TestUsersUserHelper.sign_in(
                http_client,
                email=user_to_create.email,
                password=faker.text(),
                role=user_to_create.role,
            )
            is_unreachable_code_runned = True
        except Exception:
            # expected
            pass

        try:
            await TestUsersUserHelper.sign_in(
                http_client,
                phone=user_to_create.phone,
                password=faker.text(),
                role=user_to_create.role,
            )
            is_unreachable_code_runned = True
        except Exception:
            # expected
            pass

        if is_unreachable_code_runned:
            raise Exception("User logged in with wrong password")

    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("user_role", [UserRole.CUSTOMER, UserRole.MASTER])
    async def test_get_user(http_client: AsyncClient, user_role: UserRole):
        user_to_create = TestUsersUserHelper.create_user_schema(user_role)
        await TestUsersUserHelper.signup_user(http_client, user_to_create)

        user_auth_data = await TestUsersUserHelper.sign_in(
            http_client,
            email=user_to_create.email,
            password=user_to_create.password,
            role=user_role,
        )

        user = await TestUsersUserHelper.get_user(
            http_client,
            user_auth_data.id,
            user_auth_data.access_token,
        )

        TestUsersUserHelper.validate_schemas_identity(user_to_create, user)
        if user.role == UserRole.MASTER:
            assert user.balance == MASTER_START_BALANCE

    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("user_role", [UserRole.CUSTOMER, UserRole.MASTER])
    async def test_cannot_get_another_user(
        http_client: AsyncClient,
        user_role: UserRole,
    ):
        user_to_create = TestUsersUserHelper.create_user_schema(user_role)
        await TestUsersUserHelper.signup_user(http_client, user_to_create)

        user_auth_data = await TestUsersUserHelper.sign_in(
            http_client,
            email=user_to_create.email,
            password=user_to_create.password,
            role=user_role,
        )

        is_unreachable_code_runned = False

        try:
            await TestUsersUserHelper.get_user(
                http_client,
                user_id=random.randint(100_000, 200_000),
                access_token=user_auth_data.access_token,
            )
            is_unreachable_code_runned = True
        except Exception:
            # expected
            pass

        if is_unreachable_code_runned:
            raise Exception("Received unexpected access to another user")

    @staticmethod
    @pytest.mark.asyncio()
    @pytest.mark.parametrize("user_role", [UserRole.CUSTOMER, UserRole.MASTER])
    async def test_update_user(http_client: AsyncClient, user_role: UserRole):
        user_to_create = TestUsersUserHelper.create_user_schema(user_role)
        await TestUsersUserHelper.signup_user(http_client, user_to_create)

        user_auth_data = await TestUsersUserHelper.sign_in(
            http_client,
            email=user_to_create.email,
            password=user_to_create.password,
            role=user_role,
        )

        user_to_update = schemas.UpdateUserRequest(
            id=user_auth_data.id,
            name=faker.text(),
            inn=faker.text(),
            email=user_to_create.email,
            phone=str(TimeHelper.now_ms()) + str(uuid.uuid4()),
            password=faker.text(),
            trip_radius_km=random.randint(1, 100),
            region_id=random.randint(20, 50),
            city=faker.text(),
            street=faker.text(),
        )
        await TestUsersUserHelper.update_user(
            http_client,
            authorization=user_auth_data.access_token,
            user=user_to_update,
        )

        # request new auth, because password was changed
        assert user_to_update.password
        user_auth_data = await TestUsersUserHelper.sign_in(
            http_client,
            email=user_to_create.email,
            password=user_to_update.password,
            role=user_role,
        )
        user = await TestUsersUserHelper.get_user(
            http_client,
            user_auth_data.id,
            user_auth_data.access_token,
        )

        TestUsersUserHelper.validate_schemas_identity(user_to_update, user)


class TestUsersUserHelper:
    @staticmethod
    async def sign_in(
        http_client: AsyncClient,
        *,
        email: str | None = None,
        phone: str | None = None,
        password: str,
        role: UserRole,
    ):
        response = await http_client.post(
            "/api/users/signin",
            json={
                "phone": phone,
                "email": email,
                "password": password,
                "role": role,
            },
        )
        assert response.status_code == status.HTTP_200_OK
        return schemas.SignInUserResponse(**response.json())

    @staticmethod
    async def signup_user(http_client: AsyncClient, user: schemas.CreateUserRequest):
        response = await http_client.post("/api/users/signup", json=user.model_dump())
        assert response.status_code == status.HTTP_200_OK

    @staticmethod
    async def update_user(
        http_client: AsyncClient,
        authorization: str,
        user: schemas.UpdateUserRequest,
    ):
        response = await http_client.post(
            "/api/users/update",
            headers={"Authorization": authorization},
            json=user.model_dump(),
        )
        assert response.status_code == status.HTTP_200_OK

    @staticmethod
    def create_user_schema(role: UserRole) -> schemas.CreateUserRequest:
        return schemas.CreateUserRequest(
            name=faker.text(),
            role=role,
            email=str(uuid.uuid4()) + faker.email(),
            phone=str(TimeHelper.now_ms()) + str(uuid.uuid4()),
            inn=faker.text(),
            password=faker.password(),
            trip_radius_km=random.randint(0, 1000),
            region_id=random.randint(20, 50),
            city=faker.text(),
            street=faker.text(),
        )

    @staticmethod
    def validate_schemas_identity(
        schema_1: schemas.CreateUserRequest | schemas.UpdateUserRequest,
        schema_2: schemas.UserResponse,
    ):
        if type(schema_1) == schemas.CreateUserRequest:
            assert schema_1.role == schema_2.role
        assert schema_1.email == schema_2.email
        assert schema_1.phone == schema_2.phone

        assert schema_1.name == schema_2.name
        assert schema_1.inn == schema_2.inn
        assert schema_1.trip_radius_km == schema_2.trip_radius_km

        assert schema_2.address
        assert schema_2.address.id
        assert schema_1.city == schema_2.address.city
        assert schema_1.street == schema_2.address.street
        assert schema_1.region_id == schema_2.address.region_id

    @staticmethod
    async def get_user(
        http_client: AsyncClient,
        user_id: int,
        access_token: str,
    ) -> schemas.UserResponse:
        user_response = await http_client.get(
            f"/api/users/{user_id}",
            headers={"Authorization": access_token},
        )
        assert user_response.status_code == status.HTTP_200_OK
        return schemas.UserResponse(**user_response.json())
