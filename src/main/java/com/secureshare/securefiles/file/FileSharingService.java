package com.secureshare.securefiles.file;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.secureshare.securefiles.dto.SharedFileDTO;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.secureshare.securefiles.user.User;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileSharingService {

    private final SharedFileRepository sharedFileRepository;
    private final FileRepository fileRepository;
    private final PasswordEncoder passwordEncoder;

    public String generateShareLink(Long fileId, String rawPassword, long expiryMinutes) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        String token = UUID.randomUUID().toString();
        String hashedPassword = rawPassword != null ? passwordEncoder.encode(rawPassword) : null;

        Instant expiryInstant = Instant.now().plusSeconds(expiryMinutes * 60);

        SharedFile sharedFile = SharedFile.builder()
                .file(file)
                .token(token)
                .expiry(expiryInstant)
                .password(hashedPassword)
                .build();

        sharedFileRepository.save(sharedFile);
        return token;
    }

    public Optional<SharedFile> getValidSharedFile(String token, String rawPassword) {
        return sharedFileRepository.findByToken(token)
                .filter(s -> s.getExpiry().isAfter(Instant.now()))
                .filter(s -> {
                    if (s.getPassword() == null) return true;
                    return rawPassword != null && passwordEncoder.matches(rawPassword, s.getPassword());
                });
    }

    public Page<SharedFileDTO> getUserSharedFiles(User user, Pageable pageable) {
        return sharedFileRepository.findBySharedBy(user, pageable)
                .map(this::toDto);
    }

    public void revokeShare(String token, User user) {
        SharedFile share = sharedFileRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Share not found"
                ));

        // Optional: Add ownership check only if sharedBy is set
        if (share.getSharedBy() != null && !share.getSharedBy().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not owned by user");
        }

        try {
            sharedFileRepository.delete(share);
        } catch (DataAccessException e) {
            log.error("Failed to delete share: {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Database error during deletion"
            );
        }
    }

    private Long tryParseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private SharedFileDTO toDto(SharedFile sharedFile) {
        return SharedFileDTO.builder()
                .id(sharedFile.getId())
                .token(sharedFile.getToken())
                .expiryDate(sharedFile.getExpiry())
                .hasPassword(sharedFile.hasPassword())
                .filename(sharedFile.getFile().getOriginalFilename())
                .shareUrl("http://localhost:5173/share/access/" + sharedFile.getToken())
                .createdAt(sharedFile.getCreatedAt())
                .build();
    }
}