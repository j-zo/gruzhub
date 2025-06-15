package ru.gruzhub.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.gruzhub.telegram.dto.TelegramGroupDto;
import ru.gruzhub.telegram.dto.TelegramGroupMessageDto;
import ru.gruzhub.telegram.dto.TelegramGroupMigrationDto;
import ru.gruzhub.telegram.dto.TelegramPrivateMessageDto;
import ru.gruzhub.telegram.dto.TelegramUserDto;
import ru.gruzhub.telegram.models.TelegramChat;
import ru.gruzhub.telegram.services.TelegramChatService;
import ru.gruzhub.telegram.services.TelegramListenerService;

@SpringBootTest
@ActiveProfiles("test")
class TelegramServicesTest {
    @Autowired
    private TelegramChatRepository telegramChatRepository;

    @Autowired
    private TelegramChatService telegramChatService;

    @Autowired
    private TelegramListenerService telegramListenerService;

    @Test
    void testGetTelegramChatByUuid() {
        String id = UUID.randomUUID().toString();
        TelegramChat chat = new TelegramChat(id, 1L, "title");
        chat.setChatUuid(id);
        telegramChatRepository.save(chat);

        TelegramChat result = telegramChatService.getTelegramChatByUuid(id);
        assertNotNull(result);
        assertEquals(id, result.getChatUuid());
    }

    @Test
    void testGetTelegramChatById() {
        Long chatId = new Random().nextLong();
        String id = UUID.randomUUID().toString();

        TelegramChat chat = new TelegramChat(id, chatId, "title");
        telegramChatRepository.save(chat);

        TelegramChat result = telegramChatService.getTelegramChatById(chatId);
        assertNotNull(result);
        assertEquals(chatId, result.getTelegramChatId());
        assertEquals(id, result.getChatUuid());
    }

    @Test
    void testOnGroupMessageReceived() {
        Long chatId = new Random().nextLong();
        String chatTitle = "Test Group";

        TelegramGroupDto groupDto = new TelegramGroupDto(chatId, chatTitle, "group");
        TelegramUserDto userDto = new TelegramUserDto(1L, "username", "name");
        TelegramGroupMessageDto messageDto =
            new TelegramGroupMessageDto("Test message", userDto, groupDto);

        telegramListenerService.onGroupMessageReceived(messageDto);

        TelegramChat chat = telegramChatRepository.findByTelegramChatId(chatId).orElseThrow();
        assertEquals(chatTitle, chat.getTitle());
    }

    @Test
    void testOnPrivateMessageReceived() {
        Long userId = new Random().nextLong();
        String userName = "Test User";
        TelegramUserDto userDto = new TelegramUserDto(userId, "test_username", userName);
        TelegramPrivateMessageDto messageDto =
            new TelegramPrivateMessageDto("Test message", userDto);

        telegramListenerService.onPrivateMessageReceived(messageDto);

        TelegramChat chat = telegramChatRepository.findByTelegramChatId(userId).orElseThrow();
        assertEquals(userName, chat.getTitle());
    }

    @Test
    void testOnGroupMigrated() {
        Long originalChatId = new Random().nextLong();
        Long migratedChatId = new Random().nextLong();
        TelegramChat chat = new TelegramChat(UUID.randomUUID().toString(), originalChatId, "title");
        chat.setTelegramChatId(originalChatId);
        telegramChatRepository.save(chat);

        telegramListenerService.onGroupMigrated(new TelegramGroupMigrationDto(originalChatId,
                                                                              migratedChatId));

        TelegramChat migratedChat =
            telegramChatRepository.findByTelegramChatId(migratedChatId).orElse(null);
        assertNotNull(migratedChat);
        assertEquals(migratedChatId, migratedChat.getTelegramChatId());
    }
}