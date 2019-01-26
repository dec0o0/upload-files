package com.upload.service.itests;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.assertEquals;

/**
 * Created by macbookproritena on 1/24/19.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AppIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void a_testUploadFiles() throws Exception {
        for(int i = 1; i < 9; ++i) {
            ClassPathResource resource = new ClassPathResource(String.format("/%d.txt", i), getClass());
            MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
            map.add("file", resource);
            ResponseEntity<String> response = this.restTemplate.postForEntity("/", map, String.class);
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Test
    public void b_testGet() {
        ClassPathResource resource = new ClassPathResource("1.txt", getClass());

        ResponseEntity<String> response = this.restTemplate
                .getForEntity("/{filename}", String.class, "1.txt");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("a", response.getBody());
    }

    @Test
    public void z_testDeleteAll() {
        restTemplate.delete("/");
    }
}
