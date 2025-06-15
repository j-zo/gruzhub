from src.users.enums import UserRole
from src.addresses.addresses.schemas import Address
from src.util.camel_case_schema import CamelCaseSchema


class CreateUserRequest(CamelCaseSchema):
    name: str
    inn: str | None = None
    role: UserRole

    email: str
    phone: str
    password: str

    trip_radius_km: int | None = None

    region_id: int
    city: str
    street: str


class UpdateUserRequest(CamelCaseSchema):
    id: int

    name: str | None = None
    inn: str | None = None

    email: str | None = None
    phone: str | None = None
    password: str | None = None

    trip_radius_km: int | None = None

    region_id: int | None = None
    city: str | None = None
    street: str | None = None

    user_chats_codes: list[str] | None = None


class UserResponse(CamelCaseSchema):
    id: int
    role: UserRole

    email: str | None = None
    phone: str | None = None

    balance: float | None = None

    name: str
    inn: str | None = None
    trip_radius_km: int | None = None

    address: Address | None = None

    telegram_id: int | None = None

    registration_date: int
    user_chats_codes: list[str]


class SignInUserRequest(CamelCaseSchema):
    email: str | None = None
    phone: str | None = None
    role: UserRole
    password: str


class SignInUserResponse(CamelCaseSchema):
    id: int
    access_token: str


class UserInfoChange(CamelCaseSchema):
    id: int
    user_id: int

    previous_name: str
    new_name: str

    previous_phone: str | None = None
    new_phone: str | None = None

    previous_email: str | None = None
    new_email: str | None = None

    previous_inn: str | None = None
    new_inn: str | None = None

    date: int


class GetUsersRequest(CamelCaseSchema):
    roles: list[UserRole] | None = None
    regions_ids: list[int] | None = None
