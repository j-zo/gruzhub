from enum import Enum


class OrderStatus(str, Enum):
    CREATED = "CREATED"
    CALCULATING = "CALCULATING"
    REVIEWING = "REVIEWING"
    ACCEPTED = "ACCEPTED"
    COMPLETED = "COMPLETED"
    CANCEL = "CANCEL"
