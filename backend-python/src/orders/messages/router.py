from fastapi import APIRouter, Header, Depends, File
from typing import Annotated
from sqlalchemy.ext.asyncio import AsyncSession
from src.orders.messages.dependencies import OrderMessagesDI
from src.orders.messages.service import OrderMessagesService

from src.tools.database.dependencies import DatabaseDI
from . import schemas

router = APIRouter(prefix="/api")
database = DatabaseDI.get_database()


@router.post("/orders/messages/send")
async def send_message(
    send_message_request: schemas.SendMessageRequest,
    authorization: Annotated[str, Header()],
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: OrderMessagesService = Depends(OrderMessagesDI.get_order_messages_service),
):
    await service.send_message(
        db,
        authorization,
        guarantee_id=send_message_request.guarantee_id,
        order_id=send_message_request.order_id,
        text=send_message_request.text,
    )


@router.post("/orders/messages/send-file")
async def send_file_message(
    guarantee_id: str,
    order_id: int,
    filename: str,
    extension: str,
    file: Annotated[bytes, File()],
    authorization: Annotated[str, Header()],
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: OrderMessagesService = Depends(OrderMessagesDI.get_order_messages_service),
):
    await service.send_message(
        db,
        authorization,
        guarantee_id=guarantee_id,
        order_id=order_id,
        filename=filename,
        extension=extension,
        file=file,
    )


@router.post("/orders/messages/last-messages-per-order")
async def get_last_message_per_each_order(
    get_last_message_per_order_request: schemas.GetLastMessagePerOrder,
    authorization: Annotated[str, Header()],
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: OrderMessagesService = Depends(OrderMessagesDI.get_order_messages_service),
):
    return await service.get_last_message_per_each_order(
        db,
        authorization,
        get_last_message_per_order_request.orders_ids,
    )


@router.get("/orders/messages/get-order-messages/{order_id}")
async def get_order_messages(
    order_id: int,
    authorization: Annotated[str, Header()],
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: OrderMessagesService = Depends(OrderMessagesDI.get_order_messages_service),
):
    return await service.get_order_messages(db, authorization, order_id)


@router.get("/orders/messages/get-order-messages-users/{order_id}")
async def get_order_messages_users(
    order_id: int,
    authorization: Annotated[str, Header()],
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: OrderMessagesService = Depends(OrderMessagesDI.get_order_messages_service),
):
    return await service.get_order_messages_users(db, authorization, order_id)


@router.get("/orders/messages/set-messages-viewed-by-role/{order_id}")
async def set_messages_viewed_by_role(
    order_id: int,
    authorization: Annotated[str, Header()],
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: OrderMessagesService = Depends(OrderMessagesDI.get_order_messages_service),
):
    await service.set_messages_viewed_by_role(db, authorization, order_id)
