package com.upload.service.gateway.rest;

import com.upload.service.api.IStorageService;
import com.upload.service.api.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by macbookproritena on 1/19/19.
 */
@RestController
public class UploadRestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadRestController.class);
    private static final String DONE = "Operation succeeded";
    private static final String FAILED = "Operation failed";
    private final IStorageService storageService;

    @Autowired
    public UploadRestController(IStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        ResponseEntity<String> result;
        try {
            validate(file);
            try (InputStream stream = file.getInputStream()) {
                String path = StringUtils.cleanPath(file.getOriginalFilename());
                storageService.create(path, stream);
                result = ResponseEntity.ok(DONE);
            }
        } catch (Exception e) {
            LOGGER.error("File upload of {} failed", file.getName(), e);
            result = ResponseEntity.badRequest().body(e.getMessage());
        }
        return result;
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> get(@PathVariable String filename) {
        ResponseEntity<Resource> result;
        try {
            Optional<Path> filePathOp = storageService.read(filename);
            if(filePathOp.isPresent()) {
                ByteArrayResource res = new ByteArrayResource(Files.readAllBytes(filePathOp.get()));
                result = ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/octet-stream"))
                        .body(res);
            } else {
                result = ResponseEntity.notFound().build();
            }
        } catch (StorageException | IOException e) {
            LOGGER.error("Failed to fetch the file named {}", filename, e);
            result = ResponseEntity.badRequest().build();
        }
        return result;
    }

    @PutMapping("/{filename:.+}")
    public ResponseEntity<String> update(@PathVariable String fileName,
                                         @RequestParam("file") MultipartFile file) {
        ResponseEntity<String> result;
        try {
            validate(file);
            try (InputStream stream = file.getInputStream()) {
                storageService.update(StringUtils.cleanPath(file.getOriginalFilename()), stream);
                result = ResponseEntity.ok(DONE);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update the file named {}", fileName, e);
            result = ResponseEntity.badRequest().body(FAILED);
        }
        return result;
    }

    @DeleteMapping("/{filename:.+}")
    public ResponseEntity<String> delete(String filename) {
        boolean deleted = storageService.delete(filename);
        return deleted
                ? ResponseEntity.ok(DONE)
                : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/")
    public ResponseEntity<String> deleteAll() {
        storageService.deleteAll();
        return ResponseEntity.ok(DONE);
    }

    @GetMapping("/size")
    public ResponseEntity<String> getStorageSize() {
        return ResponseEntity.ok(storageService.getFilesCount().toString());
    }

    // TODO add limit and pagination
    @GetMapping("/enum")
    public ResponseEntity<Collection<String>> enumerateFiltered(@RequestParam("filter") String filter) {
        Collection<String> paths = storageService.filterByRegex(filter);
        return paths.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(paths);
    }

    private static void validate(MultipartFile file) {
        Objects.requireNonNull(file, "Input file is null");
        Objects.requireNonNull(file.getOriginalFilename(), "Input file name is null");
        if(file.getOriginalFilename().contains("..")) {
            throw new IllegalArgumentException("Input file has relative file path");
        }
    }
}
