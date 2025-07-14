package com.secureshare.securefiles;

import com.secureshare.securefiles.file.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FileSharingServiceTest {

    private SharedFileRepository sharedFileRepository;
    private FileRepository fileRepository;
    private PasswordEncoder passwordEncoder;
    private FileSharingService fileSharingService;

    @BeforeEach
    void setUp() {
        sharedFileRepository = mock(SharedFileRepository.class);
        fileRepository = mock(FileRepository.class);
        passwordEncoder = new BCryptPasswordEncoder(); // Real encoder is okay for tests
        fileSharingService = new FileSharingService(sharedFileRepository, fileRepository, passwordEncoder);
    }

    @Test
    void shouldGenerateShareLinkWithPassword() {
        // Arrange
        Long fileId = 1L;
        FileEntity file = FileEntity.builder().id(fileId).build();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        ArgumentCaptor<SharedFile> sharedFileCaptor = ArgumentCaptor.forClass(SharedFile.class);

        // Act
        String token = fileSharingService.generateShareLink(fileId, "secret123", 10);

        // Assert
        assertNotNull(token);
        verify(sharedFileRepository).save(sharedFileCaptor.capture());

        SharedFile saved = sharedFileCaptor.getValue();
        assertEquals(file, saved.getFile());
        assertTrue(saved.getExpiry().isAfter(LocalDateTime.now()));
        assertNotNull(saved.getPassword());
        assertTrue(passwordEncoder.matches("secret123", saved.getPassword()));
    }

    @Test
    void shouldGenerateShareLinkWithoutPassword() {
        // Arrange
        Long fileId = 2L;
        FileEntity file = FileEntity.builder().id(fileId).build();
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        // Act
        String token = fileSharingService.generateShareLink(fileId, null, 5);

        // Assert
        assertNotNull(token);
        verify(sharedFileRepository).save(any(SharedFile.class));
    }

    @Test
    void shouldReturnValidSharedFileWhenNoPassword() {
        // Arrange
        String token = "abc123";
        SharedFile shared = SharedFile.builder()
                .token(token)
                .expiry(LocalDateTime.now().plusMinutes(5))
                .password(null)
                .build();

        when(sharedFileRepository.findByToken(token)).thenReturn(Optional.of(shared));

        // Act
        Optional<SharedFile> result = fileSharingService.getValidSharedFile(token, null);

        // Assert
        assertTrue(result.isPresent());
    }

    @Test
    void shouldReturnValidSharedFileWhenPasswordMatches() {
        // Arrange
        String rawPassword = "mypassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        SharedFile shared = SharedFile.builder()
                .token("t1")
                .expiry(LocalDateTime.now().plusMinutes(1))
                .password(encodedPassword)
                .build();

        when(sharedFileRepository.findByToken("t1")).thenReturn(Optional.of(shared));

        // Act
        Optional<SharedFile> result = fileSharingService.getValidSharedFile("t1", rawPassword);

        // Assert
        assertTrue(result.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenPasswordDoesNotMatch() {
        // Arrange
        SharedFile shared = SharedFile.builder()
                .token("t2")
                .expiry(LocalDateTime.now().plusMinutes(1))
                .password(passwordEncoder.encode("correct"))
                .build();

        when(sharedFileRepository.findByToken("t2")).thenReturn(Optional.of(shared));

        // Act
        Optional<SharedFile> result = fileSharingService.getValidSharedFile("t2", "wrong");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenTokenExpired() {
        // Arrange
        SharedFile shared = SharedFile.builder()
                .token("expired")
                .expiry(LocalDateTime.now().minusMinutes(1))
                .build();

        when(sharedFileRepository.findByToken("expired")).thenReturn(Optional.of(shared));

        // Act
        Optional<SharedFile> result = fileSharingService.getValidSharedFile("expired", null);

        // Assert
        assertTrue(result.isEmpty());
    }
}
