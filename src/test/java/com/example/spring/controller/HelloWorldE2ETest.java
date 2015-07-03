package com.example.spring.controller;

import com.example.spring.model.People;
import org.apache.commons.math3.stat.Frequency;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestContextManager;

/**
 * Created by benbendaisy on 6/30/15.
 */
@RunWith(Parameterized.class)
public class HelloWorldE2ETest {
    private static final List<String> blackList = Arrays.asList();
    private static final List<String> whiteList = Arrays.asList();
    private static final List<String> blackListedFields = Arrays.asList();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static Map<String, Frequency> fieldStats = new TreeMap<String, Frequency>();

    private static TestContextManager testContextManager;
    private static HelloWorldE2ETest instant;

    @BeforeClass
    public static void init() throws IOException {
        InputStream resource = null;
        try {
            testContextManager = new TestContextManager(HelloWorldE2ETest.class);
            instant = new HelloWorldE2ETest();

            // this starts the server and binds fields with @Value annotation
            testContextManager.prepareTestInstance(instant);
            resource = HelloWorldE2ETest.class.getClassLoader().getResourceAsStream("sampleData/peopleSample.json");
            People people = objectMapper.readValue(resource, People.class);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(resource);
        }
    }

    @AfterClass
    public static void dumpStats() {
        dumpFieldStats(fieldStats);
    }

    static void dumpFieldStats(Map<String, Frequency> sts) {
        System.out.println("field\tfreq\tuniq\tmin\tmax");
        for (Map.Entry<String, Frequency> entry : sts.entrySet()) {
            Iterator<Comparable<?>> iterator = entry.getValue().valuesIterator();
            long minCount = Long.MAX_VALUE;
            long maxCount = 0;
            int valueCount = 0;
            while (iterator.hasNext()) {
                final Comparable<?> value = iterator.next();
                valueCount++;
                final long count = entry.getValue().getCount(value);
                if (count > maxCount) {
                    maxCount = count;
                }
                if (count < minCount) {
                    minCount = count;
                }
            }
            System.out.println(entry.getKey() + "\t" + entry.getValue().getSumFreq() + "\t" + valueCount + "\t" + minCount + "\t" + maxCount);
        }
    }

    @Parameterized.Parameter
    public People people;

    @Value("http://localhost:${local.service.port}")
    private String url;

    @Test
    public void testHelloWorld() {
        final TestRestTemplate testRestTemplate = new TestRestTemplate();
        ResponseEntity<? extends String> responseEntity = testRestTemplate.postForEntity(instant.url + "/format", people.getName(), String.class);
        String stringRes = responseEntity.getBody();
    }

    @Parameterized.Parameters
    private static List<Object[]> data() throws IOException {
        List<Object[]> dataToBeReturned = new ArrayList<Object[]>();
        try {
            URL inputJsonDir = HelloWorldE2ETest.class.getResource("/sampleData");
            if (inputJsonDir == null) {
                System.out.println("Input json directory does not exist.");
                System.exit(1);
            }
            File[] inputJsonFiles = new File(inputJsonDir.toURI()).listFiles();
            System.out.println("Creating test suite ...");
            for (File jsonFile : inputJsonFiles) {
                try{
                    BufferedReader br = new BufferedReader(new FileReader(jsonFile.getAbsolutePath()));
                    People people = objectMapper.readValue(br, People.class);
                    dataToBeReturned.add(new Object[] { people });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return dataToBeReturned;
    }
}
