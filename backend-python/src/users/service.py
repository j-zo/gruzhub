import bcrypt
from decimal import Decimal
from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
import uuid
import jwt
import asyncio
from src.telegram.services.telegram_service import TelegramService

from src.util.time_helper import TimeHelper
from src.constants import APPLICATION_SERVER
from src.users.repository import UsersRepository
from src.util.time_helper import TimeHelper
from . import models, schemas
from .constants import (
    ADMIN_EMAIL,
    ADMIN_NAME,
    ADMIN_PHONE,
    JWT_SECRET_KEY,
    MASTER_START_BALANCE,
)
from .enums import UserRole
from src.tools.mail.service import EmailService
from src.telegram import schemas as telegram_schemas

from src.addresses.addresses import (
    schemas as addresses_schemas,
    service as addresses_service,
)


class UsersService:
    def __init__(
        self,
        users_repository: UsersRepository,
        address_service: addresses_service.AddressesService,
        telegram_service: TelegramService,
    ):
        self._users_repository = users_repository
        self._address_service = address_service
        self._telegram_service = telegram_service
        self.fake_reset_code_for_testing: str | None = None

    def set_fake_reset_code(self, fake_reset_code_for_testing: str):
        self.fake_reset_code_for_testing = fake_reset_code_for_testing

    async def sign_up(
        self,
        db: AsyncSession,
        signup_request: schemas.CreateUserRequest,
    ):
        if await self._is_user_exist(
            db=db,
            email=signup_request.email,
            phone=signup_request.phone,
            role=signup_request.role,
        ):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Почта или номер уже зарегистрирована",
            )

        if signup_request.role == UserRole.ADMIN:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Администратор не может быть зарегистрирован",
            )

        if signup_request.role == UserRole.DRIVER:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Водитель не может быть зарегистрирован",
            )

        user = models.User()
        user.role = signup_request.role

        user.email = signup_request.email
        user.phone = signup_request.phone

        if user.role == UserRole.MASTER:
            user.balance = MASTER_START_BALANCE
        else:
            user.balance = Decimal(0)

        user.name = signup_request.name
        user.inn = signup_request.inn
        user.trip_radius_km = signup_request.trip_radius_km

        address_schema = addresses_schemas.Address(
            region_id=signup_request.region_id,
            city=signup_request.city,
            street=signup_request.street,
        )
        user.address = await self._address_service.create_address(db, address_schema)

        user.registration_date = TimeHelper.now_ms()
        user.password_hash = self._generate_password_hash(signup_request.password)
        user.password_creation_time = TimeHelper.now_ms()

        user = await self._users_repository.create(db, user)

    async def create_user(
        self,
        db: AsyncSession,
        name: str,
        phone: str,
        email: str | None,
        role: UserRole,
    ):
        if await self._is_user_exist(
            db=db,
            email=email,
            phone=phone,
            role=role,
        ):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Почта или номер уже зарегистрирована",
            )

        if role == UserRole.ADMIN:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Администратор не может быть зарегистрирован",
            )

        user = models.User()
        user.role = role
        user.name = name
        user.email = email
        user.phone = phone
        user.balance = Decimal(0)
        user.registration_date = TimeHelper.now_ms()

        return await self._users_repository.create(db, user)

    async def sign_in(
        self,
        db: AsyncSession,
        sign_in_request: schemas.SignInUserRequest,
    ) -> schemas.SignInUserResponse:
        if not sign_in_request.email and not sign_in_request.phone:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Ни телефон, ни почта не указаны",
            )

        if sign_in_request.email:
            user = await self._users_repository.get_user_by_email(
                db,
                sign_in_request.email,
                UserRole.ADMIN,
            )

            if not user:
                user = await self._users_repository.get_user_by_email(
                    db,
                    sign_in_request.email,
                    sign_in_request.role,
                )

        if sign_in_request.phone:
            user = await self._users_repository.get_user_by_phone(
                db,
                sign_in_request.phone,
                UserRole.ADMIN,
            )

            if not user:
                user = await self._users_repository.get_user_by_phone(
                    db,
                    sign_in_request.phone,
                    sign_in_request.role,
                )

        if user is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Пользователь не найден",
            )

        # for the case if user registered from external service
        if user.password_hash is None:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Пароль не найден в базе",
            )

        if not await self._is_password_valid(
            sign_in_request.password,
            user.password_hash,
        ):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Пароль не подходит",
            )

        access_token = self.generate_access_token(user)
        return schemas.SignInUserResponse(id=user.id, access_token=access_token)

    async def update_request(
        self,
        db: AsyncSession,
        update_request: schemas.UpdateUserRequest,
        authorization: str,
    ):
        authorized_user = await self.get_user_model_from_token(db, authorization)
        await self.update(db, authorized_user, update_request)

    async def update(  # noqa: PLR0912, PLR0915
        self,
        db: AsyncSession,
        authorized_user: models.User | None,
        update_request: schemas.UpdateUserRequest,
    ):
        try:
            if not authorized_user:
                raise HTTPException(status.HTTP_403_FORBIDDEN)

            user = await self._users_repository.get_user_by_id(db, update_request.id)
            assert user
            inital_chat_codes = user.user_chats_codes
            if not inital_chat_codes:
                inital_chat_codes = ""

            if authorized_user.id != user.id and authorized_user.role != UserRole.ADMIN:
                raise HTTPException(status_code=status.HTTP_403_FORBIDDEN)

            user_info_change = self._create_user_info_change(user, update_request)

            if update_request.name:
                user.name = update_request.name
            if update_request.inn:
                user.inn = update_request.inn

            if update_request.email and update_request.email != user.email:
                if await self._is_user_exist(
                    db=db,
                    email=update_request.email,
                    role=UserRole(user.role),
                ):
                    raise HTTPException(
                        status_code=status.HTTP_400_BAD_REQUEST,
                        detail="Такая почта уже зарегистрирована",
                    )
                user.email = update_request.email

            if update_request.phone and update_request.phone != user.phone:
                if await self._is_user_exist(
                    db=db,
                    phone=update_request.phone,
                    role=UserRole(user.role),
                ):
                    raise HTTPException(
                        status_code=status.HTTP_400_BAD_REQUEST,
                        detail="Такой телефон уже зарегистрирован",
                    )
                user.phone = update_request.phone

            if update_request.password:
                user.password_hash = self._generate_password_hash(
                    update_request.password,
                )
                user.password_creation_time = TimeHelper.now_ms()

            if update_request.trip_radius_km:
                user.trip_radius_km = update_request.trip_radius_km

            if update_request.region_id or update_request.city or update_request.street:
                if user.address:
                    current_address = await self._address_service.get_address_by_id(
                        db,
                        user.address.id,
                    )
                    if update_request.city:
                        current_address.city = update_request.city
                    if update_request.street:
                        current_address.street = update_request.street
                    if update_request.region_id:
                        current_address.region_id = update_request.region_id
                        await self._address_service.update_address(db, current_address)
                elif update_request.region_id:
                    address = addresses_schemas.Address(
                        region_id=update_request.region_id,
                        city=update_request.city,
                        street=update_request.street,
                    )
                    address_model = await self._address_service.update_address(
                        db,
                        address,
                    )
                    user.address = address_model

            if user_info_change:
                db.add(user_info_change)

            if update_request.user_chats_codes:
                for chat_code in update_request.user_chats_codes:
                    if not await self._telegram_service.get_telegram_chat_by_uuid(
                        db,
                        chat_code,
                    ):
                        raise HTTPException(
                            status.HTTP_400_BAD_REQUEST,
                            f"Чат с кодом {chat_code} не найден",
                        )

                user.user_chats_codes = (
                    ",".join(update_request.user_chats_codes)
                    if update_request.user_chats_codes
                    else ""
                )

            await db.commit()
        except Exception as e:
            await db.rollback()
            raise e

        try:
            new_chat_codes = user.user_chats_codes.split(',') if user.user_chats_codes else []
            for chat_code in new_chat_codes:
                if chat_code not in inital_chat_codes:
                    telegram_chat = (
                        await self._telegram_service.get_telegram_chat_by_uuid(
                            db,
                            chat_code,
                        )
                    )
                    assert telegram_chat

                    await self._telegram_service.send_message(
                        telegram_chat_id=telegram_chat.telegram_chat_id,
                        message=f"Чат подключён к GruzHub",
                    )
        except Exception as e:
            raise HTTPException(
                status.HTTP_400_BAD_REQUEST,
                "Не вышло отправить сообщение в Telegram",
            )

    async def get_user_by_id_with_auth(
        self,
        db: AsyncSession,
        authorization: str,
        user_id: int,
    ):
        authorized_user = await self.get_user_from_token(db, authorization)
        user = await self._users_repository.get_user_by_id(db, user_id)

        if not user:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND)

        if authorized_user.id != user.id and authorized_user.role != UserRole.ADMIN:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN)

        return schemas.UserResponse(**user.to_dict())

    async def get_user_by_id(
        self,
        db: AsyncSession,
        user_id: int,
        *,
        is_for_update=False,
    ):
        return await self._users_repository.get_user_by_id(
            db,
            user_id,
            is_for_update=is_for_update,
        )

    async def get_user_by_email(
        self,
        db: AsyncSession,
        email: str,
        role: UserRole,
    ) -> models.User | None:
        return await self._users_repository.get_user_by_email(
            db,
            email,
            role,
        )

    async def get_user_by_phone(
        self,
        db: AsyncSession,
        phone: str,
        role: UserRole,
    ) -> models.User | None:
        return await self._users_repository.get_user_by_phone(db, phone, role)

    async def get_user_from_token(
        self,
        db: AsyncSession,
        token: str,
    ) -> schemas.UserResponse:
        user = await self.get_user_model_from_token(db, token)
        return schemas.UserResponse(**user.to_dict())

    async def get_user_model_from_token(
        self,
        db: AsyncSession,
        token: str,
    ):
        try:
            payload = jwt.decode(
                token.encode("utf-8"),
                JWT_SECRET_KEY,
                algorithms=["HS256"],
            )
        except Exception:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED)

        user_id: int = payload["id"]
        password_creation_time: int = payload["password_creation_time"]

        user = await self._users_repository.get_user_by_id(db, user_id)

        if user is None or user.password_creation_time != password_creation_time:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED)

        return user

    async def increase_user_balance_no_commit(
        self,
        db,
        user_id: int,
        amount: Decimal,
    ) -> models.User:
        user = await self._users_repository.get_user_by_id(
            db,
            user_id,
            is_for_update=True,
        )

        if not user:
            raise HTTPException(status.HTTP_404_NOT_FOUND)

        user.balance = user.balance + amount
        return user

    async def decrease_user_balance_no_commit(
        self,
        db,
        user_id: int,
        amount: Decimal,
    ) -> models.User:
        user = await self._users_repository.get_user_by_id(
            db,
            user_id,
            is_for_update=True,
        )

        if not user:
            raise HTTPException(status.HTTP_404_NOT_FOUND)

        if user.balance - amount >= 0:
            user.balance = user.balance - amount
            return user

        raise HTTPException(
            status.HTTP_400_BAD_REQUEST,
            detail="На балансе не хватает средств",
        )

    async def send_reset_code(
        self,
        db: AsyncSession,
        email_service: EmailService,
        email: str,
        role: UserRole,
    ):
        user = await self._users_repository.get_user_by_email(db, email, role)

        if user is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Пользователь не найден",
            )

        user.user_reset_code = str(uuid.uuid4())
        if self.fake_reset_code_for_testing:
            user.user_reset_code = self.fake_reset_code_for_testing

        await self._users_repository.update(db, user)

        try:
            await asyncio.to_thread(
                email_service.send_email,
                to=email,
                subject="Восстановление пароля",
                message="<html><body><p>Перейдите по ссылке для восстановления пароля - "
                + f'<a href="{APPLICATION_SERVER}/reset-password?email={user.email}&code={user.user_reset_code}">'
                + f"{APPLICATION_SERVER}/reset-password?email={user.email}&code={user.user_reset_code}&role={role.value}"
                + "</a></p></body></html>",
            )
        except Exception as e:
            print(f"Reset password error: {str(e)}")

    async def reset_password(
        self,
        db: AsyncSession,
        email: str,
        code: str,
        password: str,
        role: UserRole,
    ):
        user = await self._users_repository.get_user_by_email(db, email, role)
        if user is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Пользователь не найден",
            )

        if user.user_reset_code != code:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Код не подходит",
            )

        user.password_hash = self._generate_password_hash(password)
        user.password_creation_time = TimeHelper.now_ms()
        user.user_reset_code = None
        await self._users_repository.update(db, user)

    async def validate_auth_role(
        self,
        db: AsyncSession,
        token: str,
        roles: list[UserRole],
    ) -> bool:
        user = await self.get_user_from_token(db, token)
        if user.role in roles:
            return True

        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="У текущей роли нет доступа",
        )

    async def get_user_access(
        self,
        db: AsyncSession,
        access_token: str,
        user_id: int,
    ) -> schemas.SignInUserResponse:
        authorized_user = await self.get_user_from_token(db, access_token)
        if authorized_user.role != UserRole.ADMIN:
            raise HTTPException(status.HTTP_403_FORBIDDEN)

        return await self.get_user_access_no_auth(db, user_id)

    async def get_user_access_no_auth(
        self,
        db: AsyncSession,
        user_id: int,
    ) -> schemas.SignInUserResponse:
        user = await self.get_user_by_id(db, user_id)
        access_token = self.generate_access_token(user)

        return schemas.SignInUserResponse(id=user.id, access_token=access_token)

    def generate_access_token(self, user: models.User) -> str:
        return jwt.encode(
            {"id": user.id, "password_creation_time": user.password_creation_time},
            JWT_SECRET_KEY,
            algorithm="HS256",
        )

    async def get_masters_with_telegram_in_region(
        self,
        db: AsyncSession,
        region_id,
    ) -> list[models.User]:
        return await self._users_repository.get_masters_in_region_with_telegram(
            db,
            region_id,
        )

    async def get_admins(
        self,
        db: AsyncSession,
    ) -> list[models.User]:
        return await self._users_repository.get_admins(db)

    async def connect_telegram(
        self,
        db: AsyncSession,
        authorization: str,
        telegram_request: telegram_schemas.ConnectTelegramRequest,
    ):
        authorized_user = await self.get_user_model_from_token(db, authorization)
        self._telegram_service.validate_telegram_authority(telegram_request)

        try:
            await self._telegram_service.send_message(
                telegram_chat_id=telegram_request.id,
                message=f"Подключен {authorized_user.name} ({authorized_user.phone})",
            )

            user = await self.get_user_by_id(db, authorized_user.id)
            user.telegram_id = telegram_request.id
            await db.commit()
        except Exception:
            raise HTTPException(
                status.HTTP_400_BAD_REQUEST,
                detail="Не вышло отправить сообщение. Проверьте, предоставили ли вы доступ на отправку сообщений",
            )

    async def get_users(
        self,
        db: AsyncSession,
        authorization: str,
        get_users_request: schemas.GetUsersRequest,
    ) -> list[schemas.UserResponse]:
        authorized_user = await self.get_user_from_token(db, authorization)

        if authorized_user.role != UserRole.ADMIN:
            raise HTTPException(status.HTTP_403_FORBIDDEN)

        users_models = await self._users_repository.get_users(
            db,
            regions_ids=get_users_request.regions_ids,
            roles=get_users_request.roles,
        )
        return [schemas.UserResponse(**user.to_dict()) for user in users_models]

    async def get_user_info_changes(
        self,
        db: AsyncSession,
        user_id: int,
    ) -> list[schemas.UserInfoChange]:
        query_result = await db.execute(
            select(models.UserInfoChange)
            .filter(models.UserInfoChange.user_id == user_id)
            .order_by(models.UserInfoChange.id.desc()),
        )
        info_changes = list(query_result.scalars())
        return [schemas.UserInfoChange(**info.to_dict()) for info in info_changes]

    async def get_users_by_ids(
        self,
        db: AsyncSession,
        users_ids: list[int],
    ) -> list[schemas.UserResponse]:
        users = await self._users_repository.get_users_by_ids(db, users_ids)
        return [schemas.UserResponse(**user.to_dict()) for user in users]

    async def create_admin_user_if_not_exist(
        self,
        db: AsyncSession,
    ):
        if not await self._users_repository.get_user_by_email(
            db,
            ADMIN_EMAIL,
            UserRole.ADMIN,
        ):
            user = models.User()
            user.role = UserRole.ADMIN
            user.name = ADMIN_NAME
            user.email = ADMIN_EMAIL
            user.phone = ADMIN_PHONE
            user.balance = Decimal(0)
            user.registration_date = TimeHelper.now_ms()

            await self._users_repository.create(db, user)

    async def _is_user_exist(
        self,
        *,
        db: AsyncSession,
        email: str | None = None,
        phone: str | None = None,
        role: UserRole,
    ) -> bool:
        if not email and not phone:
            raise Exception("Ни телефон, ни почта не указаны")

        is_user_registened = False

        if email:
            user = await self._users_repository.get_user_by_email(db, email, role)
            if user is not None:
                is_user_registened = True

        if phone:
            user = await self._users_repository.get_user_by_phone(db, phone, role)
            if user is not None:
                is_user_registened = True

        return is_user_registened

    async def _is_password_valid(self, password: str, password_hash: str) -> bool:
        password_bytes = password.encode("utf-8")
        hash_bytes = password_hash.encode("utf-8")
        return bcrypt.checkpw(password_bytes, hash_bytes)

    def _generate_password_hash(self, password: str) -> str:
        password_bytes = password.encode("utf-8")
        salt = bcrypt.gensalt(10)
        hash_bytes = bcrypt.hashpw(password_bytes, salt)
        return hash_bytes.decode("utf-8")

    def _create_user_info_change(
        self,
        user: models.User,
        update_request: schemas.UpdateUserRequest,
    ) -> models.UserInfoChange | None:
        if (
            update_request.name != user.name
            or update_request.phone != user.phone
            or update_request.email != user.email
            or update_request.inn != user.id
        ):
            user_info_change = models.UserInfoChange()
            user_info_change.user = user
            user_info_change.date = TimeHelper.now_ms()

            user_info_change.previous_name = user.name
            user_info_change.new_name = (
                update_request.name if update_request.name else user.name
            )

            user_info_change.previous_phone = user.phone
            user_info_change.new_phone = (
                update_request.phone if update_request.phone else user.phone
            )

            user_info_change.previous_email = user.email
            user_info_change.new_email = (
                update_request.email if update_request.email else user.email
            )

            user_info_change.previous_inn = user.inn
            user_info_change.new_inn = (
                update_request.inn if update_request.inn else user.inn
            )

            return user_info_change

        return None
