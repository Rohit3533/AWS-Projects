package com.devops.productservice.dto.common;

public class RequestContext {
    private String requesterId;
    private String correlationId;

    public RequestContext() {}

    public RequestContext(String requesterId, String correlationId) {
        this.requesterId = requesterId;
        this.correlationId = correlationId;
    }

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
