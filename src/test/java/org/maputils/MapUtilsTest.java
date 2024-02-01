package org.maputils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("unchecked")
public class MapUtilsTest {

    private Map<String, Object> map1;
    private Map<String, Object> map2;
    private MapUtils mapUtils;

    @BeforeEach
    public void init() {
        this.map1 = this.getMap("mock-object-1.json");
        this.map2 = this.getMap("mock-object-2.json");
        this.mapUtils = new MapUtils();
    }

    @Test
    public void testNullMap() {
        Optional<Object> result = this.mapUtils.read(null, "somePath");
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testEmptyPath() {
        Optional<Object> result = this.mapUtils.read(this.map1, null);
        Assertions.assertFalse(result.isPresent());
        result = this.mapUtils.read(this.map1, "");
        Assertions.assertFalse(result.isPresent());
        result = this.mapUtils.read(this.map1, "   ");
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testInvalidPath() {
        Optional<Object> result = this.mapUtils.read(this.map1, "invalid");
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testReadSimpleField() {
        Optional<List<Map<String, Object>>> people = this.mapUtils.read(this.map1, "people");
        Assertions.assertTrue(people.isPresent());
    }

    @Test
    public void testReadList() {
        Optional<Map<String, Object>> person = this.mapUtils.read(this.map1, "people.0");
        Assertions.assertTrue(person.isPresent());
    }

    @Test
    public void testReadNestedField() {
        Optional<String> firstName = this.mapUtils.read(this.map1, "people.0.firstName");
        Optional<String> hobby = this.mapUtils.read(this.map1, "people.0.hobbies.0");
        Assertions.assertEquals("John", firstName.orElse(null));
        Assertions.assertEquals("Hiking", hobby.orElse(null));
    }

    @Test
    public void testMergeWithCollectionKeys() {
        Map<String, String> collectionKeys = new HashMap<String, String>() {{
           put("people", "id");
           put("identifiers", "type");
        }};
        this.mapUtils.merge(this.map1, this.map2, collectionKeys);
        // John assertions
        String johnNickname = (String) this.mapUtils.read(this.map1, "people.0.identifiers.1.value").orElse(null);
        String johnSsn = (String) this.mapUtils.read(this.map1, "people.0.identifiers.0.value").orElse(null);
        String johnFirstName = (String) this.mapUtils.read(this.map1, "people.0.firstName").orElse(null);
        String johnLastName = (String) this.mapUtils.read(this.map1, "people.0.lastName").orElse(null);
        Integer johnId = (Integer) this.mapUtils.read(this.map1, "people.0.id").orElse(null);
        Integer johnAge = (Integer) this.mapUtils.read(this.map1, "people.0.age").orElse(null);
        List<String> johnHobbies = (List<String>) this.mapUtils.read(this.map1, "people.0.hobbies").orElse(new ArrayList<>());
        List<Integer> johnClasses = (List<Integer>) this.mapUtils.read(this.map1, "people.0.classes").orElse(new ArrayList<>());
        Assertions.assertEquals("Johnny", johnNickname);
        Assertions.assertEquals("345-67-8900", johnSsn);
        Assertions.assertEquals("John", johnFirstName);
        Assertions.assertEquals("Doe", johnLastName);
        Assertions.assertEquals(1, johnId);
        Assertions.assertEquals(25, johnAge);
        Assertions.assertEquals(3, johnHobbies.size());
        Assertions.assertTrue(johnHobbies.contains("Hiking"));
        Assertions.assertTrue(johnHobbies.contains("Baking"));
        Assertions.assertTrue(johnHobbies.contains("Painting"));
        Assertions.assertEquals(1, johnClasses.size());
        Assertions.assertEquals(1, johnClasses.get(0));
        // John Wick assertions, Jane Smith assertions
        List<Map<String, Object>> people = (List<Map<String, Object>>) this.mapUtils.read(this.map1, "people").orElse(new ArrayList<>());
        Assertions.assertEquals(3, people.size());
        // Classes assertion
        List<Object> classes = (List<Object>) this.mapUtils.read(this.map1, "classes").orElse(new ArrayList<>());
        Assertions.assertEquals(1, classes.size());
    }

    private Map<String, Object> getMap(String fileName) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            if (in != null) {
                String json = IOUtils.toString(in, StandardCharsets.UTF_8);
                return (Map<String, Object>) new ObjectMapper().readValue(json, LinkedHashMap.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new LinkedHashMap<>();
    }

}
