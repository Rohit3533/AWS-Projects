package com.devops.orderservice.dto.common;

public class ApiRequest<T> {
    private RequestContext requestContext;
    private T data;

    public ApiRequest() {}

    public RequestContext getRequestContext() { return requestContext; }
    public void setRequestContext(RequestContext requestContext) { this.requestContext = requestContext; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public String getCorrelationId() {
        return requestContext != null ? requestContext.getCorrelationId() : null;
    }
}
