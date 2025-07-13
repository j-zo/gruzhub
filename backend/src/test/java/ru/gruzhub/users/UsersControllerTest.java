package ru.gruzhub.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import ru.gruzhub.address.models.Region;
import ru.gruzhub.address.repositories.RegionRepository;
import ru.gruzhub.users.dto.CreateUserRequestDto;
import ru.gruzhub.users.dto.GetUsersRequestDto;
import ru.gruzhub.users.dto.SignInUserRequestDto;
import ru.gruzhub.users.dto.SignInUserResponseDto;
import ru.gruzhub.users.dto.UpdateUserRequestDto;
import ru.gruzhub.users.dto.UserDto;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.testing.UserTestingHelper;
import ru.gruzhub.users.testing.dto.TestAuthDataDto;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UsersControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UsersService usersService;

    @Autowired
    private RegionRepository regionRepository;

    @Test
    void testSignUp() {
        ResponseEntity<Void> response =
            this.signUp(UUID.randomUUID() + new Faker().internet().emailAddress(), "password");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testSignIn() {
        String email = UUID.randomUUID() + new Faker().internet().emailAddress();
        String password = "password";

        this.signUp(email, password);
        SignInUserResponseDto responseBody = this.signIn(email, password);

        assertNotNull(responseBody.getId());
        assertNotNull(responseBody.getAccessToken());
    }

    @Test
    void testGetUser() {
        String email = UUID.randomUUID() + new Faker().internet().emailAddress();
        String password = "password";

        this.signUp(email, password);
        SignInUserResponseDto responseBody = this.signIn(email, password);

        Long userId = responseBody.getId();
        String accessToken = responseBody.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<UserDto> getUserResponse =
            this.restTemplate.exchange("/users/" + userId,
                                       HttpMethod.GET,
                                       entity,
                                       UserDto.class);
        assertEquals(HttpStatus.OK, getUserResponse.getStatusCode());

        UserDto userResponse = getUserResponse.getBody();
        assertNotNull(userResponse);
        assertEquals(email, userResponse.getEmail());
    }

    @Test
    void testUpdateUser() {
        String email = UUID.randomUUID() + new Faker().internet().emailAddress();
        String password = "password";

        this.signUp(email, password);
        SignInUserResponseDto responseBody = this.signIn(email, password);

        Long userId = responseBody.getId();
        String accessToken = responseBody.getAccessToken();

        UpdateUserRequestDto updateRequest = new UpdateUserRequestDto();
        updateRequest.setId(userId);
        updateRequest.setName("Updated Test User");
        updateRequest.setCity("Updated City");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateUserRequestDto> entity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<Void> updateResponse =
            this.restTemplate.postForEntity("/users/update", entity, Void.class);
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());

        ResponseEntity<UserDto> getUserResponse =
            this.restTemplate.exchange("/users/" + userId,
                                       HttpMethod.GET,
                                       new HttpEntity<>(headers),
                                       UserDto.class);
        assertEquals(HttpStatus.OK, getUserResponse.getStatusCode());

        UserDto userResponse = getUserResponse.getBody();
        assertNotNull(userResponse);
        assertEquals("Updated Test User", userResponse.getName());
        assertEquals("Updated City", userResponse.getAddress().getCity());
    }

    @Test
    void testSendResetCode() {
        String email = UUID.randomUUID() + new Faker().internet().emailAddress();
        this.signUp(email, "password");

        String url = "/users/reset-code?email=" + email + "&role=CUSTOMER";
        ResponseEntity<Void> resetCodeResponse = this.restTemplate.getForEntity(url, Void.class);
        assertEquals(HttpStatus.OK, resetCodeResponse.getStatusCode());
    }

    @Test
    void testResetPassword() {
        String email = UUID.randomUUID() + new Faker().internet().emailAddress();
        String oldPassword = "password";
        String newPassword = "newpassword";

        this.signUp(email, oldPassword);
        String resetCodeUrl = "/users/reset-code?email=" + email + "&role=CUSTOMER";
        this.restTemplate.getForEntity(resetCodeUrl, Void.class);

        String resetCode =
            "test-reset-code";  // Assume this code is generated and available in tests
        String resetPasswordUrl = "/users/reset-password?email=" +
                                  email +
                                  "&role=CUSTOMER&code=" +
                                  resetCode +
                                  "&password=" +
                                  newPassword;

        ResponseEntity<Void> resetPasswordResponse =
            this.restTemplate.getForEntity(resetPasswordUrl, Void.class);
        assertEquals(HttpStatus.OK, resetPasswordResponse.getStatusCode());

        SignInUserResponseDto responseBody = this.signIn(email, newPassword);
        assertNotNull(responseBody.getId());
        assertNotNull(responseBody.getAccessToken());
    }

    @Test
    void testGetAccessForAdmin() {
        UserTestingHelper userTestingHelper =
            new UserTestingHelper(this.usersService, this.userRepository);
        TestAuthDataDto adminAuthDto = userTestingHelper.signUp(UserRole.ADMIN, null);

        String email = UUID.randomUUID() + new Faker().internet().emailAddress();
        this.signUp(email, "password");

        Long userId =
            this.userRepository.findByEmailAndRole(email, UserRole.CUSTOMER).orElseThrow().getId();
        String url = "/users/get-access/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", adminAuthDto.getAccessToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<SignInUserResponseDto> getAccessResponse =
            this.restTemplate.exchange(url, HttpMethod.GET, entity, SignInUserResponseDto.class);
        assertEquals(HttpStatus.OK, getAccessResponse.getStatusCode());

        SignInUserResponseDto accessResponse = getAccessResponse.getBody();
        assertNotNull(accessResponse);
        assertNotNull(accessResponse.getAccessToken());
        assertEquals(userId, accessResponse.getId());
    }

    @Test
    void testGetUsers() {
        Region region1 = this.regionRepository.findById(10L).orElseThrow();
        Region region2 = this.regionRepository.findById(11L).orElseThrow();

        UserTestingHelper userTestingHelper =
            new UserTestingHelper(this.usersService, this.userRepository);
        TestAuthDataDto adminAuthDto = userTestingHelper.signUp(UserRole.ADMIN, null);

        this.createTestUser(UUID.randomUUID() + new Faker().internet().emailAddress(),
                            "User One",
                            UUID.randomUUID().toString(),
                            UserRole.CUSTOMER,
                            region1.getId(),
                            "City1",
                            "Street1");
        this.createTestUser(UUID.randomUUID() + new Faker().internet().emailAddress(),
                            "User Two",
                            UUID.randomUUID().toString(),
                            UserRole.MASTER,
                            region1.getId(),
                            "City2",
                            "Street2");
        this.createTestUser(UUID.randomUUID() + new Faker().internet().emailAddress(),
                            "User Three",
                            UUID.randomUUID().toString(),
                            UserRole.CUSTOMER,
                            region2.getId(),
                            "City3",
                            "Street3");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", adminAuthDto.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        GetUsersRequestDto getAllUsersRequest = new GetUsersRequestDto();
        HttpEntity<GetUsersRequestDto> requestEntity =
            new HttpEntity<>(getAllUsersRequest, headers);

        ResponseEntity<UserDto[]> response =
            this.restTemplate.postForEntity("/users/users", requestEntity, UserDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserDto[] users = response.getBody();
        assertNotNull(users);
        assertTrue(users.length >= 3);
    }

    private ResponseEntity<Void> signUp(String email, String password) {
        CreateUserRequestDto userRequest = new CreateUserRequestDto();
        userRequest.setEmail(email);
        userRequest.setPassword(password);
        userRequest.setRole(UserRole.CUSTOMER);
        userRequest.setRegionId(new Random().nextLong(10, 50));
        userRequest.setCity("Test City");
        userRequest.setStreet("Test Street");
        userRequest.setName("Test User");
        return this.restTemplate.postForEntity("/users/signup", userRequest, Void.class);
    }

    private SignInUserResponseDto signIn(String email, String password) {
        SignInUserRequestDto signInRequest = new SignInUserRequestDto();
        signInRequest.setEmail(email);
        signInRequest.setPassword(password);
        signInRequest.setRole(UserRole.CUSTOMER);

        ResponseEntity<SignInUserResponseDto> signInResponse = this.restTemplate.postForEntity(
            "/users/signin",
            signInRequest,
            SignInUserResponseDto.class);
        assertEquals(HttpStatus.OK, signInResponse.getStatusCode());

        SignInUserResponseDto responseBody = signInResponse.getBody();
        assertNotNull(responseBody);
        return responseBody;
    }

    private void createTestUser(String email,
                                String name,
                                String phone,
                                UserRole role,
                                Long regionId,
                                String city,
                                String street) {
        CreateUserRequestDto userRequest = new CreateUserRequestDto();
        userRequest.setName(name);
        userRequest.setEmail(email);
        userRequest.setPhone(phone);
        userRequest.setPassword("password");
        userRequest.setRole(role);
        userRequest.setRegionId(regionId);
        userRequest.setCity(city);
        userRequest.setStreet(street);

        ResponseEntity<Void> signupResponse =
            this.restTemplate.postForEntity("/users/signup", userRequest, Void.class);
        assertEquals(HttpStatus.OK, signupResponse.getStatusCode());
    }
}
