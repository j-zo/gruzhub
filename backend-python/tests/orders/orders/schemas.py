from src.util.camel_case_schema import CamelCaseSchema


class OrderWithUsers(CamelCaseSchema):
    order_id: int
    order_owner_token: str
    master_token: str
