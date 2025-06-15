from decimal import Decimal
from sqlalchemy.ext.asyncio import AsyncSession
from httpx import AsyncClient
from faker import Faker
from unittest.mock import MagicMock
import uuid
from urllib.parse import quote
import pytest
from fastapi import status

from src.main_app import create_app
from src.tools.database.dependencies import DatabaseDI
from src.users.constants import MASTER_START_BALANCE
from src.users.enums import UserRole
from src.tools.mail.service import EmailService, get_email_service
from src.users.dependencies import UsersDI
from src.users.testing.helper import UserTestingHelper


faker = Faker()


class TestUsersService:
    @staticmethod
    @pytest.mark.asyncio()
    async def test_reset_password(
        db_connection: AsyncSession,
    ):
        app = create_app()

        async with AsyncClient(app=app, base_url="http://localhost") as http_client:
            # create customer
            email = str(uuid.uuid4()) + faker.email()
            role = UserRole.CUSTOMER
            user_testing_helper = UserTestingHelper(email=email)
            await user_testing_helper.sign_up(db_connection, role)

            # create email mocks
            fake_reset_code = str(uuid.uuid4())
            email_service = EmailService()
            email_service.send_email = MagicMock()  # type: ignore

            def get_email_service_with_mock():
                return email_service

            app.dependency_overrides[get_email_service] = get_email_service_with_mock

            # attach fake reset code
            users_service = UsersDI.get_users_service()
            users_service.set_fake_reset_code(fake_reset_code)

            # send reset code
            response = await http_client.get(
                f"/api/users/reset-code?email={email}&role={role.value}",
            )
            assert response.status_code == status.HTTP_200_OK
            email_service.send_email.assert_called()

            # not registered email
            response = await http_client.get(
                f"/api/users/reset-code?email={str(uuid.uuid4()) + faker.email()}&role={role.value}",
            )
            assert response.status_code == status.HTTP_404_NOT_FOUND

            # create passwords to update
            new_password = faker.password()
            wrong_reset_code = str(uuid.uuid4())

            # test wrong reset code
            response = await http_client.get(
                f"/api/users/reset-password?email={email}&code={wrong_reset_code}&password={quote(new_password)}&role={role.value}",
            )
            assert response.status_code == status.HTTP_400_BAD_REQUEST

            # test valid reset code
            response = await http_client.get(
                f"/api/users/reset-password?email={email}&code={fake_reset_code}&password={quote(new_password)}&role={role.value}",
            )
            assert response.status_code == status.HTTP_200_OK

            # validate sign in with new password
            response = await http_client.post(
                "/api/users/signin",
                json={
                    "email": email,
                    "password": new_password,
                    "role": role,
                },
            )
            assert response.status_code == status.HTTP_200_OK

    @staticmethod
    @pytest.mark.asyncio()
    async def test_increase_balance(db_connection: AsyncSession):
        users_service = UsersDI.get_users_service()
        user_testing_helper = UserTestingHelper()

        auth_data = await user_testing_helper.sign_up(db_connection, UserRole.MASTER)

        INCREASE_BALANCE_AMOUNT = Decimal("500.50")

        await users_service.increase_user_balance_no_commit(
            db_connection,
            auth_data.user_id,
            INCREASE_BALANCE_AMOUNT,
        )
        await db_connection.commit()

        user = await users_service.get_user_by_id(db_connection, auth_data.user_id)
        assert user.balance == MASTER_START_BALANCE + INCREASE_BALANCE_AMOUNT

    @staticmethod
    @pytest.mark.asyncio()
    async def test_decrease_balance(db_connection: AsyncSession):
        users_service = UsersDI.get_users_service()
        user_testing_helper = UserTestingHelper()

        auth_data = await user_testing_helper.sign_up(db_connection, UserRole.MASTER)

        INCREASE_BALANCE_AMOUNT = Decimal("500.50")
        await users_service.increase_user_balance_no_commit(
            db_connection,
            auth_data.user_id,
            INCREASE_BALANCE_AMOUNT,
        )
        await db_connection.commit()

        DECREASE_BALANCE_AMOUNT = Decimal("125.33")
        await users_service.decrease_user_balance_no_commit(
            db_connection,
            auth_data.user_id,
            DECREASE_BALANCE_AMOUNT,
        )
        await db_connection.commit()

        user = await users_service.get_user_by_id(db_connection, auth_data.user_id)
        assert (
            user.balance
            == MASTER_START_BALANCE + INCREASE_BALANCE_AMOUNT - DECREASE_BALANCE_AMOUNT
        )

    @staticmethod
    @pytest.mark.asyncio()
    async def test_cannot_decrease_not_enought_balance(db_connection: AsyncSession):
        is_not_enough_balance_error_raised = False

        users_service = UsersDI.get_users_service()
        user_testing_helper = UserTestingHelper()

        auth_data = await user_testing_helper.sign_up(db_connection, UserRole.MASTER)

        INCREASE_BALANCE_AMOUNT = Decimal("100.50")
        await users_service.increase_user_balance_no_commit(
            db_connection,
            auth_data.user_id,
            INCREASE_BALANCE_AMOUNT,
        )
        await db_connection.commit()

        try:
            DECREASE_BALANCE_AMOUNT = MASTER_START_BALANCE + Decimal("125.33")
            await users_service.decrease_user_balance_no_commit(
                db_connection,
                auth_data.user_id,
                DECREASE_BALANCE_AMOUNT,
            )
        except Exception:
            is_not_enough_balance_error_raised = True

        assert is_not_enough_balance_error_raised == True
