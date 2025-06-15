from src.util.camel_case_schema import CamelCaseSchema
from src.orders.auto.enums import AutoType
from src.orders.orders.enums import OrderStatus
from src.addresses.addresses.schemas import Address
from src.users.schemas import UserResponse


class OrderAuto(CamelCaseSchema):
    auto_id: int | None = None

    brand: str | None = None
    model: str | None = None
    vin: str | None = None
    number: str | None = None
    type: AutoType


class CreateOrder(CamelCaseSchema):
    guarantee_uuid: str

    driver_name: str | None = None
    driver_phone: str | None = None
    driver_email: str | None = None

    autos: list[OrderAuto]

    region_id: int
    city: str | None = None
    street: str | None = None
    description: str
    notes: str | None = None

    is_need_evacuator: bool
    is_need_mobile_team: bool

    urgency: str


class CreateOrderResponse(CamelCaseSchema):
    order_id: int
    driver_id: int | None = None
    access_token: str | None = None


class AutoResponse(CamelCaseSchema):
    id: int

    driver_id: int | None = None
    customer_id: int | None = None

    brand: str | None = None
    model: str | None = None
    vin: str | None = None
    number: str | None = None

    is_merged: bool
    merge_to_id: int | None = None

    type: AutoType


class OrderResponse(CamelCaseSchema):
    id: int
    guarantee_uuid: str

    customer_id: int | None = None
    customer: UserResponse | None = None

    master_id: int | None = None
    master: UserResponse | None = None

    driver_id: int | None = None
    driver: UserResponse | None = None

    autos: list[AutoResponse]

    description: str | None = None
    notes: str | None = None
    created_at: int
    updated_at: int
    status: OrderStatus
    last_status_update_time: int

    address: Address

    is_need_evacuator: bool
    is_need_mobile_team: bool

    urgency: str | None = None

    declined_masters_ids: list[int]


class DeclineOrderRequest(CamelCaseSchema):
    comment: str


class GetOrdersRequest(CamelCaseSchema):
    statuses: list[OrderStatus] | None = None

    master_id: int | None = None
    customer_id: int | None = None
    driver_id: int | None = None
    auto_id: int | None = None
    regions_ids: list[int] | None = None
    user_id: int | None = None
    limit: int | None = None


class UpdateOrderAutoRequest(CamelCaseSchema):
    order_id: int
    auto_id: int

    brand: str | None = None
    model: str | None = None
    vin: str | None = None
    number: str | None = None


class OrderStatusChange(CamelCaseSchema):
    id: int
    updated_at: int
    order_id: int
    new_status: OrderStatus
    updated_by: UserResponse
    master: UserResponse | None = None
    comment: str | None = None
