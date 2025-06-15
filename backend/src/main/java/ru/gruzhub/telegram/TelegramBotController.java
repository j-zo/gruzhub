package ru.gruzhub.telegram;

import io.sentry.Sentry;
import lombok.Setter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.gruzhub.telegram.dto.TelegramCallbackMessageDto;
import ru.gruzhub.telegram.dto.TelegramGroupDto;
import ru.gruzhub.telegram.dto.TelegramGroupMessageDto;
import ru.gruzhub.telegram.dto.TelegramGroupMigrationDto;
import ru.gruzhub.telegram.dto.TelegramPrivateMessageDto;
import ru.gruzhub.telegram.dto.TelegramUserDto;
import ru.gruzhub.telegram.interfeces.TelegramUpdatesListener;
import ru.gruzhub.tools.env.EnvVariables;
import ru.gruzhub.tools.env.enums.AppMode;

@Service
public class TelegramBotController extends TelegramLongPollingBot {
    private final EnvVariables envVariables;
    @Setter
    private TelegramUpdatesListener updatesListener;

    public TelegramBotController(EnvVariables envVariables) {
        super(envVariables.TELEGRAM_BOT_TOKEN);
        this.envVariables = envVariables;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void postLoad() {
        if (this.envVariables.APP_MODE == AppMode.TEST) {
            return;
        }

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);

            User bot = this.getBotInfo();
            this.updatesListener.onBotLaunched(bot.getUserName(), this.getUserTgNameFromUser(bot));
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            System.out.println(update);

            if (update.hasCallbackQuery()) {
                CallbackQuery callbackQuery = update.getCallbackQuery();

                User from = callbackQuery.getFrom();
                long chatId = from.getId();
                String tgName = this.getUserTgNameFromCallbackQuery(callbackQuery);
                String tgUsername = from.getUserName();
                String text = callbackQuery.getData();

                TelegramUserDto userDto = new TelegramUserDto(chatId, tgName, tgUsername);
                TelegramCallbackMessageDto messageDto = new TelegramCallbackMessageDto(text,
                                                                                       (long) callbackQuery.getMessage()
                                                                                                           .getMessageId(),
                                                                                       userDto);
                this.updatesListener.onCallbackMessageReceived(messageDto);
                return;
            }

            if (update.hasMessage()) {
                Message updateMessage = update.getMessage();
                Chat chat = updateMessage.getChat();

                // private message
                if (chat.getType().equals("private")) {
                    TelegramUserDto userDto = new TelegramUserDto(chat.getId(),
                                                                  chat.getUserName(),
                                                                  this.getUserTgNameFromUser(
                                                                      updateMessage.getFrom()));
                    TelegramPrivateMessageDto messageDto =
                        new TelegramPrivateMessageDto(updateMessage.getText(), userDto);
                    this.updatesListener.onPrivateMessageReceived(messageDto);
                } else {
                    // chat migration
                    if (updateMessage.getMigrateToChatId() != null) {
                        TelegramGroupMigrationDto migrationDto =
                            new TelegramGroupMigrationDto(chat.getId(),
                                                          updateMessage.getMigrateToChatId());
                        this.updatesListener.onGroupMigrated(migrationDto);
                    } else {
                        // group message
                        if (update.hasMessage() && update.getMessage().hasText()) {
                            TelegramGroupMessageDto messageDto =
                                this.getTelegramMessageFromGroupChatMessage(updateMessage);
                            this.updatesListener.onGroupMessageReceived(messageDto);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return "DefaultBotName";
    }

    public User getBotInfo() throws TelegramApiException {
        GetMe getMe = new GetMe();
        return this.execute(getMe);
    }

    public void sendMessage(SendMessage sendMessage) throws TelegramApiException {
        this.execute(sendMessage);
    }

    private TelegramGroupMessageDto getTelegramMessageFromGroupChatMessage(Message message) {
        if (message.getChat().getType().equals("private")) {
            throw new IllegalArgumentException("Cannot get group message from private chat");
        }

        Chat chat = message.getChat();
        User from = message.getFrom();

        TelegramGroupDto groupDto =
            new TelegramGroupDto(chat.getId(), chat.getTitle(), chat.getType());
        TelegramUserDto userDto =
            new TelegramUserDto(from.getId(), from.getUserName(), this.getUserTgNameFromUser(from));

        return new TelegramGroupMessageDto(message.getText(), userDto, groupDto);
    }

    private String getUserTgNameFromUser(User user) {
        String userFirstName = user.getFirstName();
        String userLastName = user.getLastName();

        if (userLastName == null) {
            userLastName = "";
        }

        return (userFirstName + " " + userLastName).trim();
    }

    private String getUserTgNameFromCallbackQuery(CallbackQuery callbackQuery) {
        String userFirstName = callbackQuery.getFrom().getFirstName();
        String userLastName = callbackQuery.getFrom().getLastName();

        if (userLastName == null) {
            userLastName = "";
        }

        return (userFirstName + " " + userLastName).trim();
    }
}
