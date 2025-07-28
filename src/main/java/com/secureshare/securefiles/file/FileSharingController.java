package com.secureshare.securefiles.file;

import com.secureshare.securefiles.dto.*;
import com.secureshare.securefiles.util.QrCodeUtil;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import com.secureshare.securefiles.user.User;
import jakarta.validation.Valid;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/v1/share")
@RequiredArgsConstructor
public class FileSharingController {

    private final FileSharingService sharingService;
    private final FileStorageService fileStorageService;
    private final FileRepository fileRepository;
    private final ShareTokenService tokenService;
    private final SharedFileRepository sharedFileRepository;

    @PostMapping("/{fileId}")
    @RateLimiter(name = "fileSharing", fallbackMethod = "shareRateLimitExceeded")
    @PreAuthorize("hasAuthority('file:share')")
    public ResponseEntity<ShareResponseDTO> shareFile(
            @PathVariable Long fileId,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "1440") long expiryMinutes,
            @AuthenticationPrincipal User user) {

        try {
            ShareResponseDTO response = sharingService.createShare(
                    fileId,
                    password,
                    expiryMinutes,
                    user
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error generating share link for file {}", fileId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating share link");
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('file:share')")
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
    @PreAuthorize("hasAuthority('file:share')")
    public ResponseEntity<Void> revokeShare(
            @PathVariable String token,
            @AuthenticationPrincipal User user) {
        sharingService.revokeShare(token, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/access/{token}")
    public ResponseEntity<?> accessFile(
            @PathVariable String token,
            @RequestParam(required = false) String password) {

        try {
            // Validate token format
            if (!tokenService.isValidTokenFormat(token)) {
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
            throw e;
        } catch (Exception e) {
            log.error("File access error for token: {}", token, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error accessing file");
        }
    }

    @GetMapping("/qr/{token}")
    public ResponseEntity<ByteArrayResource> getQrCodeForToken(
            @PathVariable String token,
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "300") int height) {
        try {
            if (!tokenService.isValidTokenFormat(token)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token format");
            }

            String url = "http://localhost:5173/share/access/" + token;
            String base64 = QrCodeUtil.generateBase64QrCode(url, width, height);
            byte[] decoded = Base64.getDecoder().decode(base64);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                    .body(new ByteArrayResource(decoded));
        } catch (Exception e) {
            log.error("QR generation failed for token: {}", token, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error generating QR code");
        }
    }

    @GetMapping("/info/{token}")
    public ResponseEntity<ShareInfoDTO> getShareInfo(@PathVariable String token) {
        Optional<ShareInfoDTO> info = sharingService.getShareInfo(token);

        // Validate token format first
        if (!tokenService.isValidTokenFormat(token)) {
            log.warn("Invalid token format: {}", token);
            return ResponseEntity.badRequest().build();
        }

        Optional<SharedFile> sharedFile = sharedFileRepository.findByToken(token);

        if (sharedFile.isEmpty()) {
            log.info("Share not found for token: {}", token);
            return ResponseEntity.notFound().build();
        }

        SharedFile share = sharedFile.get();

        // Check if share is active
        if (!share.isActive()) {
            log.info("Attempt to access revoked share with token: {}", token);
            return ResponseEntity.status(HttpStatus.GONE).build(); // 410 Gone
        }

        // Check if share is expired
        if (share.isExpired()) {
            log.info("Attempt to access expired share with token: {}", token);
            return ResponseEntity.status(HttpStatus.GONE).build(); // 410 Gone
        }

        // Log successful info request
        log.debug("Share info retrieved for token: {}", token);

        return info.map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.info("Invalid or expired share token: {}", token);
                    return ResponseEntity.status(HttpStatus.GONE).build();
                });
    }

    public ResponseEntity<String> shareRateLimitExceeded(Exception ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body("Share creation rate limit exceeded. Please try again later.");
    }

    private String encodeFilename(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
    }
}