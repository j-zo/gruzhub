package ru.gruzhub.telegram;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gruzhub.telegram.models.TelegramChat;

@Repository
public interface TelegramChatRepository extends JpaRepository<TelegramChat, String> {
    TelegramChat findByChatUuid(String chatUuid);

    Optional<TelegramChat> findByTelegramChatId(Long telegramChatId);

    boolean existsByChatUuid(String chatUuid);
}
