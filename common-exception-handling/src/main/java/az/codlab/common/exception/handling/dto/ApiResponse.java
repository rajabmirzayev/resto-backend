package az.codlab.common.exception.handling.dto;

import az.codlab.common.exception.handling.error.CommonErrorCode;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import static az.codlab.common.exception.handling.utils.Constant.DEFAULT_SUCCESS_MESSAGE;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    boolean success;
    String message;
    String errorCode;
    T data;

    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .success(true)
                .message(DEFAULT_SUCCESS_MESSAGE)
                .errorCode(null)
                .data(null)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(DEFAULT_SUCCESS_MESSAGE)
                .errorCode(null)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .errorCode(null)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, CommonErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode.getCode())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, CommonErrorCode errorCode, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode.getCode())
                .data(data)
                .build();
    }

}
