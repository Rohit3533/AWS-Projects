package com.devops.productservice.dto.common;

import java.time.LocalDateTime;

public class ResponseContext {
    private String correlationId;
    private String status;
    private String timestamp;

    public ResponseContext() {}

    public ResponseContext(String correlationId, String status) {
        this.correlationId = correlationId;
        this.status = status;
        this.timestamp = LocalDateTime.now().toString();
    }

    public static ResponseContext success(String correlationId) {
        return new ResponseContext(correlationId, "SUCCESS");
    }

    public static ResponseContext failure(String correlationId) {
        return new ResponseContext(correlationId, "FAILURE");
    }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
