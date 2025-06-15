package ru.gruzhub.users.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.address.models.Address;
import ru.gruzhub.telegram.models.TelegramChat;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;

@Getter
@Setter
@NoArgsConstructor
public class UserResponseDto {
    private Long id;
    private UserRole role;

    private String email;
    private String phone;

    private BigDecimal balance;

    private String name;
    private String inn;
    private Integer tripRadiusKm;

    private Address address;

    private Long registrationDate;
    private List<TelegramChat> connectedTelegramChats;

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.role = user.getRole();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.balance = user.getBalance();
        this.name = user.getName();
        this.inn = user.getInn();
        this.tripRadiusKm = user.getTripRadiusKm();
        this.address = user.getAddress();
        this.registrationDate = user.getRegistrationDate();
        this.connectedTelegramChats = user.getConnectedTelegramChats();
    }
}
