package com.nocountry.authservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "authservice-api",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @GetMapping("/health/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "authservice-api");
        body.put("timestamp", OffsetDateTime.now().toString());

        try (Connection connection = dataSource.getConnection()) {
            boolean dbUp = connection.isValid(2);
            body.put("database", dbUp ? "UP" : "DOWN");
            body.put("status", dbUp ? "UP" : "DOWN");
            return ResponseEntity.status(dbUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
        } catch (Exception e) {
            body.put("database", "DOWN");
            body.put("status", "DOWN");
            body.put("error", "database_unavailable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }

    @GetMapping("/health/liveness")
    public ResponseEntity<Map<String, Object>> liveness() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "authservice-api",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }
}
