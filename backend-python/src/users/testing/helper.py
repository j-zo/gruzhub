from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from faker import Faker
import uuid
import random

from src.users.enums import UserRole
from src.users.models import User
from src.users.service import UsersService
from src.users.schemas import SignInUserRequest, CreateUserRequest
from src.users.dependencies import UsersDI
from src.util.time_helper import TimeHelper
from .schemas import AuthData


class UserTestingHelper:
    _users_service: UsersService

    _testing_email: str
    _testing_password: str
    _is_admin: bool

    def __init__(
        self,
        email: str | None = None,
        is_admin=False,
    ):
        fake = Faker()
        self._users_service = UsersDI.get_users_service()

        if email is None:
            email = str(TimeHelper.now_ms()) + str(uuid.uuid4()) + fake.email()
        self._testing_email = email

        self._testing_password = fake.password()
        self._is_admin = is_admin

    async def sign_up(
        self,
        db: AsyncSession,
        role: UserRole,
        region_id: int | None = None,
    ) -> AuthData:
        fake = Faker()

        if not region_id:
            region_id = random.randint(20, 50)

        await self._users_service.sign_up(
            db,
            CreateUserRequest(
                name=fake.name(),
                email=self._testing_email,
                password=self._testing_password,
                role=role,
                phone=fake.text(),
                city=fake.text(),
                street=fake.text(),
                region_id=region_id,
            ),
        )

        if self._is_admin:
            await self.update_role(db, UserRole.ADMIN)

        sign_in_response = await self._users_service.sign_in(
            db,
            SignInUserRequest(
                email=self._testing_email,
                password=self._testing_password,
                role=role,
            ),
        )

        return AuthData(
            user_id=sign_in_response.id,
            access_token=sign_in_response.access_token,
        )

    async def update_role(self, db: AsyncSession, role: UserRole):
        statement = select(User).where(User.email == self._testing_email).limit(1)
        query_result = await db.execute(statement)
        user: User | None = query_result.scalar()

        assert user
        user.role = role

        await db.commit()
