package ru.gruzhub.users.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.address.models.Address;
import ru.gruzhub.telegram.models.TelegramChat;
import ru.gruzhub.users.enums.UserRole;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role",
            nullable = false,
            columnDefinition = "TEXT")
    private UserRole role;

    @Column(name = "email",
            columnDefinition = "TEXT")
    private String email;

    @Column(name = "phone",
            columnDefinition = "TEXT")
    private String phone;

    @Column(name = "balance",
            precision = 12,
            scale = 2,
            nullable = false)
    private BigDecimal balance;

    @Column(name = "name",
            nullable = false,
            columnDefinition = "TEXT")
    private String name;

    @Column(name = "inn",
            columnDefinition = "TEXT")
    private String inn;

    @Column(name = "trip_radius_km")
    private Integer tripRadiusKm;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;

    @Column(name = "registration_date",
            nullable = false)
    private Long registrationDate;

    @JsonIgnore
    @Column(name = "password_hash",
            columnDefinition = "TEXT")
    private String passwordHash;

    @JsonIgnore
    @Column(name = "password_creation_time")
    private Long passwordCreationTime;

    @JsonIgnore
    @Column(name = "user_reset_code",
            columnDefinition = "TEXT")
    private String userResetCode;

    @Column(name = "telegram_access_error",
            columnDefinition = "TEXT")
    private String telegramAccessError;

    @Column(name = "telegram_access_error_shown")
    private Boolean telegramAccessErrorShown;

    @ManyToMany(cascade = {CascadeType.DETACH,
                           CascadeType.MERGE,
                           CascadeType.PERSIST,
                           CascadeType.REFRESH},
                fetch = FetchType.EAGER)
    @JoinTable(name = "chat_to_user_assosiation",
               joinColumns = @JoinColumn(name = "order_id"),
               inverseJoinColumns = @JoinColumn(name = "chat_uuid"))
    private List<TelegramChat> connectedTelegramChats;

    public void addTelegramChat(TelegramChat chat) {
        List<String> connectedChatsUuids =
            this.connectedTelegramChats.stream().map(TelegramChat::getChatUuid).toList();
        if (!connectedChatsUuids.contains(chat.getChatUuid())) {
            this.connectedTelegramChats.add(chat);
        }
    }

    public void removeTelegramChat(TelegramChat chat) {
        int indexOfRemovingChat = -1;

        for (int i = 0; i < this.connectedTelegramChats.size(); i++) {
            if (this.connectedTelegramChats.get(i).getChatUuid().equals(chat.getChatUuid())) {
                indexOfRemovingChat = i;
                break;
            }
        }

        if (indexOfRemovingChat >= 0) {
            this.connectedTelegramChats.remove(indexOfRemovingChat);
        }
    }
}
