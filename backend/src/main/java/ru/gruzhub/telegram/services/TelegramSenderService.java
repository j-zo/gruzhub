package ru.gruzhub.telegram.services;

import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.gruzhub.telegram.TelegramBotController;
import ru.gruzhub.tools.env.EnvVariables;
import ru.gruzhub.tools.env.enums.AppMode;

@Service
@RequiredArgsConstructor
public class TelegramSenderService {
    private final TelegramBotController telegramBotController;
    private final EnvVariables envVariables;

    public void sendMessage(Long telegramChatId, String message, ReplyKeyboard replyMarkup) {
        if (this.envVariables.APP_MODE == AppMode.TEST) {
            return;
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(telegramChatId.toString());
        sendMessage.disableWebPagePreview();
        sendMessage.setText(message);
        if (replyMarkup != null) {
            sendMessage.setReplyMarkup(replyMarkup);
        }

        try {
            this.telegramBotController.sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            if (e.getMessage().toLowerCase().contains("bot was blocked by the user") ||
                e.getMessage().toLowerCase().contains("forbidden")) {
                return;
            }

            Sentry.captureException(e);
        }

    }
}
