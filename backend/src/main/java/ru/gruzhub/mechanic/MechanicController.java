package ru.gruzhub.mechanic;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.gruzhub.mechanic.dto.MechanicDto;

@RestController
@RequestMapping("/mechanic")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class MechanicController {

    private final MechanicService mechanicService;

    @PostMapping("/")
    public MechanicDto createMechanic(@RequestBody MechanicDto mechanicDto) {
        return null;
    }
}
