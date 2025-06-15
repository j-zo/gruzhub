from enum import Enum


class LogLevel(str, Enum):
    ERROR = "error"
    WARN = "warn"
    INFO = "info"
    DEBUG = "debug"
