package com.secureshare.securefiles.file;

import com.secureshare.securefiles.service.*;
import com.secureshare.securefiles.service.exception.*;
import com.secureshare.securefiles.user.User;
import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class FileStorageService {
    private final FileRepository fileRepository;
    private final EncryptionService encryptionService;
    private final FileValidator fileValidator;
    private final AuditService auditService;
    private final Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(uploadDir);
    }

    @Retryable(
            value = {IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    public FileEntity saveFile(MultipartFile file, User user) throws FileStorageException, ValidationException {

        try {
            String storedFilename = generateSecureFilename(file.getOriginalFilename());
            Path destination = uploadDir.resolve(storedFilename);

            byte[] encrypted = encryptionService.encrypt(file.getBytes());
            Files.write(destination, encrypted);

            FileEntity entity = buildFileEntity(file, storedFilename, user);
            FileEntity saved = fileRepository.save(entity);

            auditService.logUpload(user, saved);
            return saved;
        } catch (Exception e) {
            throw new FileStorageException("Failed to store file", e);
        }
    }

    public byte[] getFileContent(FileEntity file) throws FileStorageException {
        try {
            Path path = uploadDir.resolve(file.getStoredFilename());
            byte[] encrypted = Files.readAllBytes(path);
            return encryptionService.decrypt(encrypted);
        } catch (Exception e) {
            throw new FileStorageException("Failed to retrieve file", e);
        }
    }

    public void deleteFile(Long fileId, User user) throws FileStorageException {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        if (!file.getUser().getId().equals(user.getId())) {
            throw new SecurityException(
                    String.format("User %s is not authorized to delete file %d owned by user %s",
                            user.getEmail(),
                            fileId,
                            file.getUser().getEmail())
            );
        }

        try {
            Files.deleteIfExists(uploadDir.resolve(file.getStoredFilename()));
            fileRepository.delete(file);
            auditService.logDeletion(user, file);
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file", e);
        }
    }

    private FileEntity buildFileEntity(MultipartFile file, String storedFilename, User user) {
        return FileEntity.builder()
                .originalFilename(file.getOriginalFilename())
                .storedFilename(storedFilename)
                .contentType(file.getContentType())
                .size(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .user(user)
                .build();
    }

    private String generateSecureFilename(String originalFilename) {
        String sanitized = Paths.get(originalFilename).getFileName().toString();
        return String.format("%s_%d_%s",
                UUID.randomUUID(),
                Instant.now().toEpochMilli(),
                sanitized);
    }

    public static class FileNotFoundException extends RuntimeException {
        public FileNotFoundException(Long fileId) {
            super("File not found with ID: " + fileId);
        }
    }
}