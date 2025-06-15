from __future__ import annotations
from src.tools.files.schemas import File
from src.users.enums import UserRole
from src.util.camel_case_schema import CamelCaseSchema


class SendMessageRequest(CamelCaseSchema):
    guarantee_id: str
    order_id: int
    text: str


class GetLastMessagePerOrder(CamelCaseSchema):
    orders_ids: list[int]


class OrderMessage(CamelCaseSchema):
    id: int
    order_id: int
    user_id: int
    user_role: UserRole
    text: str | None = None
    date: int
    file: File | None = None
    file_code: str | None = None
    is_viewed_by_master: bool
    is_viewed_by_driver: bool
    is_viewed_by_customer: bool
