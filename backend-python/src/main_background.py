import asyncio

from src.orders.notify.dependencies import OrdersNotificationDI
from src.orders.orders.dependencies import OrdersDI
from src.tools.database.dependencies import DatabaseDI


async def start_notifications():
    database = DatabaseDI.get_database()
    db = database.get_async_session()

    orders_background_service = OrdersDI.get_orders_background_service()
    await orders_background_service.initialize(db)

    orders_notifications_service = (
        OrdersNotificationDI.get_orders_notifications_service()
    )

    while True:
        print("Start background tasks...")
        await orders_notifications_service.send_notifications(db)
        await orders_background_service.cancel_old_new_orders(db)
        await orders_background_service.completed_old_orders(db)
        await asyncio.sleep(60)


asyncio.run(start_notifications())
