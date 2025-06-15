from http import HTTPStatus
from httpx import AsyncClient
import pytest

from src.tools.ping import router as ping_router

client = AsyncClient(app=ping_router.router, base_url="http://")


@pytest.mark.asyncio()
async def test_ping():
    response = await client.get("/api/ping")
    assert response.status_code == HTTPStatus.OK
    assert response.text == '"pong"'
