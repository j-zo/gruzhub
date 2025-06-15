from sqlalchemy.ext.asyncio import AsyncSession

from src.orders.auto.models import Auto
from src.orders.auto.service import AutoService
from src.orders.orders.repository import OrdersRepository


class UpdateAutoCommand:
    def __init__(
        self,
        auto_service: AutoService,
        orders_repository: OrdersRepository,
    ):
        self._auto_service = auto_service
        self._orders_repository = orders_repository

    async def update_auto(self, db: AsyncSession, auto_to_update: Auto) -> Auto:
        updated_auto = await self._auto_service.update_auto(db, auto_to_update)

        # if IDs do not match, this means auto was a dublicate
        # and has been merged by number or VIN with original car
        # so need to merge orders as well
        if auto_to_update.id != updated_auto.id:
            await self._move_order_from_dublicated_auto_to_original_auto(
                db,
                dublicated_auto=auto_to_update,
                original_auto=updated_auto,
            )

        return updated_auto

    async def _move_order_from_dublicated_auto_to_original_auto(
        self,
        db: AsyncSession,
        *,
        dublicated_auto: Auto,
        original_auto: Auto,
    ):
        dublicated_auto_orders = await self._orders_repository.get_orders(
            db,
            auto_id=dublicated_auto.id,
        )

        for order in dublicated_auto_orders:
            dublicated_auto_index: int | None = None

            for auto_index, auto in enumerate(order.autos):
                if auto.id == auto.id:
                    dublicated_auto_index = auto_index

            if not dublicated_auto_index:
                raise Exception("Dublicated auto has not been found in order")

            order.autos.pop(dublicated_auto_index)
            order.autos.append(original_auto)

        await db.commit()
