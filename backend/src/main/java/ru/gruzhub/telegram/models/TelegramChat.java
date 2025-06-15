package ru.gruzhub.telegram.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "telegram_chats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TelegramChat {
    @Id
    @Column(name = "chat_uuid",
            nullable = false,
            columnDefinition = "TEXT")
    private String chatUuid;

    @Column(name = "telegram_chat_id",
            nullable = false)
    private Long telegramChatId;

    @Column(name = "title",
            columnDefinition = "TEXT")
    private String title;
}
