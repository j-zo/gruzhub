package ru.gruzhub.telegram.services;

import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import ru.gruzhub.telegram.TelegramChatRepository;
import ru.gruzhub.telegram.dto.TelegramCallbackMessageDto;
import ru.gruzhub.telegram.dto.TelegramGroupMessageDto;
import ru.gruzhub.telegram.dto.TelegramGroupMigrationDto;
import ru.gruzhub.telegram.dto.TelegramPrivateMessageDto;
import ru.gruzhub.telegram.interfeces.TelegramUpdatesListener;
import ru.gruzhub.telegram.models.TelegramChat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelegramListenerService implements TelegramUpdatesListener {
    private final TelegramChatRepository telegramChatRepository;
    private final TelegramSenderService telegramSenderService;

    @Value("${app.url}")
    private String siteMainUrl;

    @Override
    public void onBotLaunched(String botUsername, String botName) {
        System.out.println("Bot launched: " + botUsername + " (" + botName + ")");
    }

    @Override
    public void onCallbackMessageReceived(TelegramCallbackMessageDto messageDto) {
        // ignore
    }

    @Override
    public void onGroupMessageReceived(TelegramGroupMessageDto message) {
        TelegramChat telegramChat =
                this.createOrUpdateChatIfNeeded(message.getGroupDto().getChatId(),
                        message.getGroupDto().getTitle());
        this.sendMessage(telegramChat, message.getText());
    }

    @Override
    public void onPrivateMessageReceived(TelegramPrivateMessageDto message) {
        TelegramChat telegramChat =
                this.createOrUpdateChatIfNeeded(message.getUserDto().getUserTgId(),
                        message.getUserDto().getTgName());
        this.sendMessage(telegramChat, message.getText());
    }

    @Override
    public void onGroupMigrated(TelegramGroupMigrationDto migrationDto) {
        this.migrateChat(migrationDto.getOriginalTgId(), migrationDto.getMigratedToTgId());
    }

    public TelegramChat createOrUpdateChatIfNeeded(Long telegramChatId, String chatTitle) {
        Optional<TelegramChat> optionalChat =
                this.telegramChatRepository.findByTelegramChatId(telegramChatId);

        TelegramChat telegramChat;
        if (optionalChat.isEmpty()) {
            telegramChat = this.createChat(telegramChatId, chatTitle);
        } else {
            telegramChat = optionalChat.get();
            if (!chatTitle.equals(telegramChat.getTitle())) {
                telegramChat.setTitle(chatTitle);
                this.telegramChatRepository.save(telegramChat);
            }
        }

        return telegramChat;
    }

    public void migrateChat(Long migrationFromChatId, Long migrateToChatId) {
        Optional<TelegramChat> optionalChat =
                this.telegramChatRepository.findByTelegramChatId(migrationFromChatId);
        if (optionalChat.isPresent()) {
            TelegramChat chat = optionalChat.get();
            chat.setTelegramChatId(migrateToChatId);
            this.telegramChatRepository.save(chat);
        }
    }

    private void sendMessage(TelegramChat telegramChat, String message) {
        try {
            String text = """
                    ГрузХаб - это агрегатор заявок на ремонт магистральных автопоездов, автобусов и спецтехники
                    
                    Водитель или сотрудник транспортной компании может за 1 минуту создать заявку на ремонт. СТО перезвонит заказчику через 15 минут
                    
                    СТО получает уведомления о заказах на ремонт транзитного транспорта из других регионов
                    
                    На платформе у каждого заказа есть чат для согласования работ, фотофиксации и обмена документами
                    
                    https://gruzhub.ru/
                    """;
            if (message.startsWith("/start")) {

                InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                keyboardMarkup.setKeyboard(rows);

                List<InlineKeyboardButton> row1 = new ArrayList<>();
                InlineKeyboardButton button1 = new InlineKeyboardButton();
                button1.setText("Личный кабинет");
                button1.setWebApp(new WebAppInfo(siteMainUrl));
                row1.add(button1);
                rows.add(row1);

                List<InlineKeyboardButton> row2 = new ArrayList<>();
                InlineKeyboardButton button2 = new InlineKeyboardButton();
                button2.setText("Оставить заявку");
                button2.setWebApp(new WebAppInfo(siteMainUrl + "/anonymous-order"));
                row2.add(button2);
                rows.add(row2);

                keyboardMarkup.setKeyboard(rows);

                this.telegramSenderService.sendMessage(telegramChat.getTelegramChatId(),
                        text,
                        keyboardMarkup);
            }

            if (message.startsWith("/code")) {
                this.telegramSenderService.sendMessage(telegramChat.getTelegramChatId(),
                        "Код чата для подключения уведомлений о " +
                                "новых " +
                                "заказах в вашем регионе (для вставки на " +
                                "сайте" +
                                " https://gruzhub.ru/):",
                        null);
                this.telegramSenderService.sendMessage(telegramChat.getTelegramChatId(),
                        telegramChat.getChatUuid(),
                        null);
            }
        } catch (Exception e) {
            if (e.getMessage().contains("bot was blocked by the user")) {
                return;
            }

            Sentry.captureException(e);
        }
    }

    private TelegramChat createChat(Long telegramChatId, String chatTitle) {
        TelegramChat telegramChat = new TelegramChat();
        telegramChat.setTelegramChatId(telegramChatId);
        telegramChat.setTitle(chatTitle);
        telegramChat.setChatUuid(this.generateUniqueChatUuid());
        this.telegramChatRepository.save(telegramChat);
        return telegramChat;
    }

    private String generateUniqueChatUuid() {
        String newUuid;
        do {
            newUuid = UUID.randomUUID().toString();
        } while (this.telegramChatRepository.existsByChatUuid(newUuid));
        return newUuid;
    }
}
