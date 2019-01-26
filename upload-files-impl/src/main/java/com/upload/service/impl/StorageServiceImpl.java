package com.upload.service.impl;

import com.upload.service.api.IStorageService;
import com.upload.service.api.StorageException;
import com.upload.service.impl.visitors.CountVisitor;
import com.upload.service.impl.visitors.RegexVisitor;
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
import java.util.Collections;
import java.util.Optional;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by macbookproritena on 1/24/19.
 */
@Service
public class StorageServiceImpl implements IStorageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageServiceImpl.class);
    private final AtomicBigDecimal count;
    private final Path filesRoot;

    @Autowired
    public StorageServiceImpl(StorageConfig configuration) throws IOException {
        this.filesRoot = Paths.get(configuration.getPath());
        this.count = new AtomicBigDecimal(initForPath(filesRoot));
    }

    private static BigDecimal initForPath(Path root) throws IOException {
        if(root == null || StringUtils.isEmpty(root.toString())) {
            throw new IllegalArgumentException("Invalid root path");
        }
        CountVisitor visitor = new CountVisitor();
        if(Files.exists(root)) {
            Files.walkFileTree(root, visitor);
        } else {
            Files.createDirectories(root);
        }
        return visitor.getCount();
    }

    @Override
    public BigDecimal getFilesCount() {
        return Optional.ofNullable(count)
                .map(AtomicBigDecimal::get)
                .orElseGet(() -> new BigDecimal(0));
    }

    @Override
    public void create(String fileName, InputStream fileStream) throws StorageException {
        if(this.read(fileName).isPresent()) {
            throw new StorageException(String.format("File %s already exists", fileName));
        }
        try {
            Files.copy(fileStream, this.filesRoot.resolve(fileName), REPLACE_EXISTING);
            count.increment();
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
                count.increment();
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
                count.decrement();
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
            count.reset();
            FileSystemUtils.deleteRecursively(filesRoot);
        } catch (IOException e) {
            LOGGER.error("Delete all failed");
        }
    }

    @Override
    public Collection<Path> filterByRegex(String regex) {
        Collection<Path> result = Collections.emptyList();
        try {
            RegexVisitor visitor = new RegexVisitor(regex);
            Files.walkFileTree(filesRoot, visitor);
            result = visitor.getMatched();
        } catch (IOException e) {
            LOGGER.error("Walk file failed", e);
        }
        return result;
    }
}
