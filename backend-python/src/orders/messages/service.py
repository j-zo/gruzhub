from fastapi import HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from src.orders.messages.repository import OrderMessagesRepository
from src.orders.orders.models import Order

from src.orders.orders.services.data_service import OrdersDataService
from src.tools.files.service import FilesService
from src.users.enums import UserRole
from src.users.models import User
from src.users.service import UsersService
from src.users import schemas as users_schemas
from src.util.time_helper import TimeHelper
from . import models, schemas


class OrderMessagesService:
    def __init__(
        self,
        users_service: UsersService,
        orders_service: OrdersDataService,
        messages_repository: OrderMessagesRepository,
        files_service: FilesService,
    ):
        self._users_service = users_service
        self._orders_service = orders_service
        self._messages_repository = messages_repository
        self._files_service = files_service

    async def send_message(
        self,
        db: AsyncSession,
        authorization: str,
        *,
        guarantee_id: str,
        order_id: int,
        text: str | None = None,
        file: bytes | None = None,
        filename: str | None = None,
        extension: str | None = None,
    ) -> None:
        user = await self._users_service.get_user_model_from_token(db, authorization)
        order = await self._orders_service.get_order_model_by_id(db, order_id)

        await self._validate_messages_authority(user, order)

        same_message = await self._messages_repository.get_message_by_guarantee_id(
            db,
            order_id,
            guarantee_id,
        )
        if same_message:
            return

        message = models.OrderMessage()
        message.guarantee_id = guarantee_id

        message.order = order
        message.user = user
        message.user_role = user.role

        if text:
            MAX_TEXT_SIZE = 10_000
            if len(text) > MAX_TEXT_SIZE:
                raise HTTPException(
                    status.HTTP_400_BAD_REQUEST,
                    "Слишком много текста",
                )

            message.text = text
        elif file and filename and extension:
            file_model = await self._files_service.create_file(
                db,
                user,
                file,
                filename,
                extension,
            )
            message.file = file_model
        else:
            raise HTTPException(
                status.HTTP_400_BAD_REQUEST,
                "Текст или файл должен быть отправлен с сообщением",
            )

        message.date = TimeHelper.now_ms()
        message.is_viewed_by_customer = user.role == UserRole.CUSTOMER
        message.is_viewed_by_driver = user.role == UserRole.DRIVER
        message.is_viewed_by_master = user.role == UserRole.MASTER

        db.add(message)
        await db.commit()

    async def get_last_message_per_each_order(
        self,
        db: AsyncSession,
        authorization: str,
        orders_ids: list[int],
    ) -> list[schemas.OrderMessage]:
        user = await self._users_service.get_user_model_from_token(db, authorization)
        last_messages = await self._messages_repository.get_last_message_per_each_order(
            db,
            user.id,
            orders_ids,
        )

        def get_message_date(message: models.OrderMessage):
            return message.date

        last_messages.sort(key=get_message_date, reverse=True)

        return [schemas.OrderMessage(**message.to_dict()) for message in last_messages]

    async def get_order_messages(
        self,
        db: AsyncSession,
        authorization: str,
        order_id: int,
    ) -> list[schemas.OrderMessage]:
        user = await self._users_service.get_user_model_from_token(db, authorization)
        order = await self._orders_service.get_order_model_by_id(db, order_id)

        await self._validate_messages_authority(user, order)

        messages = await self._messages_repository.get_order_messages(db, order_id)
        return [schemas.OrderMessage(**message.to_dict()) for message in messages]

    async def get_order_messages_users(
        self,
        db: AsyncSession,
        authorization: str,
        order_id: int,
    ) -> list[users_schemas.UserResponse]:
        user = await self._users_service.get_user_model_from_token(db, authorization)
        order = await self._orders_service.get_order_model_by_id(db, order_id)

        await self._validate_messages_authority(user, order)

        users_ids = await self._messages_repository.get_order_messages_user_ids(
            db,
            order_id,
        )
        return await self._users_service.get_users_by_ids(db, users_ids)

    async def set_messages_viewed_by_role(
        self,
        db: AsyncSession,
        authorization: str,
        order_id: int,
    ) -> None:
        user = await self._users_service.get_user_model_from_token(db, authorization)
        order = await self._orders_service.get_order_model_by_id(db, order_id)

        await self._validate_messages_authority(user, order)

        await self._messages_repository.set_messages_viewed_by_role(
            db,
            order_id,
            user.role,
        )

    async def _validate_messages_authority(self, user: User, order: Order) -> None:
        if user.role != UserRole.ADMIN and user.id not in (
            order.master_id,
            order.customer_id,
            order.driver_id,
        ):
            raise HTTPException(status.HTTP_403_FORBIDDEN)
