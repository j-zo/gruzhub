from fastapi import Depends, APIRouter
from sqlalchemy.ext.asyncio import AsyncSession
from src.tools.database.dependencies import DatabaseDI

from src.tools.logger.dependencies import LoggerDI
from src.tools.logger.service import LoggerService
from . import schemas

router = APIRouter(prefix="/api")

logger_service = LoggerDI.get_logger_service("API /logger")
database = DatabaseDI.get_database()


def get_logger_service() -> LoggerService:
    return logger_service


@router.post("/logger/log-items", response_model=list[schemas.LogItem])
async def get_log_items(
    get_log_items_request: schemas.GetLogItemsRequest,
    db: AsyncSession = Depends(database.get_connection_per_request),
    logger_service: LoggerService = Depends(get_logger_service),
):
    return await logger_service.get_log_items(
        db,
        get_log_items_request,
    )
