package com.upload.service.impl.visitors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
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


    public Collection<String> filterByRegex(String regex) {
        Collection<String> result;
        Pattern pattern = Pattern.compile(regex);
        Predicate<String> filter = fileName -> pattern.matcher(fileName).find();
        synchronized (lock) {
            result = currentSet.stream().filter(filter)
                    .collect(Collectors.toSet());
            if (serializedSetsCount > 0) {
                // TODO: serialize currentSet
                forTheSerilizedSets(set -> set.stream().filter(filter).forEach(result::add), o -> true);
                // TODO: restore currentSet
            }
        }
        return result;
    }

    @Override
    public boolean remove(Object o) {
        AtomicBoolean removed = new AtomicBoolean();
        synchronized (lock) {
            size = decrement();
            if(!currentSet.remove(o) && serializedSetsCount > 0) {
                // TODO: serialize current
                forTheSerilizedSets(set -> removed.compareAndSet(false, set.remove(o)),
                        index -> !removed.get());
                // TODO : restore current
            } else {
                removed.set(true);
            }
        }
        return removed.get();
    }

    private void forTheSerilizedSets(Consumer<Set<String>> serilizedSetConsumer, Predicate<Long> customBreakCondition) {
        Predicate<Long> indexCondition = index -> index < serializedSetsCount;
        for (long i = 0; indexCondition.or(customBreakCondition).test(i); ++i) {
            try {
                ObjectInputStream stream = new ObjectInputStream(
                        new FileInputStream(indexFilePath.resolve(i + "").toFile()));
                serilizedSetConsumer.accept((Set<String>) stream.readObject());
            } catch (IOException | ClassNotFoundException e) {
                LOGGER.error("Failed to read serialized set {}", i, e);
            }
        }
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
