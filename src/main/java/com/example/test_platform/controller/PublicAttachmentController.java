package com.example.test_platform.controller;

import com.example.test_platform.config.StorageProperties;
import com.example.test_platform.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@RestController
@RequestMapping("/api/public/attachments")
@RequiredArgsConstructor
public class PublicAttachmentController {

    private final StorageProperties storageProperties;

    @GetMapping("/**")
    public ResponseEntity<Resource> getAttachment(HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchingPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String relativePath = path.substring(bestMatchingPattern.length() - 2);

        Path basePath = Path.of(storageProperties.getImagesPath()).toAbsolutePath().normalize();
        Path filePath = basePath.resolve(relativePath).normalize();

        if (!filePath.startsWith(basePath) || !Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Attachment file not found");
        }

        Resource resource = new FileSystemResource(filePath);
        MediaType mediaType = determineMediaType(filePath.getFileName().toString());

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .contentType(mediaType)
                .body(resource);
    }

    private MediaType determineMediaType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (lower.endsWith(".webp")) return MediaType.valueOf("image/webp");
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
