package ru.gruzhub.tools.ping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.gruzhub.tools.ping.dto.PingResponseDto;

@RestController
@RequestMapping("ping")
@Tag(name = "Ping")
public class PingController {
  @Operation(
      summary = "Ping API for availability",
      description =
          "This endpoint should be used for checking whether API"
              + "(without DB or other services) is alive")
  @GetMapping("/ping")
  public PingResponseDto ping() {
    return new PingResponseDto("OK");
  }
}
