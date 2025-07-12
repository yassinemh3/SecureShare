package com.secureshare.securefiles.file;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

        SharedFile sharedFile = SharedFile.builder()
                .file(file)
                .token(token)
                .expiry(LocalDateTime.now().plusMinutes(expiryMinutes))
                .password(hashedPassword)
                .build();

        sharedFileRepository.save(sharedFile);
        return token;
    }

    public Optional<SharedFile> getValidSharedFile(String token, String rawPassword) {
        return sharedFileRepository.findByToken(token)
                .filter(s -> s.getExpiry().isAfter(LocalDateTime.now()))
                .filter(s -> {
                    if (s.getPassword() == null) return true;
                    return passwordEncoder.matches(rawPassword, s.getPassword());
                });
    }
}
