from decimal import Decimal
from dotenv import load_dotenv
import os

load_dotenv()

JWT_SECRET_KEY_VALUE = os.getenv("JWT_SECRET_KEY")

if JWT_SECRET_KEY_VALUE is None:
    raise Exception("JWT_SECRET_KEY is None")

JWT_SECRET_KEY = JWT_SECRET_KEY_VALUE
MASTER_START_BALANCE = Decimal(10_000.00)

ADMIN_EMAIL = "rostislav.dugin@outlook.com"
ADMIN_NAME = 'Ростислав'
ADMIN_PHONE = '79854776527'
