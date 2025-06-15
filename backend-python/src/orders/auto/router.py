from typing import Annotated
from fastapi import Depends, APIRouter, Header
from sqlalchemy.ext.asyncio import AsyncSession

from .dependencies import AutoDI
from src.tools.database.database import Database
from . import schemas, service

router = APIRouter(prefix="/api")
database = Database()


@router.get("/auto/{auto_id}/", response_model=schemas.Auto)
async def get_auto_by_id(
    auto_id: int,
    authorization: Annotated[str, Header()],
    db: AsyncSession = Depends(database.get_connection_per_request),
    service: service.AutoService = Depends(AutoDI.get_auto_service),
):
    return await service.get_auto_by_id_with_auth(db, authorization, auto_id)
