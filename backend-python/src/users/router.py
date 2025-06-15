from typing import Annotated
from fastapi import Depends, APIRouter, Header
from sqlalchemy.ext.asyncio import AsyncSession

from src.util.request_utils import RequestUtils

from .dependencies import UsersDI
from . import schemas
from .enums import UserRole
from .service import UsersService
from src.tools.database.database import Database
from src.tools.mail.service import EmailService, get_email_service
from src.telegram import schemas as telegram_schemas

router = APIRouter(prefix="/api")
database = Database()


@router.post("/users/signup")
async def sign_up(
    user: schemas.CreateUserRequest,
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: UsersService = Depends(UsersDI.get_users_service),
):
    return await service.sign_up(db, user)


@router.post("/users/signin", response_model=schemas.SignInUserResponse)
async def sign_in(
    user: schemas.SignInUserRequest,
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: UsersService = Depends(UsersDI.get_users_service),
):
    return vars(await service.sign_in(db, user))


@router.post("/users/update")
async def update(
    user: schemas.UpdateUserRequest,
    authorization: Annotated[str, Header()],
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: UsersService = Depends(UsersDI.get_users_service),
):
    return await service.update_request(db, user, authorization)


@router.get("/users/reset-code")
async def send_reset_code(
    email: str,
    role: str,
    db: AsyncSession = Depends(database.get_connection_per_request),
    email_service: EmailService = Depends(get_email_service),
    service: UsersService = Depends(UsersDI.get_users_service),
):
    await service.send_reset_code(db, email_service, email, UserRole(role))


@router.get("/users/reset-password")
async def reset_password(
    email: str,
    role: str,
    code: str,
    password: str,
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: UsersService = Depends(UsersDI.get_users_service),
):
    await service.reset_password(db, email, code, password, UserRole(role))


@router.get("/users/{user_id}", response_model=schemas.UserResponse)
async def get_user(
    user_id: int,
    authorization: Annotated[str, Header()],
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: UsersService = Depends(UsersDI.get_users_service),
):
    return await service.get_user_by_id_with_auth(db, authorization, user_id)


@router.get("/users/get-access/{user_id}")
async def get_user_access(
    user_id: int,
    authorization: Annotated[str, Header()],
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: UsersService = Depends(UsersDI.get_users_service),
):
    return await service.get_user_access(db, authorization, user_id)


@router.post("/users/connect-telegram")
async def connect_request(
    authorization: Annotated[str, Header()],
    connect_request: telegram_schemas.ConnectTelegramRequest,
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: UsersService = Depends(UsersDI.get_users_service),
):
    return await service.connect_telegram(
        db,
        authorization,
        connect_request,
    )


@router.post("/users/users")
async def get_users(
    get_users_request: schemas.GetUsersRequest,
    authorization: Annotated[str, Header()],
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: UsersService = Depends(UsersDI.get_users_service),
):
    return await service.get_users(db, authorization, get_users_request)
