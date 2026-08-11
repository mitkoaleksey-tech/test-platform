package com.example.test_platform.controller;

import com.example.test_platform.exception.ApiException;
import com.example.test_platform.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@RestController
@RequestMapping("/api/public/images")
@RequiredArgsConstructor
public class PublicImageController {

    private final ImageStorageService imageStorageService;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getDirectTaskImage(@PathVariable String filename) {
        Path imagePath = imageStorageService.resolveImagePath(filename);

        if (!Files.exists(imagePath) || !Files.isReadable(imagePath)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Image not found: " + filename);
        }

        Resource resource = new FileSystemResource(imagePath);
        MediaType mediaType = determineMediaType(filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .contentType(mediaType)
                .body(resource);
    }

    @GetMapping("/{taskPublicId}/{filename:.+}")
    public ResponseEntity<Resource> getTaskImage(
            @PathVariable String taskPublicId,
            @PathVariable String filename
    ) {
        Path imagePath = imageStorageService.resolvePublicImagePath(taskPublicId, filename);

        if (!Files.exists(imagePath) || !Files.isReadable(imagePath)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Image not found");
        }

        Resource resource = new FileSystemResource(imagePath);
        MediaType mediaType = determineMediaType(filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .contentType(mediaType)
                .body(resource);
    }

    private MediaType determineMediaType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.valueOf("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
