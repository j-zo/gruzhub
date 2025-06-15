from src.util.camel_case_schema import CamelCaseSchema
from src.users.schemas import UserResponse
from .enums import AutoType


class Auto(CamelCaseSchema):
    id: int
    type: AutoType

    customer: UserResponse | None = None
    driver: UserResponse | None = None

    brand: str | None = None
    model: str | None = None
    vin: str | None = None
    number: str | None = None
