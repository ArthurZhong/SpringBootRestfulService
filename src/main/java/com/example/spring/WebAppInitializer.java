package com.example.spring;

import com.mangofactory.swagger.plugin.EnableSwagger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by pzhong1 on 1/13/15.
 */

@Configuration
@ComponentScan
@EnableAutoConfiguration
@EnableConfigurationProperties
public class WebAppInitializer extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(WebAppInitializer.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(WebAppInitializer.class, args);
    }
}
