package ru.gruzhub.transport;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.gruzhub.transport.dto.TransportDto;
import ru.gruzhub.transport.dto.TransportUploadResponseDto;
import ru.gruzhub.transport.dto.UpdateTransportRequestDto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@RestController
@RequestMapping("/transport")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class TransportController {

    private static final Logger logger = LoggerFactory.getLogger(TransportController.class);
    private final TransportService transportService;

    // TODO: FIX!!!
    @GetMapping("/{transportId}")
    public TransportDto getTransportById(@PathVariable String transportId,
                                         @RequestHeader("Authorization") String authorization) {
        return this.transportService.getTransportByIdWithAuth(authorization, UUID.fromString(transportId));
    }

    @PostMapping
    public TransportDto createTransport(@RequestBody TransportDto createTransportRequestDto) {
        // TODO create transport
        return null;
    }

    @PutMapping
    public ResponseEntity updateTransport(@RequestBody UpdateTransportRequestDto createTransportRequestDto) {
        // TODO create transport
        return null;
    }


    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadTransport() throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Data");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Name");
        header.createCell(2).setCellValue("Email");

        // Add sample data
        // TODO: Replace data with real transport data
        Object[][] data = {
                {1, "Alice", "alice@example.com"},
                {2, "Bob", "bob@example.com"},
                {3, "Charlie", "charlie@example.com"}
        };

        int rowNum = 1;
        for (Object[] rowData : data) {
            Row row = sheet.createRow(rowNum++);
            for (int col = 0; col < rowData.length; col++) {
                row.createCell(col).setCellValue(String.valueOf(rowData[col]));
            }
        }

        // Write to byte array
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        InputStreamResource fileResource = new InputStreamResource(in);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sample.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(fileResource);
    }

    @PostMapping(path = "/upload", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<TransportUploadResponseDto> uploadTransportFromExcel(@RequestParam("file") MultipartFile document) {
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
