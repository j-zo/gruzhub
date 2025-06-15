from fastapi import Header, APIRouter, Depends
from typing import Annotated
from sqlalchemy.ext.asyncio import AsyncSession
from src.orders.orders.services.data_service import OrdersDataService

from ..dependencies import OrdersDI
from src.tools.database.database import Database
from .. import schemas


router = APIRouter(prefix="/api")
database = Database()


@router.post("/orders/orders/")
async def get_orders(
    get_orders_request: schemas.GetOrdersRequest,
    authorization: Annotated[str, Header()],
    db: AsyncSession = Depends(database.get_connection_per_request),
    data_service: OrdersDataService = Depends(OrdersDI.get_orders_data_service),
):
    orders_models = await data_service.get_orders(db, authorization, get_orders_request)
    return [schemas.OrderResponse(**order.to_dict()) for order in orders_models]


@router.get("/orders/auto")
async def get_order_auto(
    authorization: Annotated[str, Header()],
    order_id: int,
    auto_id: int,
    db: AsyncSession = Depends(database.get_connection_per_request),
    data_service: OrdersDataService = Depends(OrdersDI.get_orders_data_service),
):
    return await data_service.get_order_auto(db, authorization, order_id, auto_id)


@router.post("/orders/auto")
async def update_order_auto(
    authorization: Annotated[str, Header()],
    update_auto: schemas.UpdateOrderAutoRequest,
    db: AsyncSession = Depends(database.get_connection_per_request),
    data_service: OrdersDataService = Depends(OrdersDI.get_orders_data_service),
):
    return await data_service.update_order_auto(db, authorization, update_auto)


@router.get("/orders/auto/{auto_id}/")
async def get_auto_orders(
    authorization: Annotated[str, Header()],
    auto_id: int,
    db: AsyncSession = Depends(database.get_connection_per_request),
    data_service: OrdersDataService = Depends(OrdersDI.get_orders_data_service),
):
    return await data_service.get_auto_orders(db, authorization, auto_id)


@router.get("/orders/{order_id}/")
async def get_order_by_id(
    order_id: int,
    authorization: Annotated[str, Header()],
    db: AsyncSession = Depends(database.get_connection_per_request),
    data_service: OrdersDataService = Depends(OrdersDI.get_orders_data_service),
):
    return await data_service.get_order_by_id(db, authorization, order_id)


@router.get("/orders/order-status-changes/{order_id}/")
async def get_order_status_changes(
    order_id: int,
    authorization: Annotated[str, Header()],
    db: AsyncSession = Depends(database.get_connection_per_request),
    data_service: OrdersDataService = Depends(OrdersDI.get_orders_data_service),
):
    return await data_service.get_order_status_changes(db, authorization, order_id)


@router.get("/orders/user-changes")
async def get_user_changes_by_order_id(
    authorization: Annotated[str, Header()],
    order_id: int,
    user_id: int,
    db: AsyncSession = Depends(database.get_connection_per_request),
    data_service: OrdersDataService = Depends(OrdersDI.get_orders_data_service),
):
    return await data_service.get_user_info_changes(
        db,
        authorization,
        order_id=order_id,
        user_id=user_id,
    )
