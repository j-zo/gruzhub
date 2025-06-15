import smtplib
import ssl
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText

from src.tools.mail.constants import EMAIL_HOST, EMAIL_PORT, EMAIL_LOGIN, EMAIL_PASSWORD


class EmailService:
    def __init__(self):
        self.context = ssl.create_default_context()

    def send_email(self, to: str, subject: str, message: str):
        email = MIMEMultipart("alternative")

        email["From"] = EMAIL_LOGIN
        email["To"] = to
        email["Subject"] = subject

        html = MIMEText(message, "html")
        email.attach(html)

        with smtplib.SMTP_SSL(EMAIL_HOST, EMAIL_PORT, context=self.context) as server:
            server.login(EMAIL_LOGIN, EMAIL_PASSWORD)
            server.sendmail(EMAIL_LOGIN, to, email.as_string())


def get_email_service() -> EmailService:
    return EmailService()
