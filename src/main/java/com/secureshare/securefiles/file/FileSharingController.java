package com.secureshare.securefiles.file;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.secureshare.securefiles.file.FileEntity;
import com.secureshare.securefiles.file.SharedFile;
import com.secureshare.securefiles.file.FileSharingService;
import com.secureshare.securefiles.file.FileStorageService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/share")
@RequiredArgsConstructor
public class FileSharingController {

    private final FileSharingService sharingService;
    private final FileStorageService fileStorageService;

    @PostMapping("/{fileId}")
    public ResponseEntity<String> shareFile(
            @PathVariable Long fileId,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "60") long expiryMinutes
    ) {
        String token = sharingService.generateShareLink(fileId, password, expiryMinutes);
        return ResponseEntity.ok("http://localhost:5173/share/access/" + token);
    }

    @GetMapping("/access/{token}")
    public ResponseEntity<?> accessFile(
            @PathVariable String token,
            @RequestParam(required = false) String password
    ) {
        try {
            // Validate token format (simple check for UUID format)
            if (token.split("-").length != 5) {
                log.warn("Invalid token format: {}", token);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token format");
            }
            // Validate token and get shared file
            SharedFile shared = sharingService.getValidSharedFile(token, password)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid or expired token"));

            // Get file content
            FileEntity file = shared.getFile();
            byte[] content = fileStorageService.getFileContent(file);

            // Return file with proper headers
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            StringUtils.hasText(file.getContentType()) ?
                                    file.getContentType() :
                                    "application/octet-stream")) // Fallback content type
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + encodeFilename(file.getOriginalFilename()) + "\"")
                    .body(new ByteArrayResource(content));

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (Exception e) {
            log.error("File access error for token: {}", token, e);
            return ResponseEntity.internalServerError().body("File access error");
        }
    }
    private String encodeFilename(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
    }
}
