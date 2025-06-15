from pydantic import BaseModel
from enum import Enum


class DatabaseMode(Enum):
    DEVELOPMENT = "dev"
    PRODUCTION = "production"


class DatabaseParams(BaseModel):
    mode: DatabaseMode
    username: str
    password: str
    address: str
    port: int
    db_name: str
