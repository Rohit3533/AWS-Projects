package com.devops.orderservice.dto.common;

public class RequestContext {
    private String requesterId;
    private String correlationId;

    public RequestContext() {}

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
