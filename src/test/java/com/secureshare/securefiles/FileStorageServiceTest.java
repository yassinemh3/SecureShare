package com.secureshare.securefiles;

import com.secureshare.securefiles.file.FileEntity;
import com.secureshare.securefiles.file.FileRepository;
import com.secureshare.securefiles.file.FileStorageService;
import com.secureshare.securefiles.file.SharedFileRepository;
import com.secureshare.securefiles.service.*;
import com.secureshare.securefiles.service.exception.*;
import com.secureshare.securefiles.user.User;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileStorageServiceTest {

    @InjectMocks
    private FileStorageService fileStorageService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private SharedFileRepository sharedFileRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private FileValidator fileValidator;

    @Mock
    private AuditService auditService;

    private static final String TEST_UPLOAD_DIR = "test-uploads";

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        // Initialize test upload directory
        Path uploadDir = Path.of(TEST_UPLOAD_DIR);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Set test upload directory
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", uploadDir);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up test files
        Files.walk(Path.of(TEST_UPLOAD_DIR))
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Test
    void testSaveFile_success() throws Exception {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes()
        );

        User user = new User();
        user.setId(1);
        user.setEmail("test@example.com");

        String expectedStoredFilename = "uuid_123456789_test.txt";
        byte[] encryptedData = "encrypted-data".getBytes();

        // When
        when(encryptionService.encrypt(any())).thenReturn(encryptedData);

        FileEntity savedFile = FileEntity.builder()
                .id(1L)
                .originalFilename("test.txt")
                .storedFilename(expectedStoredFilename)
                .contentType("text/plain")
                .size(11L)
                .uploadedAt(LocalDateTime.now())
                .user(user)
                .build();

        when(fileRepository.save(any())).thenReturn(savedFile);

        // Then
        FileEntity result = fileStorageService.saveFile(mockFile, user);

        assertNotNull(result);
        assertEquals("test.txt", result.getOriginalFilename());
        verify(encryptionService).encrypt(mockFile.getBytes());
        verify(auditService).logUpload(user, result);
    }

    @Test
    void testGetFileContent_success() throws Exception {
        // Given
        FileEntity file = FileEntity.builder()
                .storedFilename("test.txt")
                .build();

        byte[] encryptedData = "encrypted-data".getBytes();
        byte[] decryptedData = "Hello World".getBytes();

        Path filePath = Path.of(TEST_UPLOAD_DIR, file.getStoredFilename());
        Files.write(filePath, encryptedData);

        // When
        when(encryptionService.decrypt(encryptedData)).thenReturn(decryptedData);

        // Then
        byte[] result = fileStorageService.getFileContent(file);
        assertArrayEquals(decryptedData, result);
    }

    @Test
    void testDeleteFile_success() throws Exception {
        // Given
        User user = new User();
        user.setId(1);

        FileEntity file = FileEntity.builder()
                .id(1L)
                .storedFilename("test.txt")
                .user(user)
                .build();

        Path filePath = Path.of(TEST_UPLOAD_DIR, file.getStoredFilename());
        Files.write(filePath, "test-data".getBytes());

        when(fileRepository.findById(1L)).thenReturn(Optional.of(file));

        // When
        fileStorageService.deleteFile(1L, user);

        // Then
        verify(sharedFileRepository).deleteByFile(file);
        verify(fileRepository).delete(file);
        verify(auditService).logDeletion(user, file);
        assertFalse(Files.exists(filePath));
    }

    @Test
    void testDeleteFile_notFound() {
        // Given
        when(fileRepository.findById(999L)).thenReturn(Optional.empty());

        // Then
        assertThrows(FileStorageService.FileNotFoundException.class, () -> {
            fileStorageService.deleteFile(999L, new User());
        });
    }

    @Test
    void testDeleteFile_unauthorized() {
        // Given
        User owner = new User();
        owner.setId(1);

        User requester = new User();
        requester.setId(2);

        FileEntity file = FileEntity.builder()
                .id(1L)
                .user(owner)
                .build();

        when(fileRepository.findById(1L)).thenReturn(Optional.of(file));

        // Then
        assertThrows(SecurityException.class, () -> {
            fileStorageService.deleteFile(1L, requester);
        });
    }
}