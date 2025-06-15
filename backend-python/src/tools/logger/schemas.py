from .enums import LogLevel
from src.util.camel_case_schema import CamelCaseSchema


class GetLogItemsRequest(CamelCaseSchema):
    limit: int = 10
    offset: int = 0
    log_level: str
    request_id_query: str | None = None
    user_id: int | None = None
    text_query: str | None = None


class LogItem(CamelCaseSchema):
    id: int
    level: LogLevel
    request_id: str | None = None
    user_id: int | None = None
    text: str | None = None
    time: int
