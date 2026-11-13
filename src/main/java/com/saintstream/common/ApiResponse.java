package com.saintstream.common;

/**
 * Bütün endpointlərin ortaq cavab formatı: { success, message, data }.
 * "record" - sahələri dəyişməyən sadə data class-dır (getter-lər avtomatik yaranır).
 */
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
