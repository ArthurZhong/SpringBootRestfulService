package com.example.spring.health;

import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.web.BasicErrorController;
import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pzhong1 on 1/15/15.
 */

@RestController
public class ErrorControllerHealthIndicator extends BasicErrorController implements HealthIndicator {
    final static Logger logger = Logger.getLogger(ErrorControllerHealthIndicator.class);

    @Value("${example.error.up.threshold:100}")
    private int upThreshold;

    @Value("${example.error.unknown.threshold:1000}")
    private int unknownThreshold;

    private final Map<String, Map<String, Integer>> statusToPathCounts = Maps.newConcurrentMap();
    private final Map<String, Integer> remoteIpCounts = Maps.newConcurrentMap();

    public ErrorControllerHealthIndicator() {
        super(new DefaultErrorAttributes());
    }

    @Override
    public ModelAndView errorHtml(HttpServletRequest request) {
        ModelAndView response = super.errorHtml(request);
        increment(response.getModel().get("status").toString(), response.getModel(), request);
        return response;
    }

    @Override
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> response = super.error(request);
        increment(response.getStatusCode().toString(), response.getBody(), request);
        return response;
    }

    private void increment(String status, Map<String, ?> response, HttpServletRequest request) {
        try {
            String requestPath;
            if (response.containsKey("path")) {
                requestPath = response.get("path").toString();
            } else if (null != request.getAttribute("javax.servlet.forward.request_uri")) {
                requestPath = request.getAttribute("javax.servlet.forward.request_uri").toString();
            } else {
                requestPath = request.getRequestURI();
            }
            Map<String, Integer> pathCounts = statusToPathCounts.get(status);
            if (null == pathCounts) {
                pathCounts = Maps.newConcurrentMap();
                statusToPathCounts.put(status, pathCounts);
            }
            Integer cnt = pathCounts.get(requestPath);
            if (null == cnt) {
                cnt = 1;
            } else {
                cnt++;
            }
            pathCounts.put(requestPath, cnt);

            cnt = remoteIpCounts.get(request.getRemoteAddr());
            if (null == cnt) {
                cnt = 1;
            } else {
                cnt++;
            }
            remoteIpCounts.put(request.getRemoteAddr(), cnt);
        } catch (Exception logged) {
            logger.warn("Failed to handle error request for " + request, logged);
        }
    }

    @Override
    public Health health() {
        final Health.Builder builder = new Health.Builder();
        int totalCnt = 0;
        for (Map.Entry<String, Map<String, Integer>> entry : statusToPathCounts.entrySet()) {
            builder.withDetail("status." + entry.getKey(), entry.getValue());
            for (Integer cnt : entry.getValue().values()) {
                totalCnt += cnt;
            }
        }
        for (Map.Entry<String, Integer> entry : remoteIpCounts.entrySet()) {
            builder.withDetail("host." + entry.getKey(), entry.getValue());
        }
        builder.withDetail("total", totalCnt);
        if (totalCnt < upThreshold) {
            builder.up();
        } else if (totalCnt < unknownThreshold) {
            builder.unknown();
        } else {
            builder.down();
        }
        return builder.build();
    }
}
