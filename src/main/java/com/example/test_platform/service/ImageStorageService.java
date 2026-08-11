package com.example.test_platform.service;

import com.example.test_platform.config.StorageProperties;
import com.example.test_platform.domain.entity.Task;
import com.example.test_platform.domain.entity.TaskImage;
import com.example.test_platform.exception.ApiException;
import com.example.test_platform.repository.TaskImageRepository;
import com.example.test_platform.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final StorageProperties storageProperties;
    private final TaskImageRepository taskImageRepository;

    public TaskImage storeTaskImage(Task task, MultipartFile file) {
        validateImage(file);

        try {
            StoredImage storedImage = optimizeAndStore(task.getPublicId(), file);
            int nextSortOrder = task.getImages().stream()
                    .mapToInt(TaskImage::getSortOrder)
                    .max()
                    .orElse(-1) + 1;

            TaskImage taskImage = new TaskImage();
            taskImage.setFilePath(storedImage.relativePath());
            taskImage.setOriginalFilename(file.getOriginalFilename() == null ? "image.jpg" : file.getOriginalFilename());
            taskImage.setWidthPx(storedImage.widthPx());
            taskImage.setSortOrder(nextSortOrder);
            task.addImage(taskImage);

            return taskImageRepository.save(taskImage);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to process image");
        }
    }

    public void deleteTaskImage(TaskImage image) {
        deleteFile(image.getFilePath());
        taskImageRepository.delete(image);
    }

    public void deleteAllTaskImages(Task task) {
        for (TaskImage image : task.getImages()) {
            deleteFile(image.getFilePath());
        }
    }

    public Path resolveImagePath(String relativePath) {
        Path basePath = Path.of(storageProperties.getImagesPath()).toAbsolutePath().normalize();
        Path imagePath = basePath.resolve(relativePath).normalize();

        if (!imagePath.startsWith(basePath)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid image path");
        }

        return imagePath;
    }

    public Path resolvePublicImagePath(String taskPublicId, String filename) {
        Path basePath = Path.of(storageProperties.getImagesPath()).toAbsolutePath().normalize();
        Path imagePath = basePath.resolve(taskPublicId).resolve(filename).normalize();

        if (!imagePath.startsWith(basePath)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid image path");
        }

        return imagePath;
    }

    private StoredImage optimizeAndStore(String taskPublicId, MultipartFile file) throws IOException {
        Path taskDirectory = Path.of(storageProperties.getImagesPath(), taskPublicId);
        Files.createDirectories(taskDirectory);

        String extension = resolveExtension(file);
        String filename = UUID.randomUUID() + extension;
        Path destination = taskDirectory.resolve(filename);

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage original = ImageIO.read(inputStream);
            if (original == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported image format");
            }

            BufferedImage optimized = resizeToStandardWidth(original);
            ImageIO.write(optimized, extensionToFormat(extension), destination.toFile());

            String relativePath = taskPublicId + "/" + filename;
            return new StoredImage(relativePath, optimized.getWidth());
        }
    }

    private BufferedImage resizeToStandardWidth(BufferedImage original) {
        int targetWidth = storageProperties.getImageTargetWidth();
        if (original.getWidth() <= targetWidth) {
            return copyImage(original);
        }

        int targetHeight = Math.max(1, (int) Math.round(
                (double) original.getHeight() / original.getWidth() * targetWidth
        ));

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return resized;
    }

    private BufferedImage copyImage(BufferedImage original) {
        BufferedImage copy = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(original, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Image file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, WEBP and GIF images are supported");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/gif".equalsIgnoreCase(contentType)) {
            return ".gif";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }

    private String extensionToFormat(String extension) {
        return switch (extension) {
            case ".png" -> "png";
            case ".gif" -> "gif";
            case ".webp" -> "webp";
            default -> "jpg";
        };
    }

    private void deleteFile(String relativePath) {
        try {
            Path filePath = Path.of(storageProperties.getImagesPath()).resolve(relativePath);
            Files.deleteIfExists(filePath);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete image file");
        }
    }

    private record StoredImage(String relativePath, int widthPx) {
    }
}
