package com.upload.service.api;

/**
 * Created by macbookproritena on 1/24/19.
 */
public class StorageException extends Exception {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
