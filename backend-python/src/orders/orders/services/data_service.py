from fastapi import HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from src.orders.auto.models import Auto
from src.orders.orders.commands.update_auto_command import UpdateAutoCommand
from src.orders.orders.enums import OrderStatus
from ..repository import OrdersRepository
from src.users.service import UsersService
from .. import models, schemas
from src.orders.auto.service import AutoService
from src.users.enums import UserRole
from src.users import schemas as users_schemas


class OrdersDataService:
    def __init__(
        self,
        orders_repository: OrdersRepository,
        users_service: UsersService,
        auto_service: AutoService,
    ):
        self._orders_repository = orders_repository
        self._users_service = users_service
        self._auto_service = auto_service

    async def get_order_by_id(
        self,
        db: AsyncSession,
        authorization: str,
        order_id: int,
    ) -> schemas.OrderResponse:
        order = await self.get_order_model_by_id(db, order_id, authorization)
        return schemas.OrderResponse(**order.to_dict())

    async def get_order_model_by_id(
        self,
        db: AsyncSession,
        order_id: int,
        authorization: str | None = None,
    ) -> models.Order:
        order = await self._orders_repository.get_order_by_id(db, order_id)

        if authorization:
            authorized_user = await self._users_service.get_user_from_token(
                db,
                authorization,
            )

            if authorized_user.role == UserRole.MASTER:
                if not authorized_user.address:
                    raise HTTPException(
                        status.HTTP_500_INTERNAL_SERVER_ERROR,
                        "Master does not have address",
                    )

                if authorized_user.id in [
                    int(master_id)
                    for master_id in order.declined_masters_ids.split(",")
                    if master_id
                ]:
                    raise HTTPException(
                        status.HTTP_403_FORBIDDEN,
                        detail="К сожалению, вы не можете взять этот заказ",
                    )

                if (
                    order.master_id != authorized_user.id
                    or order.address.region_id != authorized_user.address.region_id
                ) and order.status != OrderStatus.CREATED:
                    raise HTTPException(status.HTTP_403_FORBIDDEN)
            elif (
                authorized_user.id not in (order.driver_id, order.customer_id)
                and authorized_user.role != UserRole.ADMIN
            ):
                raise HTTPException(status.HTTP_403_FORBIDDEN)

        return order

    async def get_auto_orders(
        self,
        db: AsyncSession,
        authorization: str,
        auto_id: int,
    ) -> list[schemas.OrderResponse]:
        authorized_user = await self._users_service.get_user_from_token(
            db,
            authorization,
        )
        auto = await self._auto_service.get_auto_by_id(db, auto_id)

        if (
            authorized_user.id not in (auto.driver_id, auto.customer_id)
            and authorized_user.role != UserRole.ADMIN
        ):
            raise HTTPException(status.HTTP_403_FORBIDDEN)

        orders = await self._orders_repository.get_orders(db, auto_id=auto_id)
        return [schemas.OrderResponse(**order.to_dict()) for order in orders]

    async def get_orders(
        self,
        db: AsyncSession,
        authorization: str,
        get_orders_request: schemas.GetOrdersRequest,
    ) -> list[models.Order]:
        authorized_user = await self._users_service.get_user_from_token(
            db,
            authorization,
        )

        if authorized_user.role == UserRole.DRIVER:
            return await self._orders_repository.get_orders(
                db,
                driver_id=authorized_user.id,
                statuses=get_orders_request.statuses,
                limit=get_orders_request.limit,
            )

        if authorized_user.role == UserRole.MASTER:
            if not authorized_user.address:
                raise HTTPException(
                    status.HTTP_500_INTERNAL_SERVER_ERROR,
                    detail="Master does not have an address",
                )

            return await self._orders_repository.get_master_orders(
                db,
                master_id=authorized_user.id,
                region_id=authorized_user.address.region_id,
                statuses=get_orders_request.statuses,
                limit=get_orders_request.limit,
            )

        if authorized_user.role == UserRole.CUSTOMER:
            return await self._orders_repository.get_orders(
                db,
                customer_id=authorized_user.id,
                statuses=get_orders_request.statuses,
                limit=get_orders_request.limit,
            )

        if authorized_user.role == UserRole.ADMIN:
            return await self._orders_repository.get_orders(
                db,
                statuses=get_orders_request.statuses,
                master_id=get_orders_request.master_id,
                customer_id=get_orders_request.customer_id,
                driver_id=get_orders_request.driver_id,
                auto_id=get_orders_request.auto_id,
                regions_ids=get_orders_request.regions_ids,
                user_id=get_orders_request.user_id,
                limit=get_orders_request.limit,
            )

        return []

    async def get_order_auto(
        self,
        db: AsyncSession,
        authorization: str,
        order_id: int,
        auto_id,
    ) -> schemas.AutoResponse:
        auto = await self._auto_service.get_auto_by_id(db, auto_id)
        await self._validate_order_auto_permissions(
            db,
            authorization,
            order_id,
            auto,
        )
        return schemas.AutoResponse(**auto.to_dict())

    async def update_order_auto(
        self,
        db: AsyncSession,
        authorization: str,
        update_auto: schemas.UpdateOrderAutoRequest,
    ):
        auto = await self._auto_service.get_auto_by_id(db, update_auto.auto_id)

        await self._validate_order_auto_permissions(
            db,
            authorization,
            update_auto.order_id,
            auto,
        )

        auto.brand = update_auto.brand
        auto.model = update_auto.model
        auto.vin = update_auto.vin
        auto.number = update_auto.number

        update_auto_command = UpdateAutoCommand(
            self._auto_service,
            self._orders_repository,
        )
        await update_auto_command.update_auto(db, auto)

    async def get_user_info_changes(
        self,
        db: AsyncSession,
        authorization: str,
        *,
        order_id: int,
        user_id: int,
    ) -> list[users_schemas.UserInfoChange]:
        authorized_user = await self._users_service.get_user_from_token(
            db,
            authorization,
        )
        order = await self._orders_repository.get_order_by_id(db, order_id)

        is_authorized_user_in_order = authorized_user.id in (
            order.driver_id,
            order.master_id,
            order.customer_id,
        )
        is_request_user_in_order = user_id in (
            order.driver_id,
            order.master_id,
            order.customer_id,
        )

        if (
            is_authorized_user_in_order and is_request_user_in_order
        ) or authorized_user.role == UserRole.ADMIN:
            return await self._users_service.get_user_info_changes(db, user_id)

        raise HTTPException(status.HTTP_403_FORBIDDEN)

    async def get_order_status_changes(
        self,
        db: AsyncSession,
        authorization: str,
        order_id: int,
    ):
        authorized_user = await self._users_service.get_user_from_token(
            db,
            authorization,
        )
        order = await self._orders_repository.get_order_by_id(db, order_id)

        is_master_access_to_created_order = (
            authorized_user.role == UserRole.MASTER
            and order.status == OrderStatus.CREATED
        )
        is_access_to_existing_order = (
            authorized_user.id in (order.driver_id, order.master_id, order.customer_id)
            or authorized_user.role == UserRole.ADMIN
        )

        if not is_master_access_to_created_order and not is_access_to_existing_order:
            raise HTTPException(status.HTTP_403_FORBIDDEN)

        order_status_changes = await self._orders_repository.get_order_status_changes(
            db,
            order_id,
        )
        return [
            schemas.OrderStatusChange(**change.to_dict())
            for change in order_status_changes
        ]

    async def _validate_order_auto_permissions(
        self,
        db: AsyncSession,
        authorization: str,
        order_id: int,
        auto: Auto,
    ):
        authorized_user = await self._users_service.get_user_from_token(
            db,
            authorization,
        )
        if authorized_user.role == UserRole.ADMIN:
            return

        order = await self._orders_repository.get_order_by_id(db, order_id)

        if (
            authorized_user.role == UserRole.MASTER
            and order.master_id != authorized_user.id
            and order.status != OrderStatus.CREATED
        ):
            raise HTTPException(status.HTTP_403_FORBIDDEN)

        autos_ids = [auto.id for auto in order.autos]
        if auto.id not in autos_ids and authorized_user.id not in (
            auto.customer_id,
            auto.driver_id,
        ):
            raise HTTPException(status.HTTP_403_FORBIDDEN)
