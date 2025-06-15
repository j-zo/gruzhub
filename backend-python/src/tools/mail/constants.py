from dotenv import load_dotenv
import os

load_dotenv()

EMAIL_HOST_VALUE = os.getenv("EMAIL_HOST")
EMAIL_PORT_VALUE = os.getenv("EMAIL_PORT")
EMAIL_LOGIN_VALUE = os.getenv("EMAIL_LOGIN")
EMAIL_PASSWORD_VALUE = os.getenv("EMAIL_PASSWORD")

if not EMAIL_HOST_VALUE:
    raise Exception("EMAIL_HOST is None")
if not EMAIL_PORT_VALUE:
    raise Exception("EMAIL_PORT is None")
if not EMAIL_LOGIN_VALUE:
    raise Exception("EMAIL_LOGIN is None")
if not EMAIL_PASSWORD_VALUE:
    raise Exception("EMAIL_PASSWORD is None")

EMAIL_HOST = EMAIL_HOST_VALUE
EMAIL_PORT = int(EMAIL_PORT_VALUE)
EMAIL_LOGIN = EMAIL_LOGIN_VALUE
EMAIL_PASSWORD = EMAIL_PASSWORD_VALUE
