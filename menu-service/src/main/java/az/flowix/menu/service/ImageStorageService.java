package az.flowix.menu.service;

import az.flowix.menu.config.StorageProperties;

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
    private static final int MAGIC_CHECK_SIZE = 12;

    private static final String JPEG = "image/jpeg";
    private static final String PNG  = "image/png";
    private static final String WEBP = "image/webp";

    private static final Map<String, String> EXTENSIONS = Map.of(
            JPEG, "jpg",
            PNG,  "png",
            WEBP, "webp"
    );

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

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }

        String contentType = detectContentType(bytes);
        if (contentType == null) {
            throw new IllegalArgumentException("Only JPEG, PNG and WebP images are allowed");
        }

        var extension = EXTENSIONS.get(contentType);
        var filename = itemId + "." + extension;
        try {
            Files.createDirectories(baseDir);
            removeExistingImages(itemId);
            Files.write(baseDir.resolve(filename), bytes);
        } catch (IOException e) {
            log.error("Failed to store image for item {}", itemId, e);
            throw new IllegalStateException("Failed to store image", e);
        }

        log.info("Image stored for item {}: {}", itemId, filename);
        return publicBaseUrl + "/api/menu-ms/v1/images/" + filename;
    }

    public void deleteItemImage(UUID itemId) {
        try {
            removeExistingImages(itemId);
            log.info("Image deleted for item {}", itemId);
        } catch (IOException e) {
            log.warn("Failed to delete image for item {}: {}", itemId, e.getMessage());
        }
    }

    private void removeExistingImages(UUID itemId) throws IOException {
        try (var stream = Files.newDirectoryStream(baseDir, itemId + ".*")) {
            for (var existing : stream) {
                Files.deleteIfExists(existing);
            }
        }
    }

    private String detectContentType(byte[] header) {
        if (header.length < 4) {
            return null;
        }
        // PNG: 89 50 4E 47
        if (header[0] == (byte) 0x89 && header[1] == 0x50
                && header[2] == 0x4E && header[3] == 0x47) {
            return PNG;
        }
        // JPEG: FF D8 FF
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
            return JPEG;
        }
        // WebP: RIFF....WEBP (WEBP at offset 8)
        if (header.length >= 12
                && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return WEBP;
        }
        return null;
    }

    private String stripTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
