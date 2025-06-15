import asyncio
from logging.config import fileConfig
from sqlalchemy.engine import Connection
from alembic import context
from dotenv import load_dotenv

from src.tools.database.database import Database, Base
from src.users import models as users_models
from src.addresses.regions import models as regions_models
from src.orders.auto import models as auto_models
from src.orders.orders import models as orders_models
from src.orders.tasks import models as tasks_models
from src.tools.logger import models as logger_models
from src.orders.notify import models as notifications_models
from src.orders.messages import models as order_messages_models
from src.telegram import models as telegram_models

print(users_models)
print(regions_models)
print(auto_models)
print(orders_models)
print(tasks_models)
print(logger_models)
print(notifications_models)
print(order_messages_models)
print(telegram_models)

load_dotenv()
# this is the Alembic Config object, which provides
# access to the values within the .ini file in use.
config = context.config

# Interpret the config file for Python logging.
# This line sets up loggers basically.
if config.config_file_name is not None:
    fileConfig(config.config_file_name)

# add your model's MetaData object here
# for 'autogenerate' support
# from myapp import mymodel
# target_metadata = mymodel.Base.metadata
target_metadata = Base.metadata

# other values from the config, defined by the needs of env.py,
# can be acquired:
# my_important_option = config.get_main_option("my_important_option")
# ... etc.


def run_migrations_offline() -> None:
    """Run migrations in 'offline' mode.

    This configures the context with just a URL
    and not an Engine, though an Engine is acceptable
    here as well.  By skipping the Engine creation
    we don't even need a DBAPI to be available.

    Calls to context.execute() here emit the given string to the
    script output.

    """
    url = config.get_main_option("sqlalchemy.url")
    context.configure(
        url=url,
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
    )

    with context.begin_transaction():
        context.run_migrations()


def do_run_migrations(connection: Connection) -> None:
    context.configure(connection=connection, target_metadata=target_metadata)

    with context.begin_transaction():
        context.run_migrations()


async def run_async_migrations() -> None:
    database = Database()
    connectable = database.get_async_engine()

    async with connectable.connect() as connection:
        await connection.run_sync(do_run_migrations)


def run_migrations_online() -> None:
    """Run migrations in 'online' mode."""

    asyncio.run(run_async_migrations())


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
