package az.codlab.menu.service;

import az.codlab.menu.config.StorageProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final Path baseDir;
    private final String publicBaseUrl;

    public ImageStorageService(StorageProperties storageProperties) {
        this.baseDir = Path.of(storageProperties.getBaseDir()).toAbsolutePath().normalize();
        this.publicBaseUrl = stripTrailingSlash(storageProperties.getPublicBaseUrl());
    }

    public String storeImage(UUID itemId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 2MB limit");
        }
        var extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException("Only JPEG, PNG and WebP images are allowed");
        }

        var filename = itemId + "." + extension;
        try {
            Files.createDirectories(baseDir);
            removeExistingImages(itemId);
            try (var in = file.getInputStream()) {
                Files.copy(in, baseDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Failed to store image for item {}", itemId, e);
            throw new IllegalStateException("Failed to store image", e);
        }

        log.info("Image stored for item {}: {}", itemId, filename);
        return publicBaseUrl + "/api/menu-ms/v1/images/" + filename;
    }

    public void deleteImage(String imageUrl) {
        var filename = Path.of(imageUrl).getFileName().toString();
        try {
            Files.deleteIfExists(baseDir.resolve(filename));
            log.info("Image deleted: {}", filename);
        } catch (IOException e) {
            log.warn("Failed to delete image {}: {}", filename, e.getMessage());
        }
    }

    private void removeExistingImages(UUID itemId) throws IOException {
        try (var stream = Files.newDirectoryStream(baseDir, itemId + ".*")) {
            for (var existing : stream) {
                Files.deleteIfExists(existing);
            }
        }
    }

    private String stripTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

}
