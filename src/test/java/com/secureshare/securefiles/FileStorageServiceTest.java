package com.secureshare.securefiles;

import com.secureshare.securefiles.file.FileEntity;
import com.secureshare.securefiles.file.FileRepository;
import com.secureshare.securefiles.file.FileStorageService;
import com.secureshare.securefiles.file.SharedFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FileStorageServiceTest {

    private FileRepository fileRepository;
    private SharedFileRepository sharedFileRepository;
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() throws Exception {
        fileRepository = mock(FileRepository.class);
        sharedFileRepository = mock(SharedFileRepository.class);
        fileStorageService = new FileStorageService(fileRepository, sharedFileRepository);

        // Inject a test key
        String testKey = "1234567890123456"; // 16-char = 128-bit key
        ReflectionTestUtils.setField(fileStorageService, "encryptionKey", testKey);

        fileStorageService.init();
    }

    @Test
    void shouldEncryptAndSaveFileSuccessfully() throws Exception {
        // Mock the SecurityContext
        SecurityContext securityContext = mock(SecurityContext.class);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("testuser", null, List.of());

        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        // Arrange
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "Hello SecureShare".getBytes());

        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FileEntity savedFile = fileStorageService.saveFile(multipartFile);

        // Assert
        assertNotNull(savedFile);
        assertEquals("hello.txt", savedFile.getOriginalFilename());
        assertEquals("text/plain", savedFile.getContentType());
        verify(fileRepository).save(any(FileEntity.class));

        // Check file exists and is encrypted
        Path savedPath = Path.of("uploads", savedFile.getStoredFilename());
        assertTrue(Files.exists(savedPath));
        byte[] rawBytes = Files.readAllBytes(savedPath);
        assertNotEquals("Hello SecureShare", new String(rawBytes));

        // Clear security context after test
        SecurityContextHolder.clearContext();
    }
    @Test
    void shouldDecryptAndReturnFileContent() throws Exception {
        // Arrange
        String content = "Secret Data!";
        String testKey = "1234567890123456";
        SecretKeySpec key = new SecretKeySpec(testKey.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(content.getBytes());

        String fakeName = "test_encrypted_file.dat";
        Path filePath = Path.of("uploads", fakeName);
        Files.write(filePath, encrypted);

        FileEntity entity = FileEntity.builder()
                .storedFilename(fakeName)
                .build();

        ReflectionTestUtils.setField(fileStorageService, "encryptionKey", testKey);

        // Act
        byte[] decrypted = fileStorageService.getFileContent(entity);

        // Assert
        assertEquals(content, new String(decrypted));
    }

    @Test
    void shouldDeleteFileAndRemoveMetadata() throws Exception {
        // Arrange
        String fakeFile = "file_to_delete.txt";
        Path path = Path.of("uploads", fakeFile);
        Files.write(path, "dummy".getBytes());

        FileEntity entity = FileEntity.builder()
                .storedFilename(fakeFile)
                .build();

        // Act
        fileStorageService.deleteFile(entity);

        // Assert
        assertFalse(Files.exists(path));
        verify(sharedFileRepository).deleteByFile(entity);
        verify(fileRepository).delete(entity);
    }
}
