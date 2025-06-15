package ru.gruzhub.tools.files;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.gruzhub.tools.files.enums.FileType;
import ru.gruzhub.tools.files.models.File;
import ru.gruzhub.users.models.User;

@Service
@RequiredArgsConstructor
public class FilesService {
    private final FileRepository fileRepository;

    public File createFile(User user, byte[] fileBytes, String filename, String extension) {
        final long MAX_SIZE_BYTES = 104_857_600; // 100 MB

        if (fileBytes.length > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Разрешены файлы до 100Мб");
        }

        String code =
            String.format("%d-%d-%s", System.currentTimeMillis(), user.getId(), UUID.randomUUID());
        String location = "./files/" + code;

        FileType fileType = this.getFileTypeByExtension(extension);
        String contentType = this.getContentTypeForExtension(extension);

        File file = File.builder()
                        .code(code)
                        .location(location)
                        .filename(filename)
                        .extension(extension)
                        .contentType(contentType)
                        .type(fileType)
                        .fileSizeBytes((long) fileBytes.length)
                        .user(user)
                        .createdAt(System.currentTimeMillis())
                        .build();

        try {
            Path filePath = Paths.get(location);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, fileBytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                              "Failed to save file.",
                                              e);
        }

        return this.fileRepository.save(file);
    }

    public byte[] getBinaryFile(String code) {
        File file = this.fileRepository.findById(code)
                                       .orElseThrow(() -> new EntityNotFoundException(
                                           "File not found with code: " + code));

        Path filePath = Paths.get(file.getLocation());
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                              "Failed to read file.",
                                              e);
        }
    }

    public File getFileByCode(String code) {
        return this.fileRepository.findById(code)
                                  .orElseThrow(() -> new EntityNotFoundException(
                                      "File not found with code: " + code));
    }

    private String getContentTypeForExtension(String extension) {
        return switch (extension.toLowerCase()) {
            // Images
            case "svg" -> "image/svg+xml";
            case "jpeg", "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            // Videos
            case "mpeg" -> "video/mpeg";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "ogg" -> "video/ogg";
            case "avi" -> "video/x-msvideo";
            case "flv" -> "video/x-flv";
            case "wmv" -> "video/x-ms-wmv";
            // Files
            case "pdf" -> "application/pdf";
            case "xml" -> "application/xml";
            case "csv" -> "application/csv";
            case "doc", "docx", "xls", "xlsx" -> "application/msword";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                         "Unsupported file extension: " +
                                                         extension);
        };
    }

    private FileType getFileTypeByExtension(String extension) {
        return switch (extension.toLowerCase()) {
            case "svg", "jpeg", "jpg", "png", "webp" -> FileType.IMAGE;
            default -> FileType.FILE;
        };
    }
}
