package com.upload.service.impl.visitors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by macbookproritena on 1/26/19.
 */
public class PartiallySerializedSet implements Set<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartiallySerializedSet.class);
    private final long memThreshHold;
    private final Path indexFilePath;
    private final Object lock = new Object();

    private BigDecimal size;
    private Set<String> currentSet;
    private long serializedSetsCount;

    public PartiallySerializedSet(Path indexFilesPath, long memThreshHold) {
        if(!Files.isDirectory(indexFilesPath)) {
            throw new IllegalArgumentException("Index root not a directory");
        }
        this.memThreshHold = memThreshHold;
        this.indexFilePath = indexFilesPath;
        this.size = BigDecimal.ZERO;
        this.currentSet = new HashSet<>();
        this.serializedSetsCount = 0L;
    }

    public Collection<String> filterByRegex(String regex) {
        Pattern pattern = Pattern.compile(regex);
        Collection<String> result = currentSet.stream().filter(fileName -> pattern.matcher(fileName).find())
                .collect(Collectors.toSet());
        if(serializedSetsCount > 0) {
            // TODO: complete
        }
        return result;
    }

    @Override
    public boolean add(String s) {
        synchronized (lock) {
            size = increment();
            currentSet.add(s);
            if(!memoryStillAvailable()) {
                try {
                    Path newFile = Files.createFile(indexFilePath.resolve(serializedSetsCount++ + ""));
                    ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(newFile.toFile()));
                    stream.writeObject(currentSet);
                    stream.flush();
                    stream.close();
                    currentSet = new HashSet<>();
                } catch (Exception e) {
                    LOGGER.error("Failed to serialize", e);
                }
            }
        }
        return true;
    }

    @Override
    public boolean remove(Object o) {
        synchronized (lock) {
            size = decrement();
            // TODO: complete
        }
        return true;
    }

    @Override
    public void clear() {
        synchronized (lock) {
            size = BigDecimal.ZERO;
            currentSet.clear();
            try {
                Files.deleteIfExists(indexFilePath);
            } catch (IOException e) {
                LOGGER.error("Failed to clean", e);
            }
        }
    }

    public BigDecimal bigSize() {
        synchronized (lock) {
            return size;
        }
    }

    @Override
    public boolean isEmpty() {
        return size.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator<String> iterator() {
        throw new NotImplementedException();
    }

    @Override
    public Object[] toArray() {
        throw new NotImplementedException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new NotImplementedException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new NotImplementedException();
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        c.forEach(this::add);
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new NotImplementedException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new NotImplementedException();
    }

    @Override
    public int size() {
        synchronized (lock) {
            return size.intValue();
        }
    }

    private BigDecimal increment() {
        return size.add(BigDecimal.ONE);
    }

    private BigDecimal decrement() {
        return size.subtract(BigDecimal.ONE);
    }

    private boolean memoryStillAvailable() {
        long allocatedMemory =
                (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
        long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;
        return presumableFreeMemory > memThreshHold;
    }
}
