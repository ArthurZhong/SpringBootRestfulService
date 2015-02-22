package com.example.spring.controller;

import com.example.spring.WebAppInitializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.OutputCapture;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.StringWriter;
import java.io.Writer;

import static org.junit.Assert.assertTrue;

/**
 * Created by pzhong1 on 2/22/15.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = WebAppInitializer.class)
@WebIntegrationTest
public class HelloWorldControllerAspectTest {
    @Autowired
    HelloWorldController helloWorldController;

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    private String profiles;

//    @Before
//    public void init() {
//        this.profiles = System.getProperty("com.example.spring.aop.name");
//    }
//
//    @After
//    public void after() {
//        if (this.profiles != null) {
//            System.setProperty("com.example.spring.aop.name", this.profiles);
//        }
//        else {
//            System.clearProperty("com.example.spring.aop.name");
//        }
//    }

    @Test
    public void testAopArround() throws Exception {
        helloWorldController.sayHello("Phil");
        String output = this.outputCapture.toString();
        System.out.println(output);
        assertTrue("Wrong output: " + output, output.contains("apiResponseTime"));
        assertTrue("Wrong output: " + output, output.contains("Phil"));
    }
}
