from enum import Enum


class UserRole(str, Enum):
    MASTER = "MASTER"
    CUSTOMER = "CUSTOMER"
    DRIVER = "DRIVER"
    ADMIN = "ADMIN"
