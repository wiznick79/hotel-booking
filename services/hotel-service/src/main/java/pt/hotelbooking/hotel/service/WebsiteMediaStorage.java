package pt.hotelbooking.hotel.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class WebsiteMediaStorage {
    private final Path root;

    public WebsiteMediaStorage(@Value("${website-media.storage-path:./data/website-media}") String path) {
        this.root = Path.of(path).toAbsolutePath().normalize();
        try { Files.createDirectories(root); }
        catch (IOException exception) { throw new IllegalStateException("Could not initialize media storage.", exception); }
    }

    public void store(String key, byte[] bytes) {
        Path target = resolve(key);
        try {
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Could not store the image.", exception);
        }
    }

    public Resource load(String key) {
        try {
            Resource resource = new UrlResource(resolve(key).toUri());
            if (!resource.exists() || !resource.isReadable()) throw new ResponseStatusException(NOT_FOUND, "Image content was not found.");
            return resource;
        } catch (IOException exception) {
            throw new ResponseStatusException(NOT_FOUND, "Image content was not found.", exception);
        }
    }

    public void delete(String key) {
        try { Files.deleteIfExists(resolve(key)); }
        catch (IOException exception) { throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Could not delete the image.", exception); }
    }

    private Path resolve(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) throw new IllegalArgumentException("Invalid media storage key.");
        return resolved;
    }
}
