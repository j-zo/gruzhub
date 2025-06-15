package ru.gruzhub.telegram.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.gruzhub.telegram.TelegramChatRepository;
import ru.gruzhub.telegram.models.TelegramChat;

@Service
@RequiredArgsConstructor
public class TelegramChatService {
    private final TelegramChatRepository telegramChatRepository;

    public TelegramChat getTelegramChatByUuid(String chatUuid) {
        return this.telegramChatRepository.findByChatUuid(chatUuid);
    }

    public TelegramChat getTelegramChatById(Long telegramChatId) {
        return this.telegramChatRepository.findByTelegramChatId(telegramChatId).orElse(null);
    }

    public TelegramChat getTelegramChatOrError(String chatUuid) {
        return this.telegramChatRepository.findByChatUuid(chatUuid);
    }
}
