package ru.gruzhub.adresses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import ru.gruzhub.address.models.Region;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RegionsControllerTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testGetRegions() {
        ResponseEntity<Region[]> response =
            this.restTemplate.getForEntity("/addresses/regions", Region[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Region[] regions = response.getBody();
        assertNotNull(regions);
        assertTrue(regions.length > 0);
        assertNotNull(regions[0].getId());
        assertNotNull(regions[0].getName());
    }
}
