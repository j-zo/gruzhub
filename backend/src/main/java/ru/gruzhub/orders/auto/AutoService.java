package ru.gruzhub.orders.auto;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.gruzhub.orders.auto.dto.AutoResponseDto;
import ru.gruzhub.orders.auto.models.Auto;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.dto.UserResponseDto;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;

@Service
@RequiredArgsConstructor
public class AutoService {
    private final UsersService usersService;
    private final AutoRepository autoRepository;

    public Auto createAuto(Auto auto) {
        // Check for existing VIN
        if (auto.getVin() != null) {
            Optional<Auto> sameVinAuto = this.autoRepository.findByVin(auto.getVin());
            if (sameVinAuto.isPresent()) {
                auto.setId(sameVinAuto.get().getId());
                return this.updateAuto(auto);
            }
        }

        // Check for existing Number
        if (auto.getNumber() != null) {
            Optional<Auto> sameNumberAuto = this.autoRepository.findByNumber(auto.getNumber());
            if (sameNumberAuto.isPresent()) {
                auto.setId(sameNumberAuto.get().getId());
                return this.updateAuto(auto);
            }
        }

        auto.setMerged(false);
        return this.autoRepository.save(auto);
    }

    public Auto updateAuto(Auto auto) {
        Auto existingAuto = this.getAutoById(auto.getId());

        Auto originalAuto =
            this.getOriginalAutoByVinOrNumber(auto.getId(), auto.getVin(), auto.getNumber());

        if (originalAuto != null) {
            existingAuto.setMerged(true);
            existingAuto.setMergedTo(originalAuto);

            this.mergeDuplicatedAutoFieldsToOriginalAuto(existingAuto, originalAuto);

            existingAuto = originalAuto;
        }

        if (auto.getCustomer() != null) {
            existingAuto.setCustomer(auto.getCustomer());
        }
        if (auto.getDriver() != null) {
            existingAuto.setDriver(auto.getDriver());
        }
        if (auto.getBrand() != null) {
            existingAuto.setBrand(auto.getBrand());
        }
        if (auto.getModel() != null) {
            existingAuto.setModel(auto.getModel());
        }
        if (auto.getVin() != null) {
            existingAuto.setVin(auto.getVin());
        }
        if (auto.getNumber() != null) {
            existingAuto.setNumber(auto.getNumber());
        }
        if (auto.getType() != null) {
            existingAuto.setType(auto.getType());
        }

        return this.autoRepository.save(existingAuto);
    }

    public Auto getAutoById(Long autoId) {
        return this.autoRepository.findById(autoId)
                                  .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                                 "Auto not found"));
    }

    public AutoResponseDto getAutoByIdWithAuth(String authorization, Long autoId) {
        User user = this.usersService.getUserFromToken(authorization);
        Auto auto = this.getAutoById(autoId);

        if ((auto.getDriver() != null && !auto.getDriver().getId().equals(user.getId())) &&
            (auto.getCustomer() != null && !auto.getCustomer().getId().equals(user.getId())) &&
            user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return this.convertToAutoDto(auto);
    }

    private Auto getOriginalAutoByVinOrNumber(Long excludeId, String vin, String number) {
        if (vin != null) {
            Optional<Auto> autoByVin = this.autoRepository.findByVinAndIdNot(vin, excludeId);
            if (autoByVin.isPresent()) {
                return autoByVin.get();
            }
        }

        if (number != null) {
            Optional<Auto> autoByNumber =
                this.autoRepository.findByNumberAndIdNot(number, excludeId);
            if (autoByNumber.isPresent()) {
                return autoByNumber.get();
            }
        }

        return null;
    }

    private void mergeDuplicatedAutoFieldsToOriginalAuto(Auto newAuto, Auto originalAuto) {
        if (newAuto.getDriver() != null && originalAuto.getDriver() == null) {
            originalAuto.setDriver(newAuto.getDriver());
        }
        if (newAuto.getCustomer() != null && originalAuto.getCustomer() == null) {
            originalAuto.setCustomer(newAuto.getCustomer());
        }
    }

    private AutoResponseDto convertToAutoDto(Auto auto) {
        AutoResponseDto autoResponseDto = new AutoResponseDto();
        autoResponseDto.setId(auto.getId());
        autoResponseDto.setType(auto.getType());
        autoResponseDto.setBrand(auto.getBrand());
        autoResponseDto.setModel(auto.getModel());
        autoResponseDto.setVin(auto.getVin());
        autoResponseDto.setNumber(auto.getNumber());

        if (auto.getCustomer() != null) {
            autoResponseDto.setCustomer(this.convertToUserResponseDto(auto.getCustomer()));
        }
        if (auto.getDriver() != null) {
            autoResponseDto.setDriver(this.convertToUserResponseDto(auto.getDriver()));
        }

        return autoResponseDto;
    }

    private UserResponseDto convertToUserResponseDto(User user) {
        UserResponseDto userDto = new UserResponseDto();
        userDto.setId(user.getId());
        userDto.setName(user.getName());
        userDto.setEmail(user.getEmail());
        // Add other necessary fields
        return userDto;
    }
}
