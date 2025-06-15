from uuid import uuid4


class RequestUtils:
    @staticmethod
    def get_request_id() -> str:
        return str(uuid4())
