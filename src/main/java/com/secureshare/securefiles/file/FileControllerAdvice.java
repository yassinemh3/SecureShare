package com.secureshare.securefiles.file;

import com.secureshare.securefiles.dto.ErrorResponse;
import com.secureshare.securefiles.file.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import com.secureshare.securefiles.service.exception.CryptoException;

import java.time.Instant;

@Slf4j
@ControllerAdvice
public class FileControllerAdvice {

    // Handles when a file is not found (404)
    @ExceptionHandler(FileStorageService.FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFound(FileStorageService.FileNotFoundException e) {
        log.warn("File not found: {}", e.getMessage());
        return ResponseEntity.notFound().build(); // Returns HTTP 404
    }

    // Handles unauthorized access (403)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("Access denied", "FILE_ACCESS_DENIED", Instant.now())); // HTTP 403 + JSON body
    }

    // Handles encryption/decryption failures (500)
    @ExceptionHandler(CryptoException.class)
    public ResponseEntity<ErrorResponse> handleCryptoException(CryptoException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "CRYPTO_ERROR",
                        "File processing failed due to security error",
                        Instant.now()
                ));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException e) {
        log.warn("Security violation: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        e.getMessage(),
                        "UNAUTHORIZED_OPERATION",
                        Instant.now()
                ));
    }

    // (Optional) Catch-all for unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("An unexpected error occurred", "INTERNAL_ERROR", Instant.now()));

    }
}