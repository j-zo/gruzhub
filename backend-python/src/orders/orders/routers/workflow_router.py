from fastapi import Header, APIRouter, Depends
from typing import Annotated
from sqlalchemy.ext.asyncio import AsyncSession

from src.orders.orders.services.workflow_service import OrdersWorkflowService

from src.util.request_utils import RequestUtils

from ..dependencies import OrdersDI
from src.tools.database.database import Database
from .. import schemas


router = APIRouter(prefix="/api")
database = Database()


@router.post("/orders/create/")
async def create_order(
    create_order: schemas.CreateOrder,
    authorization: Annotated[str | None, Header()] = None,
    request_id: str = Depends(RequestUtils.get_request_id),
    db: AsyncSession = Depends(database.get_connection_per_request),
    workflow_service: OrdersWorkflowService = Depends(
        OrdersDI.get_orders_workflow_service,
    ),
):
    return await workflow_service.create_order(
        db,
        authorization,
        request_id,
        create_order,
    )


@router.get("/orders/{order_id}/start_calculation_by_master")
async def start_calculation_by_master(
    authorization: Annotated[str, Header()],
    order_id: int,
    request_id: str = Depends(RequestUtils.get_request_id),
    db: AsyncSession = Depends(database.get_connection_per_request),
    workflow_service: OrdersWorkflowService = Depends(
        OrdersDI.get_orders_workflow_service,
    ),
):
    return await workflow_service.start_calculation_by_master(
        db,
        authorization,
        request_id,
        order_id,
    )


@router.post("/orders/{order_id}/decline_order_master")
async def decline_order_master(
    authorization: Annotated[str, Header()],
    order_id: int,
    request: schemas.DeclineOrderRequest,
    request_id: str = Depends(RequestUtils.get_request_id),
    db: AsyncSession = Depends(database.get_connection_per_request),
    workflow_service: OrdersWorkflowService = Depends(
        OrdersDI.get_orders_workflow_service,
    ),
):
    return await workflow_service.decline_order_master(
        db,
        authorization,
        request_id,
        order_id,
        request.comment,
    )


@router.get("/orders/{order_id}/send_for_confirmation_by_master")
async def send_for_comfirmation_by_master(
    authorization: Annotated[str, Header()],
    order_id: int,
    request_id: str = Depends(RequestUtils.get_request_id),
    db: AsyncSession = Depends(database.get_connection_per_request),
    workflow_service: OrdersWorkflowService = Depends(
        OrdersDI.get_orders_workflow_service,
    ),
):
    return await workflow_service.send_for_confirmation_by_master(
        db,
        authorization,
        request_id,
        order_id,
    )


@router.get("/orders/{order_id}/accept_by_customer")
async def accept_by_customer(
    authorization: Annotated[str, Header()],
    order_id: int,
    request_id: str = Depends(RequestUtils.get_request_id),
    db: AsyncSession = Depends(database.get_connection_per_request),
    workflow_service: OrdersWorkflowService = Depends(
        OrdersDI.get_orders_workflow_service,
    ),
):
    return await workflow_service.accept_by_customer(
        db,
        authorization,
        request_id,
        order_id,
    )


@router.get("/orders/{order_id}/complete_order")
async def complete_order(
    authorization: Annotated[str, Header()],
    order_id: int,
    request_id: str = Depends(RequestUtils.get_request_id),
    db: AsyncSession = Depends(database.get_connection_per_request),
    workflow_service: OrdersWorkflowService = Depends(
        OrdersDI.get_orders_workflow_service,
    ),
):
    return await workflow_service.complete_order(
        db,
        authorization,
        request_id,
        order_id,
    )


@router.post("/orders/{order_id}/cancel_order")
async def cancel_order(
    authorization: Annotated[str, Header()],
    order_id: int,
    request: schemas.DeclineOrderRequest,
    request_id: str = Depends(RequestUtils.get_request_id),
    db: AsyncSession = Depends(database.get_connection_per_request),
    workflow_service: OrdersWorkflowService = Depends(
        OrdersDI.get_orders_workflow_service,
    ),
):
    return await workflow_service.cancel_order(
        db,
        authorization,
        request_id,
        order_id,
        request.comment,
    )
