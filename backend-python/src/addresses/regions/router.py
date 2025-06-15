from fastapi import Depends, APIRouter
from sqlalchemy.ext.asyncio import AsyncSession

from src.tools.database.dependencies import DatabaseDI
from src.addresses.regions.dependencies import RegionsDI
from . import schemas, service

router = APIRouter(prefix="/api/addresses")
database = DatabaseDI.get_database()


@router.get("/regions/", response_model=list[schemas.Region])
async def get_regions(
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: service.RegionsService = Depends(RegionsDI.get_regions_service),
):
    return await service.get_regions(db)


@router.get("/countries/", response_model=list[schemas.Country])
async def get_countries(
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: service.RegionsService = Depends(RegionsDI.get_regions_service),
):
    return await service.get_countries(db)
