package com.upload.service.impl.visitors;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Created by macbookproritena on 1/24/19.
 */
public class RegexVisitor implements FileVisitor<Path> {
    private final Pattern pattern;
    private final Collection<Path> matched;

    public RegexVisitor(String pattern) {
        this.pattern = Pattern.compile(Objects.requireNonNull(pattern));
        this.matched = new HashSet<>();
    }

    private boolean matches(Path p) {
        return pattern.matcher(p.getFileName().toString()).find();
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if(matches(file)) {
            // TODO : handle out of memory ? Add offset
            matched.add(file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        if(matches(file)) {
            matched.remove(file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    public Collection<Path> getMatched() {
        return matched;
    }
}
