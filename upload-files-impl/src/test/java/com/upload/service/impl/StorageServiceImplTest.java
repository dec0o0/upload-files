package com.upload.service.impl;

import com.upload.service.api.IStorageService;
import com.upload.service.api.StorageException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

/**
 * Created by macbookproritena on 1/24/19.
 */
public class StorageServiceImplTest {
    private IStorageService service;

    public StorageServiceImplTest() throws IOException {
        service = new StorageServiceImpl(new StorageConfig());
    }

    @After
    public void cleanup() {
        service.deleteAll();
    }

    @Before
    public void makeSureWeStartFromScratch() {
        Assert.assertEquals(0, service.getFilesCount().intValue());
    }

    @Test
    public void getFilesCount() throws Exception {
        int count = 11;
        insertNFiles(count);
        Assert.assertEquals(count, service.getFilesCount().intValue());
    }

    @Test
    public void create() throws Exception {
        service.create(
                "1.txt",
                getClass().getResourceAsStream("/1.txt")
        );
    }

    @Test
    public void read() throws Exception {
        service.create(
                "1.txt",
                getClass().getResourceAsStream("/1.txt")
        );
        Optional<Path> pathOp = service.read("1.txt");
        Assert.assertTrue(pathOp.isPresent());
        Assert.assertTrue(Files.exists(pathOp.get()));
        BufferedReader reader = new BufferedReader(new FileReader(pathOp.get().toFile()));
        Assert.assertEquals("a", reader.readLine());
    }

    @Test(expected = StorageException.class)
    public void createDuplicate() throws StorageException {
        service.create(
                "1.txt",
                getClass().getResourceAsStream("/1.txt")
        );
        service.create(
                "1.txt",
                getClass().getResourceAsStream("/1.txt")
        );
    }

    @Test
    public void update() throws Exception {
        service.create(
                "1.txt",
                getClass().getResourceAsStream("/1.txt")
        );
        Assert.assertTrue(service.read("1.txt").isPresent());
        Assert.assertEquals(1, service.getFilesCount().intValue());
        service.update("1.txt", getClass().getResourceAsStream("/2.txt"));
        Assert.assertEquals(1, service.getFilesCount().intValue());
        Optional<Path> pathOp = service.read("1.txt");
        Assert.assertTrue(pathOp.isPresent());
        BufferedReader reader = new BufferedReader(new FileReader(pathOp.get().toFile()));
        Assert.assertEquals("b", reader.readLine());
    }

    @Test
    public void delete() throws Exception {
        service.create(
                "1.txt",
                getClass().getResourceAsStream("/1.txt")
        );
        Assert.assertEquals(1, service.getFilesCount().intValue());
        Assert.assertTrue(service.delete("1.txt"));
        Assert.assertEquals(0, service.getFilesCount().intValue());
    }

    @Test
    public void deleteAll() throws Exception {
        int count = 20;
        insertNFiles(count);
        Assert.assertEquals(count, service.getFilesCount().intValue());
        service.deleteAll();
        Assert.assertEquals(0, service.getFilesCount().intValue());
    }

    @Test
    public void filterByRegex() throws Exception {
        int count = 40;
        insertNFiles(count);
        Collection<Path> found = service.filterByRegex("\\d+");
        Assert.assertEquals(count, found.size());
        found = service.filterByRegex("2\\d");
        Assert.assertEquals(10, found.size());
    }


    private void insertNFiles(int count) throws StorageException {
        for(int i = 0; i < count; ++i) {
            service.create(
                    String.format("%d.txt", i + 1),
                    getClass().getResourceAsStream("/1.txt")
            );
        }
    }


}