package com.secureshare.securefiles.file;

import com.secureshare.securefiles.dto.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.secureshare.securefiles.user.User;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileSharingService {

    private final SharedFileRepository sharedFileRepository;
    private final FileRepository fileRepository;
    private final PasswordEncoder passwordEncoder;
    private final ShareTokenService tokenService;

    public ShareResponseDTO createShare(Long fileId, String rawPassword, long expiryMinutes, User user) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        String token = tokenService.generateSecureToken();
        String hashedPassword = StringUtils.hasText(rawPassword) ? passwordEncoder.encode(rawPassword) : null;

        Instant expiryInstant = Instant.now().plusSeconds(expiryMinutes * 60);

        SharedFile sharedFile = SharedFile.builder()
                .file(file)
                .token(token)
                .expiry(expiryInstant)
                .password(hashedPassword)  // This will be null if no password was provided
                .sharedBy(user)
                .active(true)
                .build();

        sharedFileRepository.save(sharedFile);

        return ShareResponseDTO.builder()
                .token(token)
                .expiry(expiryInstant)
                .hasPassword(hashedPassword != null)  // This correctly indicates if password exists
                .shareUrl("http://localhost:5173/share/access/" + token)
                .qrCodeUrl("http://localhost:8080/api/v1/share/qr/" + token)
                .build();
    }

    public Optional<SharedFile> getValidSharedFile(String token, String rawPassword) {
        log.debug("Looking up token: {}", token);

        Optional<SharedFile> sharedOpt = sharedFileRepository.findByToken(token);
        if (sharedOpt.isEmpty()) {
            log.warn("Token not found in database: {}", token);
            return Optional.empty();
        }

        SharedFile shared = sharedOpt.get();
        log.debug("Found shared file record: {}", shared.getId());

        if (!shared.isActive()) {
            log.warn("Share is not active for token: {}", token);
            return Optional.empty();
        }

        if (shared.isExpired()) {
            log.warn("Share is expired for token: {}. Expiry: {}", token, shared.getExpiry());
            return Optional.empty();
        }

        if (shared.getPassword() != null) {
            if (rawPassword == null || !passwordEncoder.matches(rawPassword, shared.getPassword())) {
                log.warn("Password validation failed for token: {}", token);
                return Optional.empty();
            }
        }

        return Optional.of(shared);
    }

    public Page<SharedFileDTO> getUserSharedFiles(User user, Pageable pageable) {
        return sharedFileRepository.findBySharedBy(user, pageable)
                .map(SharedFileDTO::fromEntity);
    }

    public Optional<ShareInfoDTO> getShareInfo(String token) {
        if (!tokenService.isValidTokenFormat(token)) {
            return Optional.empty();
        }

        return sharedFileRepository.findByToken(token)
                .filter(SharedFile::isActive)
                .filter(s -> !s.isExpired())
                .map(share -> new ShareInfoDTO(
                        share.getFile().getOriginalFilename(),
                        share.hasPassword(),
                        share.getExpiry()
                ));
    }

    public void revokeShare(String token, User user) {
        SharedFile share = sharedFileRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Share not found"
                ));

        if (!share.getSharedBy().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not owned by user");
        }

        try {
            share.setActive(false);
            sharedFileRepository.save(share); // Soft delete
        } catch (DataAccessException e) {
            log.error("Failed to revoke share: {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Database error during revocation"
            );
        }
    }

}