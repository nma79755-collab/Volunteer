package com.cyh.exception;

import com.cyh.Res.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global Exception Handler
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(java.util.stream.Collectors.joining("；"));
        return ResponseEntity.badRequest().body(
                ApiResponse.error(400, message)
        );
    }
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ResponseEntity.badRequest().body(
                ApiResponse.error(e.getCode(), e.getMessage())
        );
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("System exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "System error, please try again later")
        );
    }
}
