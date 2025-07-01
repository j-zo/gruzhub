package ru.gruzhub.address;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.gruzhub.address.models.Country;
import ru.gruzhub.address.models.Region;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class RegionsController {
    private final RegionsService regionsService;

    @GetMapping("/regions")
    public List<Region> getRegions() {
        return this.regionsService.getRegions();
    }

    @GetMapping("/countries")
    public List<Country> getCountries() {
        return this.regionsService.getCountries();
    }
}
