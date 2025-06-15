from src.util.camel_case_schema import CamelCaseSchema


class Region(CamelCaseSchema):
    id: int
    name: str
    country_code: str


class Country(CamelCaseSchema):
    code: str
    name: str
