package com.upload.service.impl;

import com.upload.service.api.IStorageService;
import com.upload.service.api.StorageException;
import com.upload.service.impl.visitors.PartiallySerializedSet;
import com.upload.service.impl.visitors.IndexationVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by macbookproritena on 1/24/19.
 */
@Service
public class StorageServiceImpl implements IStorageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageServiceImpl.class);
    private final PartiallySerializedSet files;
    private final Path filesRoot;

    @Autowired
    public StorageServiceImpl(StorageConfig configuration) throws IOException {
        this.filesRoot = Paths.get(configuration.getPath());
        this.files = initFor(filesRoot, Paths.get("/indexes/"), configuration.getMemoryThreshold());
    }

    private static PartiallySerializedSet initFor(Path root, Path indexRoot, long threshHold) throws IOException {
        if(root == null || StringUtils.isEmpty(root.toString())) {
            throw new IllegalArgumentException("Invalid root path");
        }
        Files.deleteIfExists(indexRoot);
        Files.createDirectories(indexRoot);
        IndexationVisitor visitor = new IndexationVisitor(new PartiallySerializedSet(indexRoot, threshHold));
        if(Files.exists(root)) {
            Files.walkFileTree(root, visitor);
        } else {
            Files.createDirectories(root);
        }
        return visitor.getResults();
    }

    @Override
    public BigDecimal getFilesCount() {
        return files.bigSize();
    }

    @Override
    public void create(String fileName, InputStream fileStream) throws StorageException {
        if(this.read(fileName).isPresent()) {
            throw new StorageException(String.format("File %s already exists", fileName));
        }
        try {
            Files.copy(fileStream, this.filesRoot.resolve(fileName), REPLACE_EXISTING);
            files.add(fileName);
        } catch (IOException e) {
            throw new StorageException("File write failed", e);
        }
    }

    @Override
    public Optional<Path> read(String fileName) {
        Path resolved = filesRoot.resolve(Paths.get(fileName));
        return Files.exists(resolved)
                ? Optional.of(resolved)
                : Optional.empty();
    }

    @Override
    public void update(String filename, InputStream updateStream) throws StorageException {
        try {
            Optional<Path> existingFile = this.read(filename);
            Files.copy(updateStream, this.filesRoot.resolve(filename), REPLACE_EXISTING);
            if(!existingFile.isPresent()) {
                files.add(filename);
            }
        } catch (IOException e) {
            throw new StorageException("File write failed", e);
        }
    }

    @Override
    public boolean delete(String fileName) {
        Optional<Path> filePath = this.read(fileName);
        Optional<Boolean> resultOp = filePath.map(path -> {
            try {
                files.remove(fileName);
                return Files.deleteIfExists(path);
            } catch (IOException e) {
                LOGGER.error("Failed to delete {}", path);
                return false;
            }
        });
        return resultOp.orElse(Boolean.FALSE);
    }

    @Override
    public void deleteAll() {
        try {
            files.clear();
            FileSystemUtils.deleteRecursively(filesRoot);
        } catch (IOException e) {
            LOGGER.error("Delete all failed");
        }
    }

    @Override
    public Collection<String> filterByRegex(String regex) {
        return files.filterByRegex(regex);
    }
}
