from src.util.camel_case_schema import CamelCaseSchema


class Address(CamelCaseSchema):
    id: int | None = None
    region_id: int
    region_name: str | None = None
    city: str | None = None
    street: str | None = None
    latitude: float | None = None
    longtitude: float | None = None
