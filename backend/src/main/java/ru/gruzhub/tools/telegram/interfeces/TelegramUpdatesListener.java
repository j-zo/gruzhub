package ru.gruzhub.tools.telegram.interfeces;

import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.api.objects.payments.SuccessfulPayment;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.gruzhub.tools.telegram.dto.TelegramCallbackMessageDto;
import ru.gruzhub.tools.telegram.dto.TelegramGroupDto;
import ru.gruzhub.tools.telegram.dto.TelegramGroupMessageDto;
import ru.gruzhub.tools.telegram.dto.TelegramGroupMigrationDto;
import ru.gruzhub.tools.telegram.dto.TelegramPrivateMessageDto;

public interface TelegramUpdatesListener {
  void onGroupInfoChanged(TelegramGroupDto groupDto) throws TelegramApiException;

  void onBotGroupRightsChanged(TelegramGroupDto groupDto) throws TelegramApiException;

  void onBotAddedToTheGroup(TelegramGroupDto groupDto) throws TelegramApiException;

  void onBotRemovedFromTheGroup(TelegramGroupDto groupDto) throws TelegramApiException;

  void onCallbackMessageReceived(TelegramCallbackMessageDto messageDto) throws TelegramApiException;

  void onGroupMessageReceived(TelegramGroupMessageDto message) throws TelegramApiException;

  void onPrivateMessageReceived(TelegramPrivateMessageDto message) throws TelegramApiException;

  void onGroupMigrated(TelegramGroupMigrationDto migrationDto) throws TelegramApiException;

  void onPreCheckoutQuery(PreCheckoutQuery preCheckoutQuery) throws TelegramApiException;

  void onSuccessfulPayment(SuccessfulPayment successfulPayment) throws TelegramApiException;
}
