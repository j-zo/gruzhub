from dotenv import load_dotenv
import os

from src.tools.database.schemas import DatabaseMode

load_dotenv()

MODE_VALUE = os.getenv("MODE")
DATABASE_USERNAME_VALUE = os.getenv("DATABASE_USERNAME")
DATABASE_PASSWORD_VALUE = os.getenv("DATABASE_PASSWORD")
DATABASE_NAME_VALUE = os.getenv("DATABASE_NAME")
DATABASE_ADDRESS_VALUE = os.getenv("DATABASE_ADDRESS")
DATABASE_PORT_VALUE = os.getenv("DATABASE_PORT")

if MODE_VALUE is None:
    raise Exception("MODE is None")
if DATABASE_USERNAME_VALUE is None:
    raise Exception("DATABASE_USERNAME is None")
if DATABASE_PASSWORD_VALUE is None:
    raise Exception("DATABASE_PASSWORD is None")
if DATABASE_NAME_VALUE is None:
    raise Exception("DATABASE_NAME is None")
if DATABASE_ADDRESS_VALUE is None:
    raise Exception("DATABASE_ADDRESS is None")
if DATABASE_PORT_VALUE is None:
    raise Exception("DATABASE_PORT is None")

MODE = DatabaseMode.DEVELOPMENT if MODE_VALUE == "dev" else DatabaseMode.PRODUCTION
DATABASE_USERNAME = DATABASE_USERNAME_VALUE
DATABASE_PASSWORD = DATABASE_PASSWORD_VALUE
DATABASE_NAME = DATABASE_NAME_VALUE
DATABASE_ADDRESS = DATABASE_ADDRESS_VALUE
DATABASE_PORT = int(DATABASE_PORT_VALUE)
