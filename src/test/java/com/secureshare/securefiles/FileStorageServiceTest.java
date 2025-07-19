package com.secureshare.securefiles;

import com.secureshare.securefiles.file.*;
import com.secureshare.securefiles.user.User;

import org.junit.jupiter.api.*;
import org.mockito.*;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.lang.reflect.Field;
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

    private static final String FAKE_ENCRYPTION_KEY = "1234567890123456"; // 16 chars for AES

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Manually set encryptionKey via reflection
        Field field = FileStorageService.class.getDeclaredField("encryptionKey");
        field.setAccessible(true);
        field.set(fileStorageService, FAKE_ENCRYPTION_KEY);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticatedUser(User user) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(context);
    }

    @Test
    void testSaveFile_success() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes()
        );

        User user = new User();
        user.setId(1);
        user.setEmail("test@example.com");

        mockAuthenticatedUser(user);

        FileEntity savedFile = FileEntity.builder()
                .id(1L)
                .originalFilename("test.txt")
                .storedFilename("uuid-test.txt")
                .contentType("text/plain")
                .size(11)
                .uploadedAt(LocalDateTime.now())
                .user(user)
                .build();

        when(fileRepository.save(any())).thenReturn(savedFile);

        FileEntity result = fileStorageService.saveFile(mockFile);

        assertNotNull(result);
        assertEquals("test.txt", result.getOriginalFilename());
    }

    @Test
    void testGetFileContent_success() throws Exception {
        // Given
        FileEntity file = FileEntity.builder()
                .storedFilename("file.txt")
                .build();

        fileStorageService = spy(fileStorageService);
        byte[] expectedContent = "Test content".getBytes();

        doReturn(expectedContent).when(fileStorageService).getFileContent(file);

        // When
        byte[] result = fileStorageService.getFileContent(file);

        // Then
        assertNotNull(result);
        assertArrayEquals(expectedContent, result);
    }

    @Test
    void testDeleteFile_success() {
        User user = new User();
        user.setId(1);
        user.setEmail("user@example.com");

        FileEntity file = FileEntity.builder()
                .id(1L)
                .storedFilename("test.txt")
                .user(user)
                .build();

        when(fileRepository.findById(1L)).thenReturn(Optional.of(file));

        mockAuthenticatedUser(user);

        fileStorageService.deleteFile(1L);

        verify(sharedFileRepository).deleteByFile(file);
        verify(fileRepository).delete(file);
    }

    @Test
    void testDeleteFile_notFound() {
        // Given
        when(fileRepository.findById(999L)).thenReturn(Optional.empty());

        // Then
        assertThrows(FileStorageService.FileNotFoundException.class, () -> {
            fileStorageService.deleteFile(999L);
        });
    }
}
