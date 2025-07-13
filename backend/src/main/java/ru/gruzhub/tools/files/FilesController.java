package ru.gruzhub.tools.files;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.gruzhub.tools.files.models.File;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/files")
public class FilesController {
    private final FilesService filesService;

    @Autowired
    public FilesController(FilesService filesService) {
        this.filesService = filesService;
    }

    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getFile(@PathVariable String filename) {
        String code = filename.split("\\.")[0];
        File file = this.filesService.getFileByCode(code);
        byte[] fileBytes = this.filesService.getBinaryFile(code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.getContentType()));
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                                                        .filename(file.getFilename())
                                                        .build());

        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }
}
