package com.upload.service.api;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

/**
 * Created by macbookproritena on 1/19/19.
 */
public interface IStorageService {

    BigDecimal getFilesCount();

    void create(String fileName, InputStream fileStream) throws StorageException;

    Optional<Path> read(String fileName) throws StorageException;

    void update(String filename, InputStream updateStream) throws StorageException;

    boolean delete(String fileName);

    void deleteAll();

    Collection<Path> filterByRegex(String regex);
}
