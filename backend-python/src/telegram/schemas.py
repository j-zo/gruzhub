from src.util.camel_case_schema import CamelCaseSchema


class ConnectTelegramRequest(CamelCaseSchema):
    id: int
    first_name: str | None = None
    last_name: str | None = None
    username: str | None = None
    photo_url: str | None = None
    auth_date: int
    hash: str
