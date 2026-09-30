package com.tcc.triAI_api_java.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(String status, String message, T data, Map<String, ?> errors) {

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>("success", message, data, null);
    }

    public static ApiResponse<Void> error(Map<String, ?> errors, String message) {
        return new ApiResponse<>("error", message, null, errors);
    }
}
