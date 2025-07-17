package com.secureshare.securefiles.file;

import com.secureshare.securefiles.util.QrCodeUtil;
import com.secureshare.securefiles.dto.SharedFileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.secureshare.securefiles.user.User;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import com.secureshare.securefiles.file.SharedFileRepository;

@Slf4j
@RestController
@RequestMapping("/api/v1/share")
@RequiredArgsConstructor
public class FileSharingController {

    private final FileSharingService sharingService;
    private final FileStorageService fileStorageService;
    private final FileRepository fileRepository;
    private final SharedFileRepository sharedFileRepository;

    @PostMapping("/{fileId}")
    public ResponseEntity<String> shareFile(
            @PathVariable Long fileId,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "1440") long expiryMinutes // Default 24 hours
    ) {
        try {
            // Validate expiry time
            if (expiryMinutes <= 0) {
                throw new IllegalArgumentException("Expiry time must be positive");
            }

            // Verify file exists
            if (!fileRepository.existsById(fileId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }

            String token = sharingService.generateShareLink(fileId, password, expiryMinutes);
            String shareUrl = "http://localhost:5173/share/access/" + token;
            return ResponseEntity.ok(shareUrl);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error generating share link for file {}", fileId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating share link");
        }
    }

    @GetMapping
    public ResponseEntity<Page<SharedFileDTO>> getUserSharedFiles(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {
        try {
            Page<SharedFileDTO> shares = sharingService.getUserSharedFiles(user, pageable);
            return ResponseEntity.ok(shares);
        } catch (Exception e) {
            log.error("Error fetching shared files for user {}", user.getId(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching shares");
        }
    }

    @DeleteMapping("/{token}")
    public ResponseEntity<Void> revokeShare(
            @PathVariable String token,
            @AuthenticationPrincipal User user) {
        sharingService.revokeShare(token, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/access/{token}")
    public ResponseEntity<?> accessFile(
            @PathVariable String token,
            @RequestParam(required = false) String password
    ) {
        try {
            // Validate token format (basic UUID check)
            if (!token.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                log.warn("Invalid token format: {}", token);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token format");
            }

            SharedFile shared = sharingService.getValidSharedFile(token, password)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid or expired token"));

            FileEntity file = shared.getFile();
            byte[] content = fileStorageService.getFileContent(file);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            StringUtils.hasText(file.getContentType()) ?
                                    file.getContentType() :
                                    MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + encodeFilename(file.getOriginalFilename()) + "\"")
                    .body(new ByteArrayResource(content));

        } catch (ResponseStatusException e) {
            throw e; // Re-throw existing status exceptions
        } catch (Exception e) {
            log.error("File access error for token: {}", token, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error accessing file");
        }
    }

    @GetMapping("/qr/{token}")
    public ResponseEntity<ByteArrayResource> getQrCodeForToken(
            @PathVariable String token,
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "300") int height
    ) {
        try {
            // Basic token validation
            if (token.length() != 36) { // UUID length
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token format");
            }

            String url = "http://localhost:5173/share/access/" + token;
            String base64 = QrCodeUtil.generateBase64QrCode(url, width, height);
            byte[] decoded = Base64.getDecoder().decode(base64);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS)) // Cache QR codes
                    .body(new ByteArrayResource(decoded));
        } catch (Exception e) {
            log.error("QR generation failed for token: {}", token, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error generating QR code");
        }
    }

    private String encodeFilename(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
    }
}