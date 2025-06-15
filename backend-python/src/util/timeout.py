import asyncio


class Timeout:
    @staticmethod
    async def call_sync_in_time(func, time_ms: int) -> None:
        await asyncio.sleep(time_ms / 1000)
        func()
