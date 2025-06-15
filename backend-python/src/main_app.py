from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.routing import APIRoute

from src.addresses.regions.dependencies import RegionsDI
from src.tools.database.dependencies import DatabaseDI
from src.tools.logger.dependencies import LoggerDI
from src.tools.ping import router as ping_router
from src.users import router as users_router
from src.addresses.regions import router as regions_router
from src.orders.auto import router as auto_router
from src.orders.orders.routers import data_router as orders_data_router
from src.orders.orders.routers import workflow_router as orders_workflow_router
from src.orders.tasks import router as tasks_router
from src.tools.logger import router as logger_router
from src.orders.messages import router as order_messages_router
from src.tools.files import router as files_router


@asynccontextmanager
async def lifespan(app: FastAPI):  # noqa: ARG001
    logger_service_cleaner = LoggerDI.get_logger_service_cleaner()
    await logger_service_cleaner.start_cleaning()
    yield


def create_app() -> FastAPI:
    app = FastAPI(root_path="/api", lifespan=lifespan)

    app.include_router(ping_router.router)
    app.include_router(users_router.router)
    app.include_router(regions_router.router)
    app.include_router(auto_router.router)
    app.include_router(orders_data_router.router)
    app.include_router(orders_workflow_router.router)
    app.include_router(tasks_router.router)
    app.include_router(logger_router.router)
    app.include_router(order_messages_router.router)
    app.include_router(files_router.router)

    # initialize data
    database = DatabaseDI.get_database()
    regions_service = RegionsDI.get_regions_service()
    regions_service.initialize(database.get_sync_session())

    return app


app = create_app()

# Print all routes to the terminal
print("API endpoints list:")
for route in app.routes:
    if isinstance(route, APIRoute):
        print(f"{route.methods}".ljust(15) + f"{route.path}")
