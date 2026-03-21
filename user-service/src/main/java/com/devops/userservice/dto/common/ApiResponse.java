package com.devops.userservice.dto.common;

public class ApiResponse<T> {
    private ResponseContext responseContext;
    private T data;
    private ErrorDetail error;

    public ApiResponse() {}

    public static <T> ApiResponse<T> success(String correlationId, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.responseContext = ResponseContext.success(correlationId);
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> failure(String correlationId, String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.responseContext = ResponseContext.failure(correlationId);
        response.error = new ErrorDetail(code, message);
        return response;
    }

    public ResponseContext getResponseContext() { return responseContext; }
    public void setResponseContext(ResponseContext responseContext) { this.responseContext = responseContext; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public ErrorDetail getError() { return error; }
    public void setError(ErrorDetail error) { this.error = error; }
}
