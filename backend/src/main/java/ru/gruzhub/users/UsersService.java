package ru.gruzhub.users;

import io.jsonwebtoken.Claims;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.gruzhub.address.AddressesService;
import ru.gruzhub.address.RegionsService;
import ru.gruzhub.address.models.Address;
import ru.gruzhub.address.models.Region;
import ru.gruzhub.telegram.auth.TelegramOauthService;
import ru.gruzhub.telegram.models.TelegramChat;
import ru.gruzhub.telegram.services.TelegramChatService;
import ru.gruzhub.telegram.services.TelegramSenderService;
import ru.gruzhub.tools.JwtTokenUtil;
import ru.gruzhub.tools.env.EnvVariables;
import ru.gruzhub.tools.env.enums.AppMode;
import ru.gruzhub.tools.mail.MailService;
import ru.gruzhub.users.dto.CreateUserRequestDto;
import ru.gruzhub.users.dto.GetUsersRequestDto;
import ru.gruzhub.users.dto.SignInUserRequestDto;
import ru.gruzhub.users.dto.SignInUserResponseDto;
import ru.gruzhub.users.dto.UpdateUserRequestDto;
import ru.gruzhub.users.dto.UserResponseDto;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;
import ru.gruzhub.users.models.UserInfoChange;

@Service
@RequiredArgsConstructor
public class UsersService {
    private final UserRepository userRepository;
    private final UserInfoChangeRepository userInfoChangeRepository;
    private final AddressesService addressesService;
    private final TelegramChatService telegramChatService;
    private final TelegramSenderService telegramSenderService;
    private final MailService mailService;
    private final RegionsService regionsService;
    private final TelegramOauthService telegramOauthService;

    private final JwtTokenUtil jwtTokenUtil;
    private final EnvVariables envVariables;

    public void signUp(CreateUserRequestDto signupRequest) {
        if (this.isUserExist(signupRequest.getEmail(),
                             signupRequest.getPhone(),
                             signupRequest.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                              "Почта или номер уже зарегистрирована");
        }

        if (signupRequest.getRole() == UserRole.ADMIN ||
            signupRequest.getRole() == UserRole.DRIVER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                              "Недопустимая роль для регистрации");
        }

        User user = new User();
        user.setRole(signupRequest.getRole());
        user.setEmail(signupRequest.getEmail());
        user.setPhone(signupRequest.getPhone());

        if (user.getRole() == UserRole.MASTER) {
            user.setBalance(EnvVariables.MASTER_START_BALANCE);
        } else {
            user.setBalance(BigDecimal.ZERO);
        }

        user.setName(signupRequest.getName());
        user.setInn(signupRequest.getInn());
        user.setTripRadiusKm(signupRequest.getTripRadiusKm());

        Region region = this.regionsService.getRegionById(signupRequest.getRegionId());
        Address address = new Address(null,
                                      region,
                                      signupRequest.getCity(),
                                      signupRequest.getStreet(),
                                      null,
                                      null);
        address = this.addressesService.createAddress(address);
        user.setAddress(address);

        user.setRegistrationDate(Instant.now().toEpochMilli());
        user.setPasswordHash(this.generatePasswordHash(signupRequest.getPassword()));
        user.setPasswordCreationTime(Instant.now().toEpochMilli());

        this.userRepository.save(user);
    }

    public User createUser(String name, String phone, String email, UserRole role) {
        if (this.isUserExist(email, phone, role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                              "Почта или номер уже зарегистрирована");
        }

        if (role == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                              "Администратор не может быть зарегистрирован");
        }

        User user = new User();
        user.setRole(role);
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setBalance(BigDecimal.ZERO);
        user.setRegistrationDate(Instant.now().toEpochMilli());

        return this.userRepository.save(user);
    }

    public SignInUserResponseDto signIn(SignInUserRequestDto signInRequest) {
        if (signInRequest.getEmail() == null && signInRequest.getPhone() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                              "Ни телефон, ни почта не указаны");
        }

        User user = null;
        if (signInRequest.getEmail() != null) {
            user = this.userRepository.findByEmailAndRole(signInRequest.getEmail(), UserRole.ADMIN)
                                      .orElse(this.userRepository.findByEmailAndRole(signInRequest.getEmail(),
                                                                                     signInRequest.getRole())
                                                                 .orElse(null));
        }

        if (signInRequest.getPhone() != null) {
            user = this.userRepository.findByPhoneAndRole(signInRequest.getPhone(), UserRole.ADMIN)
                                      .orElse(this.userRepository.findByPhoneAndRole(signInRequest.getPhone(),
                                                                                     signInRequest.getRole())
                                                                 .orElse(null));
        }

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден");
        }

        if (user.getPasswordHash() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                              "Для аккаунта не установлен пароль. Восстановите " +
                                              "его для входа в аккаунт");
        }

        if (!this.isPasswordValid(signInRequest.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пароль не подходит");
        }

        String accessToken = this.generateAccessToken(user);
        return new SignInUserResponseDto(user.getId(), accessToken);
    }

    public void updateRequest(UpdateUserRequestDto updateRequest) {
        this.update(this.getCurrentUser(), updateRequest);
    }

    public void update(User authorizedUser, UpdateUserRequestDto updateRequest) {
        if (authorizedUser == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        User user = this.userRepository.findById(updateRequest.getId())
                                       .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!authorizedUser.getId().equals(user.getId()) &&
            authorizedUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        UserInfoChange userInfoChange = this.createUserInfoChange(user, updateRequest);

        if (updateRequest.getName() != null) {
            user.setName(updateRequest.getName());
        }
        if (updateRequest.getInn() != null) {
            user.setInn(updateRequest.getInn());
        }

        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
            if (this.isUserExist(updateRequest.getEmail(), null, user.getRole())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                  "Такая почта уже зарегистрирована");
            }
            user.setEmail(updateRequest.getEmail());
        }

        if (updateRequest.getPhone() != null && !updateRequest.getPhone().equals(user.getPhone())) {
            if (this.isUserExist(null, updateRequest.getPhone(), user.getRole())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                  "Такой телефон уже зарегистрирован");
            }
            user.setPhone(updateRequest.getPhone());
        }

        if (updateRequest.getPassword() != null) {
            user.setPasswordHash(this.generatePasswordHash(updateRequest.getPassword()));
            user.setPasswordCreationTime(Instant.now().toEpochMilli());
        }

        if (updateRequest.getTripRadiusKm() != null) {
            user.setTripRadiusKm(updateRequest.getTripRadiusKm());
        }

        if (updateRequest.getRegionId() != null ||
            updateRequest.getCity() != null ||
            updateRequest.getStreet() != null) {
            if (user.getAddress() != null) {
                Address currentAddress =
                    this.addressesService.getAddressById(user.getAddress().getId());
                if (updateRequest.getCity() != null) {
                    currentAddress.setCity(updateRequest.getCity());
                }
                if (updateRequest.getStreet() != null) {
                    currentAddress.setStreet(updateRequest.getStreet());
                }
                if (updateRequest.getRegionId() != null) {
                    currentAddress.setRegion(this.regionsService.getRegionById(updateRequest.getRegionId()));
                }
                this.addressesService.updateAddress(currentAddress);
            } else if (updateRequest.getRegionId() != null) {
                Region region = this.regionsService.getRegionById(updateRequest.getRegionId());
                Address address = new Address(null,
                                              region,
                                              updateRequest.getCity(),
                                              updateRequest.getStreet(),
                                              null,
                                              null);
                address = this.addressesService.createAddress(address);
                user.setAddress(address);
            }
        }

        if (userInfoChange != null) {
            this.userInfoChangeRepository.save(userInfoChange);
        }

        this.userRepository.save(user);
    }

    public UserResponseDto getUserByIdWithAuth(Long userId) {
        User authorizedUser = this.getCurrentUser();
        User user = this.userRepository.findById(userId)
                                       .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!authorizedUser.getId().equals(user.getId()) &&
            authorizedUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return new UserResponseDto(user);
    }

    public User getUserById(Long userId) {
        return this.userRepository.findById(userId).orElseThrow();
    }

    public User getUserByEmail(String email, UserRole role) {
        return this.userRepository.findByEmailAndRole(email, role).orElse(null);
    }

    public User getUserByPhone(String phone, UserRole role) {
        return this.userRepository.findByPhoneAndRole(phone, role).orElse(null);
    }

    public User getUserFromToken(String token) {
        return this.getUserModelFromToken(token);

    }

    public User getUserModelFromToken(String token) {
        try {
            Claims claims = this.jwtTokenUtil.getClaimsFromToken(token);
            Long userId = claims.get("id", Long.class);
            Long passwordCreationTime = claims.get("password_creation_time", Long.class);

            User user = this.userRepository.findById(userId)
                                           .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

            if (!Objects.equals(user.getPasswordCreationTime(), passwordCreationTime)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }

            return user;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    public void increaseUserBalance(Long userId, BigDecimal amount) {
        User user = this.userRepository.findById(userId)
                                       .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        user.setBalance(user.getBalance().add(amount));
        this.userRepository.save(user);
    }

    public void decreaseUserBalance(Long userId, BigDecimal amount) {
        User user = this.userRepository.findById(userId)
                                       .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (user.getBalance().subtract(amount).compareTo(BigDecimal.ZERO) >= 0) {
            user.setBalance(user.getBalance().subtract(amount));
            this.userRepository.save(user);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                              "На балансе не хватает средств");
        }
    }

    public void sendResetCode(String email, UserRole role) {
        User user = this.userRepository.findByEmailAndRole(email, role)
                                       .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                                      "Пользователь не" +
                                                                                      " найден"));

        user.setUserResetCode(UUID.randomUUID().toString());
        this.userRepository.save(user);

        String resetLink = this.envVariables.APPLICATION_SERVER +
                           "/reset-password?email=" +
                           user.getEmail() +
                           "&code=" +
                           user.getUserResetCode() +
                           "&role=" +
                           role.name();
        String message =
            "<html><body><p>Перейдите по ссылке для восстановления пароля - <a href=\"" +
            resetLink +
            "\">" +
            resetLink +
            "</a></p></body></html>";

        this.mailService.sendEmail(email, "Восстановление пароля", message);
    }

    public void resetPassword(String email, String code, String password, UserRole role) {
        User user = this.userRepository.findByEmailAndRole(email, role)
                                       .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                                      "Пользователь не" +
                                                                                      " найден"));

        if (this.envVariables.APP_MODE != AppMode.TEST) {
            if (!code.equals(user.getUserResetCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Код не подходит");
            }
        } else {
            if (!code.equals("test-reset-code")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Код не подходит");
            }
        }

        user.setPasswordHash(this.generatePasswordHash(password));
        user.setPasswordCreationTime(Instant.now().toEpochMilli());
        user.setUserResetCode(null);
        this.userRepository.save(user);
    }

    public void validateAuthRole(String token, List<UserRole> roles) {
        User user = this.getUserModelFromToken(token);

        if (roles.contains(user.getRole())) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "У текущей роли нет доступа");
    }

    public SignInUserResponseDto getUserAccess(String accessToken, Long userId) {
        User authorizedUser = this.getUserModelFromToken(accessToken);

        if (authorizedUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return this.getUserAccessNoAuth(userId);
    }

    public SignInUserResponseDto getUserAccessNoAuth(Long userId) {
        User user = this.getUserById(userId);
        String accessToken = this.generateAccessToken(user);
        return new SignInUserResponseDto(user.getId(), accessToken);
    }

    public String generateAccessToken(User user) {
        return this.jwtTokenUtil.generateToken(user);
    }

    public List<User> getMastersWithTelegramInRegion(Long regionId) {
        return this.userRepository.findMastersInRegion(UserRole.MASTER, regionId);
    }

    public List<User> getAdmins() {
        return this.userRepository.findByRole(UserRole.ADMIN);
    }

    public void connectTelegramViaWebApp(String authorization, Long tgId) {
        User authorizedUser = this.getUserModelFromToken(authorization);
        TelegramChat chat = this.telegramChatService.getTelegramChatById(tgId);
        authorizedUser.addTelegramChat(chat);
        this.userRepository.save(authorizedUser);

        this.telegramSenderService.sendMessage(chat.getTelegramChatId(),
                                               "Чат подключён к GruzHub",
                                               null);
    }

    public void connectTelegramChat(User user, String chatUuid) {
        TelegramChat chat = this.telegramChatService.getTelegramChatByUuid(chatUuid);
        user.addTelegramChat(chat);
        this.userRepository.save(user);

        this.telegramSenderService.sendMessage(chat.getTelegramChatId(),
                                               "Чат подключён к GruzHub",
                                               null);
    }

    public void disconnectTelegramChat(User user, String chatUuid) {
        TelegramChat chat = this.telegramChatService.getTelegramChatByUuid(chatUuid);
        user.removeTelegramChat(chat);
        this.userRepository.save(user);

        this.telegramSenderService.sendMessage(chat.getTelegramChatId(),
                                               "Чат отключён от GruzHub",
                                               null);
    }

    public User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public List<UserResponseDto> getUsers(String authorization,
                                          GetUsersRequestDto getUsersRequest) {
        User authorizedUser = this.getUserModelFromToken(authorization);
        if (authorizedUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<User> users;

        if (getUsersRequest.getRoles() != null &&
            !getUsersRequest.getRoles().isEmpty() &&
            getUsersRequest.getRegionsIds() != null &&
            !getUsersRequest.getRegionsIds().isEmpty()) {
            users = this.userRepository.findUsersByRolesAndRegions(getUsersRequest.getRoles(),
                                                                   getUsersRequest.getRegionsIds());
        } else if (getUsersRequest.getRoles() != null && !getUsersRequest.getRoles().isEmpty()) {
            users = this.userRepository.findUsersByRoles(getUsersRequest.getRoles());
        } else if (getUsersRequest.getRegionsIds() != null &&
                   !getUsersRequest.getRegionsIds().isEmpty()) {
            users = this.userRepository.findUsersByRegions(getUsersRequest.getRegionsIds());
        } else {
            users = this.userRepository.findAllUsers();
        }

        return users.stream().map(UserResponseDto::new).collect(Collectors.toList());
    }

    public List<UserInfoChange> getUserInfoChanges(Long userId) {
        return this.userInfoChangeRepository.findByUserIdOrderByIdDesc(userId);
    }

    public List<UserResponseDto> getUsersByIds(List<Long> userIds) {
        List<User> users = this.userRepository.findByIdIn(userIds);
        return users.stream().map(UserResponseDto::new).collect(Collectors.toList());
    }

    public void createAdminUserIfNotExist(String adminEmail, String adminName, String adminPhone) {
        if (this.userRepository.findByEmailAndRole(adminEmail, UserRole.ADMIN).isEmpty()) {
            User user = new User();
            user.setRole(UserRole.ADMIN);
            user.setName(adminName);
            user.setEmail(adminEmail);
            user.setPhone(adminPhone);
            user.setBalance(BigDecimal.ZERO);
            user.setRegistrationDate(Instant.now().toEpochMilli());
            this.userRepository.save(user);
        }
    }

    private boolean isUserExist(String email, String phone, UserRole role) {
        if (email == null && phone == null) {
            throw new IllegalArgumentException("Ни телефон, ни почта не указаны");
        }

        if (email != null && this.userRepository.findByEmailAndRole(email, role).isPresent()) {
            return true;
        }

        return phone != null && this.userRepository.findByPhoneAndRole(phone, role).isPresent();
    }

    private boolean isPasswordValid(String password, String passwordHash) {
        return BCrypt.checkpw(password, passwordHash);
    }

    private String generatePasswordHash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }

    private UserInfoChange createUserInfoChange(User user, UpdateUserRequestDto updateRequest) {
        boolean hasChange = !Objects.equals(updateRequest.getName(), user.getName()) ||
                            !Objects.equals(updateRequest.getPhone(), user.getPhone()) ||
                            !Objects.equals(updateRequest.getEmail(), user.getEmail()) ||
                            !Objects.equals(updateRequest.getInn(), user.getInn());

        if (hasChange) {
            UserInfoChange userInfoChange = new UserInfoChange();
            userInfoChange.setUser(user);
            userInfoChange.setDate(Instant.now().toEpochMilli());

            userInfoChange.setPreviousName(user.getName());
            userInfoChange.setNewName(updateRequest.getName() != null ?
                                      updateRequest.getName() :
                                      user.getName());

            userInfoChange.setPreviousPhone(user.getPhone());
            userInfoChange.setNewPhone(updateRequest.getPhone() != null ?
                                       updateRequest.getPhone() :
                                       user.getPhone());

            userInfoChange.setPreviousEmail(user.getEmail());
            userInfoChange.setNewEmail(updateRequest.getEmail() != null ?
                                       updateRequest.getEmail() :
                                       user.getEmail());

            userInfoChange.setPreviousInn(user.getInn());
            userInfoChange.setNewInn(updateRequest.getInn() != null ?
                                     updateRequest.getInn() :
                                     user.getInn());

            return userInfoChange;
        }

        return null;
    }
}
