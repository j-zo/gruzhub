package ru.gruzhub;

import io.sentry.Hint;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import java.util.Arrays;
import org.springframework.stereotype.Component;
import ru.gruzhub.tools.env.EnvVariables;
import ru.gruzhub.tools.env.enums.AppMode;

@Component
public class SentryStart {
    public SentryStart(EnvVariables envVariables) {
        Sentry.init(options -> {
            options.setDsn(envVariables.SENTRY_DSN);

            options.setBeforeSend((SentryEvent event, Hint hint) -> {
                Throwable throwable = event.getThrowable();

                if (throwable != null) {
                    Throwable rootCause = throwable;

                    while (rootCause.getCause() != null) {
                        rootCause = rootCause.getCause(); // Traverse the exception chain
                    }

                    String rootCauseMessage =
                        rootCause.getMessage() + Arrays.toString(rootCause.getStackTrace());

                    event.addBreadcrumb("Exception: " + rootCause.getMessage());

                    if (!rootCause.getMessage()
                                  .contains("This is a test exception during backend deploy")) {
                        System.out.println("Exception: " + rootCause.getMessage());
                        System.out.println("Class: " + rootCause.getClass().getName());
                        System.out.println("Stack trace: ");
                        rootCause.printStackTrace();
                    }

                    // Skip certain exceptions
                    if (rootCauseMessage.contains("ClosedChannelException")) {
                        return null;
                    }

                    if (rootCauseMessage.contains("duplicate key") ||
                        rootCauseMessage.contains("already exist")) {
                        return null;
                    }

                    if (rootCauseMessage.contains("Broken pipe")) {
                        return null;
                    }
                }

                if (envVariables.APP_MODE != AppMode.PRODUCTION) {
                    return null;
                }

                return event;
            });
        });
    }
}
