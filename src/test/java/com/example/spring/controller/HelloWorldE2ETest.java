package com.example.spring.controller;

import com.example.spring.WebAppInitializer;
import com.example.spring.model.People;
import com.example.spring.utils.DataExpectedBuilder;
import org.apache.commons.math3.stat.Frequency;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.Parameter;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

/**
 * Created by benbendaisy on 6/30/15.
 */
@RunWith(Parameterized.class)
@SpringApplicationConfiguration(classes = WebAppInitializer.class)
@WebAppConfiguration
@IntegrationTest({
        "spring.profiles.active:test"
})
public class HelloWorldE2ETest {
    private static final List<String> blackList = Arrays.asList();
    private static final List<String> whiteList = Arrays.asList();
    private static final List<String> blackListedFields = Arrays.asList();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static Map<String, Frequency> fieldStats = new TreeMap<String, Frequency>();
    private static People expectedPeople;


    private static TestContextManager testContextManager;
    private static HelloWorldE2ETest instant;

    @BeforeClass
    public static void init() throws IOException {
        InputStream resource = null;
        try {
            testContextManager = new TestContextManager(HelloWorldE2ETest.class);
            resource = HelloWorldE2ETest.class.getClassLoader().getResourceAsStream("sampleData/peopleSample.json");
            expectedPeople = objectMapper.readValue(resource, People.class);
            instant = new HelloWorldE2ETest();
            // this starts the server and binds fields with @Value annotation
            testContextManager.prepareTestInstance(instant);
            System.out.println(instant.url);
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

    @Value("http://localhost:${local.server.port}")
    private String url;

    @Parameter
    public People people;

    @Parameters
    public static Collection<Object[]> peopleData() throws IOException {
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
                    People tp = objectMapper.readValue(br, People.class);
                    dataToBeReturned.add(new Object[] { tp });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return dataToBeReturned;
    }

    @Test
    public void testHelloWorld() throws Exception {
        DataExpectedBuilder expectedDataBuilder = DataExpectedBuilder.newBuilder().from(people);
        assumeTrue(!blackList.contains(expectedDataBuilder.getPeopleId()));
        String whiteListedPeopleId = null;
        if (null != whiteList && !whiteList.isEmpty()) {
            assumeTrue(whiteList.contains(expectedDataBuilder.getPeopleId()));
            whiteListedPeopleId = expectedDataBuilder.getPeopleId();
        }

        final TestRestTemplate testRestTemplate = new TestRestTemplate();
        ResponseEntity<People> responseEntity = testRestTemplate.postForEntity(instant.url + "/format", people, People.class);
        People newPeople = responseEntity.getBody();
        DataExpectedBuilder actualDataBuilder = DataExpectedBuilder.newBuilder().from(newPeople);

        accumulateStats(actualDataBuilder.getRawData(), fieldStats);

        if (null != whiteListedPeopleId) {
            System.out.println("nothing need to be verified");
        }

        int compareCode = actualDataBuilder.compareTo(expectedDataBuilder);
        if (compareCode > 0) {
            System.out.println("People has more fields than expected for " + expectedDataBuilder.getPeopleId() + ": " + expectedDataBuilder.diff(actualDataBuilder).getRawData());
            return;
        } else if (compareCode < 0) {
            if (diffOnlyByBlackListedFields(actualDataBuilder, expectedDataBuilder)) {
                System.out.println(expectedDataBuilder.getPeopleId() + " test assertion ignored for blacklisted fields: " + expectedDataBuilder.diff(actualDataBuilder).getRawData());
                System.out.println();
                return;
            }
        }
        assertEquals(expectedDataBuilder.getPeopleId() + ": " + expectedDataBuilder.diff(actualDataBuilder).getRawData(),
                expectedDataBuilder.getRawData(), actualDataBuilder.getRawData());
    }

    private void accumulateStats(Map<String, List<String>> data, Map<String, Frequency> stats) {
        for (Map.Entry<String, List<String>> entry : data.entrySet()) {
            Frequency frequency = stats.get(entry.getKey());
            if (null == frequency) {
                frequency = new Frequency();
                stats.put(entry.getKey(), frequency);
            }

            for (String value : entry.getValue()) {
                frequency.addValue(value);
            }
        }
    }

    private boolean diffOnlyByBlackListedFields(DataExpectedBuilder actualDataBuilder, DataExpectedBuilder expectedDataBuilder) {
        DataExpectedBuilder diffDataBuilder = expectedDataBuilder.diff(actualDataBuilder);
        for (String key : diffDataBuilder.getRawData().keySet()) {
            if (key.startsWith("+>")) {
                continue;
            } else if (key.startsWith("-<")) {
                key = key.substring(2);
            } else {
                boolean allNew = true;
                for (String value : diffDataBuilder.getRawData().get(key)) {
                    if (!value.startsWith("+>")) {
                        allNew = false;
                    }
                }
                if (allNew) {
                    continue;
                }
            }
            if (!blackListedFields.contains(key)) {
                return false;
            }
        }
        return true;
    }
}
