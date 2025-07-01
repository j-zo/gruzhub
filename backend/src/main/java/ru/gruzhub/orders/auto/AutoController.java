package ru.gruzhub.orders.auto;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.gruzhub.orders.auto.dto.AutoResponseDto;
import ru.gruzhub.orders.auto.dto.AutoUploadResponseDto;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/auto")
@RequiredArgsConstructor
public class AutoController {

    private static final Logger logger = LoggerFactory.getLogger(AutoController.class);
    private final AutoService autoService;

    @GetMapping("/{autoId}")
    public AutoResponseDto getAutoById(@PathVariable Long autoId,
                                       @RequestHeader("Authorization") String authorization) {
        return this.autoService.getAutoByIdWithAuth(authorization, autoId);
    }

    @PostMapping(path = "/upload", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<AutoUploadResponseDto> uploadExcelFile(@RequestParam("file") MultipartFile document) {
        if (document.isEmpty()) {
            logger.error("Uploaded file is empty");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // TODO: parse the file and save to the db

        // Log file details
        String fileName = document.getOriginalFilename();
        String fileType = document.getContentType();
        long fileSize = document.getSize();

        logger.info("Received file: Name={}, Type={}, Size={}", fileName, fileType, fileSize);

        // Validate file type by extension
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            logger.error("Unsupported file type: {}", fileType);
            return new ResponseEntity<>(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        try (InputStream is = document.getInputStream()) {
            return new ResponseEntity<>(HttpStatus.OK);

        } catch (IOException e) {
            logger.error("IOException occurred while processing file", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
