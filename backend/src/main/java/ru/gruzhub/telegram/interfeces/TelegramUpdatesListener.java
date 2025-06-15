package ru.gruzhub.telegram.interfeces;

import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.gruzhub.telegram.TelegramBotController;
import ru.gruzhub.telegram.dto.TelegramCallbackMessageDto;
import ru.gruzhub.telegram.dto.TelegramGroupMessageDto;
import ru.gruzhub.telegram.dto.TelegramGroupMigrationDto;
import ru.gruzhub.telegram.dto.TelegramPrivateMessageDto;

public interface TelegramUpdatesListener {
    void onBotLaunched(String botUsername, String botName) throws TelegramApiException;

    void onCallbackMessageReceived(TelegramCallbackMessageDto messageDto)
        throws TelegramApiException;

    void onGroupMessageReceived(TelegramGroupMessageDto message) throws TelegramApiException;

    void onPrivateMessageReceived(TelegramPrivateMessageDto message) throws TelegramApiException;

    void onGroupMigrated(TelegramGroupMigrationDto migrationDto);

    @Autowired
    default void setMyself(TelegramBotController telegramBotController) {
        telegramBotController.setUpdatesListener(this);
    }
}
