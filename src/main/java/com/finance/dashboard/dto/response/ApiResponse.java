package com.finance.dashboard.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standardised JSON envelope returned by every endpoint.
 *
 * Success shape:
 * <pre>{@code
 * {
 *   "success": true,
 *   "message": "Expense created successfully",
 *   "data": { ... },
 *   "statusCode": 201,
 *   "timestamp": "2024-04-05T10:30:00"
 * }
 * }</pre>
 *
 * Error shape:
 * <pre>{@code
 * {
 *   "success": false,
 *   "message": "Expense not found with id: 99",
 *   "error": "NOT_FOUND",
 *   "statusCode": 404,
 *   "timestamp": "2024-04-05T10:30:00"
 * }
 * }</pre>
 *
 * {@code @JsonInclude(NON_NULL)} ensures null fields (e.g. {@code data} on errors,
 * {@code alert} when budget is not exceeded) are omitted from the output.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** Whether the request completed without errors. */
    private boolean success;

    /** Human-readable outcome message. */
    private String message;

    /** Response payload – null on error responses. */
    private T data;

    /** HTTP status code mirrored in the body for client convenience. */
    private Integer statusCode;

    /** Short error code (e.g. "NOT_FOUND", "FORBIDDEN") – null on success. */
    private String error;

    /** ISO-8601 timestamp of response generation. */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Optional budget alert message included when the user's monthly EXPENSE
     * spending exceeds the configured threshold. Null otherwise.
     */
    private String alert;

    // ── Static factory methods ────────────────────────────────────────────────

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(200)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operation successful");
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(201)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> successWithAlert(T data, String message, String alert) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(200)
                .alert(alert)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, int statusCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .statusCode(statusCode)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
