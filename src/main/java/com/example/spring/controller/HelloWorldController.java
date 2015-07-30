package com.example.spring.controller;

import com.example.spring.model.Greeting;
import com.example.spring.model.People;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.codahale.metrics.annotation.Timed;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by pzhong1 on 1/13/15.
 */

@Controller
public class HelloWorldController implements ServletContextInitializer {
    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();
    final static Logger logger = Logger.getLogger(HelloWorldController.class);

    @Value("${example.test.url}")
    private String testUrl;

    @RequestMapping(value = "/greeting", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Timed
    @Secured("ROLE_ADMIN")
    public Greeting greeting(@RequestParam(value="name", defaultValue="World") String name) {
        return new Greeting(counter.incrementAndGet(),
                String.format(template, name));
    }

    @RequestMapping(value = "/sayhello", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Timed
    public Greeting sayHello(@RequestParam(value="name", defaultValue="World") String name) {
        return new Greeting(counter.incrementAndGet(),
                String.format(template, name));
    }

    @RequestMapping(value = "/format", method = RequestMethod.POST)
    @Timed
    public @ResponseBody People format(@RequestBody final People people) {
        return people;
    }

    @Scheduled(cron = "${example.test.cron}")
    public void cronJob() {
        System.out.println("say hello!");
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        //System.out.println(test)
    }

    String getFirstLineFromUrl(String testUrl) {
        try {
            URL url = new URL(testUrl);
            List<String> lines = IOUtils.readLines(url.openStream());
            String firstLine = lines.get(0).toString();
            if (firstLine != null) {
                logger.info("New first line: " + firstLine + " from " + testUrl);
                return firstLine;
            }
        } catch (Exception logged) {
            logger.warn("Failed to get first line from " + testUrl, logged);
        }
        return null;
    }
}
