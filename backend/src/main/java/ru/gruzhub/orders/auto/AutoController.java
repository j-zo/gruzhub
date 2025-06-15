package ru.gruzhub.orders.auto;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.gruzhub.orders.auto.dto.AutoResponseDto;

@RestController
@RequestMapping("/auto")
@RequiredArgsConstructor
public class AutoController {
    private final AutoService autoService;

    @GetMapping("/{autoId}")
    public AutoResponseDto getAutoById(@PathVariable Long autoId,
                                       @RequestHeader("Authorization") String authorization) {
        return this.autoService.getAutoByIdWithAuth(authorization, autoId);
    }
}
