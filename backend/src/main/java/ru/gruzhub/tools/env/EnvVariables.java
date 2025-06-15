package ru.gruzhub.tools.env;

import java.math.BigDecimal;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ru.gruzhub.tools.env.enums.AppMode;

@Component
@SuppressWarnings({"checkstyle:AbbreviationAsWordInName",
                   "checkstyle:LocalVariableName",
                   "checkstyle:MemberName"})
public class EnvVariables {
    public static final String TELEGRAM_API_SECRET_KEY = "this_is_super_secret_key_for_logging";
    public static final BigDecimal MASTER_START_BALANCE = new BigDecimal("10000");

    // email
    public final String EMAIL_HOST;
    public final int EMAIL_PORT;
    public final String EMAIL_LOGIN;
    public final String EMAIL_PASSWORD;
    // telegram
    public final String TELEGRAM_BOT_TOKEN;
    public final String TELEGRAM_BOT_USERNAME;
    // app
    public final AppMode APP_MODE;
    public final long ADMIN_TG_ID;
    public final String APPLICATION_SERVER;
    public final String JWT_SECRET_KEY;
    // Sentry
    public final String SENTRY_DSN;

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public EnvVariables(Environment env) {
        String TELEGRAM_BOT_TOKEN = env.getProperty("app.telegram_bot_token");
        if (TELEGRAM_BOT_TOKEN == null) {
            throw new NullPointerException("app.telegram_bot_token");
        }

        String TELEGRAM_BOT_USERNAME = env.getProperty("app.telegram_bot_username");
        if (TELEGRAM_BOT_USERNAME == null) {
            throw new NullPointerException("app.telegram_bot_username");
        }

        String APP_MODE = env.getProperty("app.mode");
        if (APP_MODE == null) {
            throw new NullPointerException("app.mode");
        }

        String ADMIN_TG_ID = env.getProperty("app.admin_tg_id");
        if (ADMIN_TG_ID == null) {
            throw new NullPointerException("app.admin_tg_id");
        }

        String EMAIL_HOST = env.getProperty("app.email_host");
        if (EMAIL_HOST == null) {
            throw new NullPointerException("app.email_host");
        }

        String EMAIL_PORT = env.getProperty("app.email_port");
        if (EMAIL_PORT == null) {
            throw new NullPointerException("app.email_port");
        }

        String EMAIL_LOGIN = env.getProperty("app.email_login");
        if (EMAIL_LOGIN == null) {
            throw new NullPointerException("app.email_login");
        }

        String EMAIL_PASSWORD = env.getProperty("app.email_password");
        if (EMAIL_PASSWORD == null) {
            throw new NullPointerException("app.email_password");
        }

        String APPLICATION_SERVER = env.getProperty("app.application_server");
        if (APPLICATION_SERVER == null) {
            throw new NullPointerException("app.application_server");
        }

        String JWT_SECRET_KEY = env.getProperty("app.jwt_secret_key");
        if (JWT_SECRET_KEY == null) {
            throw new NullPointerException("app.jwt_secret_key");
        }

        // sentry

        String SENTRY_DSN = env.getProperty("sentry.dsn");
        if (SENTRY_DSN == null) {
            throw new NullPointerException("sentry.dsn");
        }

        // telegram
        this.TELEGRAM_BOT_TOKEN = TELEGRAM_BOT_TOKEN;
        this.TELEGRAM_BOT_USERNAME = TELEGRAM_BOT_USERNAME;
        // app
        this.APP_MODE = AppMode.fromString(APP_MODE);
        this.ADMIN_TG_ID = Long.parseLong(ADMIN_TG_ID);
        this.APPLICATION_SERVER = APPLICATION_SERVER;
        this.JWT_SECRET_KEY = JWT_SECRET_KEY;
        // email
        this.EMAIL_HOST = EMAIL_HOST;
        this.EMAIL_PORT = Integer.parseInt(EMAIL_PORT);
        this.EMAIL_LOGIN = EMAIL_LOGIN;
        this.EMAIL_PASSWORD = EMAIL_PASSWORD;
        // sentry
        this.SENTRY_DSN = SENTRY_DSN;
    }
}
