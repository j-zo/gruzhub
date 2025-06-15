import pytest
from http import HTTPStatus
from httpx import AsyncClient

from src.addresses.regions import schemas


class TestRegionsRouter:
    @staticmethod
    @pytest.mark.asyncio()
    async def test_get_regions(http_client: AsyncClient):
        response = await http_client.get("/api/addresses/regions/")
        assert response.status_code == HTTPStatus.OK
        response_json = response.json()
        assert len(response_json) > 0

        region = schemas.Region(**response_json[0])
        assert region.id
        assert region.name
