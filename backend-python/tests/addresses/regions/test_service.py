import random
import pytest
from sqlalchemy.ext.asyncio import AsyncSession

from src.addresses.regions import dependencies


class TestRegionsService:
    @staticmethod
    @pytest.mark.asyncio()
    async def test_get_region_by_id(db_connection: AsyncSession):
        regions_service = dependencies.RegionsDI.get_regions_service()

        region = await regions_service.get_region_by_id(
            db_connection,
            random.randint(20, 50),
        )
        assert region.id
        assert region.name
