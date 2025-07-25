package com.secureshare.securefiles.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FileValidator {

    // Configuration - externalize to application.properties
    @Value("${app.file.max-size:52428800}") // 50MB default
    private long maxFileSize;

    @Value("${app.file.max-filename-length:255}")
    private int maxFilenameLength;

    // Comprehensive MIME type validation
    private static final Map<String, Set<String>> ALLOWED_MIME_TYPES = Map.of(
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "png", Set.of("image/png"),
            "pdf", Set.of("application/pdf"),
            "txt", Set.of("text/plain"),
            "doc", Set.of("application/msword"),
            "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "xls", Set.of("application/vnd.ms-excel"),
            "xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            "enc", Set.of("application/octet-stream", "application/x-encrypted", "application/encrypted")
    );

    // Magic number validation for file type verification
    private static final Map<String, byte[][]> FILE_SIGNATURES = Map.of(
            "pdf", new byte[][]{{0x25, 0x50, 0x44, 0x46}}, // %PDF
            "jpg", new byte[][]{{(byte)0xFF, (byte)0xD8, (byte)0xFF}}, // JPEG
            "png", new byte[][]{{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}}, // PNG
            "txt", new byte[][]{}, // Text files don't have reliable magic numbers
            "enc", new byte[][]{}
    );

    // Malicious filename patterns
    private static final Pattern MALICIOUS_FILENAME_PATTERN = Pattern.compile(
            ".*(\\.\\.|/|\\\\|<|>|:|\\*|\\?|\"|\\||%00|%2e|%2f|%5c).*",
            Pattern.CASE_INSENSITIVE
    );

    // Suspicious executable extensions
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "com", "pif", "scr", "vbs", "js", "jar",
            "app", "deb", "pkg", "dmg", "sh", "ps1", "msi", "dll"
    );

    // Known malicious file hashes (simplified example)
    private static final Set<String> MALICIOUS_HASHES = Set.of(
            // Add known malicious file hashes here
            "d41d8cd98f00b204e9800998ecf8427e" // Example MD5 hash
    );

    public void validate(MultipartFile file) throws FileValidationException {
        log.debug("Starting validation for file: {}", file.getOriginalFilename());

        try {
            validateBasicProperties(file);
            validateFilename(file.getOriginalFilename());
            validateFileExtension(file.getOriginalFilename());
            validateMimeType(file);
            validateFileSignature(file);
            validateFileContent(file);
            validateFileHash(file);

            log.info("File validation successful: {}", file.getOriginalFilename());

        } catch (Exception e) {
            log.warn("File validation failed for {}: {}", file.getOriginalFilename(), e.getMessage());
            throw e;
        }
    }

    private void validateBasicProperties(MultipartFile file) throws FileValidationException {
        if (file == null) {
            throw new FileValidationException("File cannot be null", "NULL_FILE");
        }

        if (file.isEmpty()) {
            throw new FileValidationException("File cannot be empty", "EMPTY_FILE");
        }

        if (file.getSize() > maxFileSize) {
            throw new FileValidationException(
                    String.format("File size (%d bytes) exceeds maximum limit (%d bytes)",
                            file.getSize(), maxFileSize),
                    "FILE_TOO_LARGE"
            );
        }

        if (file.getSize() == 0) {
            throw new FileValidationException("File has zero bytes", "ZERO_SIZE_FILE");
        }
    }

    private void validateFilename(String filename) throws FileValidationException {
        if (filename == null || filename.trim().isEmpty()) {
            throw new FileValidationException("Filename cannot be null or empty", "INVALID_FILENAME");
        }

        if (filename.length() > maxFilenameLength) {
            throw new FileValidationException(
                    String.format("Filename too long (%d chars), maximum allowed: %d",
                            filename.length(), maxFilenameLength),
                    "FILENAME_TOO_LONG"
            );
        }

        // Sanitize and check for path traversal
        String sanitizedFilename = Paths.get(filename).getFileName().toString();
        if (!sanitizedFilename.equals(filename)) {
            throw new FileValidationException("Filename contains invalid path characters", "INVALID_PATH");
        }

        // Check for malicious patterns
        if (MALICIOUS_FILENAME_PATTERN.matcher(filename).matches()) {
            throw new FileValidationException("Filename contains prohibited characters", "MALICIOUS_FILENAME");
        }

        // Check for reserved Windows filenames
        String[] reservedNames = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3",
                "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
                "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6",
                "LPT7", "LPT8", "LPT9"};

        String baseFilename = filename.contains(".") ?
                filename.substring(0, filename.lastIndexOf(".")) : filename;

        for (String reserved : reservedNames) {
            if (reserved.equalsIgnoreCase(baseFilename)) {
                throw new FileValidationException("Filename uses reserved system name", "RESERVED_FILENAME");
            }
        }
    }

    private void validateFileExtension(String filename) throws FileValidationException {
        if (!filename.contains(".")) {
            throw new FileValidationException("File must have an extension", "NO_EXTENSION");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        if (extension.isEmpty()) {
            throw new FileValidationException("File extension cannot be empty", "EMPTY_EXTENSION");
        }

        // Check for dangerous extensions
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            throw new FileValidationException("File extension is not allowed for security reasons", "DANGEROUS_EXTENSION");
        }

        // Check if extension is in allowed list
        if (!ALLOWED_MIME_TYPES.containsKey(extension)) {
            throw new FileValidationException(
                    String.format("File extension '%s' is not allowed. Allowed extensions: %s",
                            extension, ALLOWED_MIME_TYPES.keySet()),
                    "EXTENSION_NOT_ALLOWED"
            );
        }
    }

    private void validateMimeType(MultipartFile file) throws FileValidationException {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        if (contentType == null || contentType.trim().isEmpty()) {
            throw new FileValidationException("File content type cannot be determined", "UNKNOWN_CONTENT_TYPE");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        Set<String> allowedMimeTypes = ALLOWED_MIME_TYPES.get(extension);

        if (allowedMimeTypes != null && !allowedMimeTypes.contains(contentType)) {
            throw new FileValidationException(
                    String.format("Content type '%s' doesn't match file extension '%s'",
                            contentType, extension),
                    "MIME_TYPE_MISMATCH"
            );
        }

        // Additional MIME type security checks
        if (contentType.contains("script") || contentType.contains("executable")) {
            throw new FileValidationException("Executable content type not allowed", "EXECUTABLE_CONTENT");
        }
    }

    private void validateFileSignature(MultipartFile file) throws FileValidationException {
        try {
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length < 8) {
                return; // Skip signature validation for very small files
            }

            String filename = file.getOriginalFilename();
            String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

            byte[][] signatures = FILE_SIGNATURES.get(extension);
            if (signatures == null || signatures.length == 0) {
                return; // No signature validation for this file type
            }

            boolean signatureMatches = false;
            for (byte[] signature : signatures) {
                if (matchesSignature(fileBytes, signature)) {
                    signatureMatches = true;
                    break;
                }
            }

            if (!signatureMatches) {
                throw new FileValidationException(
                        String.format("File signature doesn't match expected format for %s files", extension),
                        "INVALID_FILE_SIGNATURE"
                );
            }

        } catch (IOException e) {
            throw new FileValidationException("Cannot read file for signature validation", "FILE_READ_ERROR");
        }
    }

    private boolean matchesSignature(byte[] fileBytes, byte[] signature) {
        if (fileBytes.length < signature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            if (fileBytes[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private void validateFileContent(MultipartFile file) throws FileValidationException {
        try {

            String filename = file.getOriginalFilename();
            String extension = getFileExtension(filename);

            // Skip content validation for encrypted files
            if ("enc".equals(extension)) {
                return;
            }

            byte[] content = file.getBytes();

            // Check for embedded executables or scripts
            String contentStr = new String(content).toLowerCase();

            // Detect potential script injections
            String[] suspiciousPatterns = {
                    "<script", "javascript:", "vbscript:", "onload=", "onerror=",
                    "<?php", "<%", "exec(", "system(", "shell_exec",
                    "rm -rf", "format c:", "del /", "rundll32"
            };

            for (String pattern : suspiciousPatterns) {
                if (contentStr.contains(pattern)) {
                    throw new FileValidationException(
                            "File contains potentially malicious content",
                            "MALICIOUS_CONTENT"
                    );
                }
            }

            // Check for excessive null bytes (potential for hiding content)
            int nullByteCount = 0;
            for (byte b : content) {
                if (b == 0) nullByteCount++;
            }

            if (nullByteCount > content.length * 0.3) { // More than 30% null bytes
                throw new FileValidationException(
                        "File contains suspicious amount of null bytes",
                        "SUSPICIOUS_CONTENT"
                );
            }

        } catch (IOException e) {
            throw new FileValidationException("Cannot read file content for validation", "CONTENT_READ_ERROR");
        }
    }

    private void validateFileHash(MultipartFile file) throws FileValidationException {
        try {
            byte[] content = file.getBytes();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(content);

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            String fileHash = sb.toString();

            if (MALICIOUS_HASHES.contains(fileHash)) {
                throw new FileValidationException(
                        "File matches known malicious file signature",
                        "KNOWN_MALWARE"
                );
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            log.warn("Could not calculate file hash for validation", e);
            // Don't fail validation if hash calculation fails
        }
    }

    // Custom exception class
    @Getter
    public static class FileValidationException extends RuntimeException {
        private final String errorCode;

        public FileValidationException(String message, String errorCode) {
            super(message);
            this.errorCode = errorCode;
        }

        public FileValidationException(String message, String errorCode, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }

    }

    // Utility method to get file extension
    public String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    // Method to check if file type is allowed
    public boolean isAllowedFileType(String extension) {
        return ALLOWED_MIME_TYPES.containsKey(extension.toLowerCase());
    }
}