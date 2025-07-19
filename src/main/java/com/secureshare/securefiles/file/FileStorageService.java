package com.secureshare.securefiles.file;

import com.secureshare.securefiles.user.User;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12; // bytes for GCM

    private final FileRepository fileRepository;
    private final SharedFileRepository sharedFileRepository;
    private final Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

    @Value("${app.encryption.secret-key}")

    private String encryptionKey;
    public static class FileNotFoundException extends RuntimeException {
        public FileNotFoundException(Long fileId) {
            super("File not found with ID: " + fileId);
        }
    }

    @PostConstruct
    public void init() throws IOException {
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
            log.info("Created upload directory: {}", uploadDir);
        }
        // Set directory permissions (Unix only)
        try {
            Files.setPosixFilePermissions(uploadDir,
                    PosixFilePermissions.fromString("rwxr-x---"));
        } catch (UnsupportedOperationException e) {
            log.warn("Could not set POSIX permissions on upload directory");
        }
    }

    @Transactional
    public FileEntity saveFile(MultipartFile file) throws IOException, CryptoException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        String originalFilename = Paths.get(Objects.requireNonNull(file.getOriginalFilename())).getFileName().toString(); // Sanitize filename
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;
        Path destination = uploadDir.resolve(storedFilename);

        try {
            // Generate random IV for GCM
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            // Encrypt file
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), parameterSpec);

            byte[] encryptedData = cipher.doFinal(file.getBytes());

            // Write IV + encrypted data
            try (OutputStream os = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
                os.write(iv);
                os.write(encryptedData);
            }

            FileEntity entity = FileEntity.builder()
                    .originalFilename(originalFilename)
                    .storedFilename(storedFilename)
                    .contentType(file.getContentType())
                    .size(Files.size(destination))
                    .uploadedAt(LocalDateTime.now())
                    .user(user) // Set the user relationship
                    .build();

            return fileRepository.save(entity);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException | InvalidAlgorithmParameterException e) {
            // Clean up if encryption fails
            Files.deleteIfExists(destination);
            throw new CryptoException("File encryption failed", e);
        }
    }

    public byte[] getFileContent(FileEntity entity) throws CryptoException, IOException {
        Path path = uploadDir.resolve(entity.getStoredFilename());
        byte[] fileContent = Files.readAllBytes(path);

        try {
            // Extract IV (first 12 bytes)
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(fileContent, 0, iv, 0, IV_LENGTH);
            byte[] encryptedData = new byte[fileContent.length - IV_LENGTH];
            System.arraycopy(fileContent, IV_LENGTH, encryptedData, 0, encryptedData.length);

            // Decrypt
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), parameterSpec);

            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new CryptoException("File decryption failed", e);
        }
    }

    @Transactional
    public void deleteFile(Long fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        // Verify ownership
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        if (!file.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("User not authorized to delete this file");
        }

        try {
            // Delete shares first
            sharedFileRepository.deleteByFile(file);

            // Delete physical file
            Path filePath = uploadDir.resolve(file.getStoredFilename());
            Files.deleteIfExists(filePath);

            // Delete DB record
            fileRepository.delete(file);
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file", e);
        }
    }


    // Custom exceptions
    public static class CryptoException extends Exception {
        public CryptoException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class FileStorageException extends RuntimeException {
        public FileStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private SecretKey getSecretKey() {
        return new SecretKeySpec(encryptionKey.getBytes(), "AES");
    }

}