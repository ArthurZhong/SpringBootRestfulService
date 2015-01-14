package com.example.spring;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;

import java.util.concurrent.TimeUnit;

/**
 * Created by pzhong1 on 1/13/15.
 */
public class MetricsConfig extends MetricsConfigurerAdapter{
    @Override
    public void configureReporters(MetricRegistry metricRegistry) {
        Slf4jReporter.forRegistry(metricRegistry).build().start(1, TimeUnit.MINUTES);
    }
}
