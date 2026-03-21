package com.devops.userservice.dto.common;

public class ApiRequest<T> {
    private RequestContext requestContext;
    private T data;

    public ApiRequest() {}

    public ApiRequest(RequestContext requestContext, T data) {
        this.requestContext = requestContext;
        this.data = data;
    }

    public RequestContext getRequestContext() { return requestContext; }
    public void setRequestContext(RequestContext requestContext) { this.requestContext = requestContext; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public String getCorrelationId() {
        return requestContext != null ? requestContext.getCorrelationId() : null;
    }
}
