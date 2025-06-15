import time
import math


class TimeHelper:
    @staticmethod
    def now_ms() -> int:
        return math.floor(time.time() * 1000)
