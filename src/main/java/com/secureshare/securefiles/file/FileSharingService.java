package com.secureshare.securefiles.file;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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
}