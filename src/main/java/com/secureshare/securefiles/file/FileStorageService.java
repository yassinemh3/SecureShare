package com.secureshare.securefiles.file;

import com.secureshare.securefiles.file.FileEntity;
import com.secureshare.securefiles.file.FileRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileRepository fileRepository;
    private final Path uploadDir = Paths.get("uploads");

    @Value("${app.encryption.secret-key}")
    private String encryptionKey;

    private SecretKey getSecretKey() {
        return new SecretKeySpec(encryptionKey.getBytes(), "AES");
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(uploadDir);
    }

    public FileEntity saveFile(MultipartFile file) throws Exception {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String originalFilename = file.getOriginalFilename();
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;

        // Encrypt the file
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
        byte[] encryptedData = cipher.doFinal(file.getBytes());

        // Save to disk
        Path destination = uploadDir.resolve(storedFilename);
        Files.write(destination, encryptedData);

        FileEntity entity = FileEntity.builder()
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .contentType(file.getContentType())
                .size((long) encryptedData.length)
                .uploadedBy(username)
                .uploadedAt(LocalDateTime.now())
                .build();

        return fileRepository.save(entity);
    }

    public FileEntity getFileMetadata(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    public List<FileEntity> getUserFiles() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return fileRepository.findAllByUploadedBy(username);
    }

    public byte[] getFileContent(FileEntity entity) throws Exception {
        Path path = uploadDir.resolve(entity.getStoredFilename());
        byte[] encryptedData = Files.readAllBytes(path);

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
        return cipher.doFinal(encryptedData);
    }
    public void deleteFile(FileEntity file) {
        Path filePath = uploadDir.resolve(file.getStoredFilename());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file from storage", e);
        }

        // Remove from database
        fileRepository.delete(file);
    }
}
