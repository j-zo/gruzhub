from decimal import Decimal
from src.util.camel_case_schema import CamelCaseSchema


class CreateTask(CamelCaseSchema):
    auto_id: int
    order_id: int
    name: str
    description: str | None = None
    price: str | None = None


class UpdateTask(CamelCaseSchema):
    id: int
    name: str
    description: str | None = None
    price: str | None = None


class TaskResponse(CamelCaseSchema):
    id: int
    auto_id: int
    order_id: int
    name: str
    description: str | None = None
    price: Decimal | None = None
