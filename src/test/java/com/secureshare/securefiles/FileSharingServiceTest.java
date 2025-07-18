package com.secureshare.securefiles;

import com.secureshare.securefiles.file.*;
import com.secureshare.securefiles.dto.SharedFileDTO;
import com.secureshare.securefiles.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        passwordEncoder = new BCryptPasswordEncoder();
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
        assertTrue(saved.getExpiry().isAfter(Instant.now()));
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
    void shouldThrowWhenFileNotFoundForShareLink() {
        // Arrange
        Long fileId = 99L;
        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                fileSharingService.generateShareLink(fileId, null, 5)
        );
    }

    @Test
    void shouldReturnValidSharedFileWhenNoPassword() {
        // Arrange
        String token = "abc123";
        SharedFile shared = SharedFile.builder()
                .token(token)
                .expiry(Instant.now().plusSeconds(60))
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
                .expiry(Instant.now().plusSeconds(60))
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
                .expiry(Instant.now().plusSeconds(60))
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
                .expiry(Instant.now().minusSeconds(60))
                .build();

        when(sharedFileRepository.findByToken("expired")).thenReturn(Optional.of(shared));

        // Act
        Optional<SharedFile> result = fileSharingService.getValidSharedFile("expired", null);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenTokenNotFound() {
        // Arrange
        when(sharedFileRepository.findByToken("missing")).thenReturn(Optional.empty());

        // Act
        Optional<SharedFile> result = fileSharingService.getValidSharedFile("missing", null);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldGetUserSharedFiles() {
        // Arrange
        User user = User.builder().id(1).build();
        Pageable pageable = PageRequest.of(0, 10);

        FileEntity file = FileEntity.builder()
                .id(1L)
                .originalFilename("test.txt")
                .build();

        SharedFile sharedFile = SharedFile.builder()
                .id(1L)
                .token(UUID.randomUUID().toString())
                .file(file)
                .expiry(Instant.now().plusSeconds(3600))
                .build();

        when(sharedFileRepository.findBySharedBy(user, pageable))
                .thenReturn(new PageImpl<>(List.of(sharedFile)));

        // Act
        Page<SharedFileDTO> result = fileSharingService.getUserSharedFiles(user, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        SharedFileDTO dto = result.getContent().get(0);
        assertEquals(file.getOriginalFilename(), dto.getFilename());
        assertEquals(sharedFile.getToken(), dto.getToken());
    }

    @Test
    void shouldRevokeShareSuccessfully() {
        // Arrange
        String token = "valid-token";
        User user = User.builder().id(1).build();
        SharedFile share = SharedFile.builder()
                .token(token)
                .sharedBy(user)
                .build();

        when(sharedFileRepository.findByToken(token)).thenReturn(Optional.of(share));

        // Act & Assert
        assertDoesNotThrow(() -> fileSharingService.revokeShare(token, user));
        verify(sharedFileRepository).delete(share);
    }

    @Test
    void shouldThrowWhenRevokingNonExistentShare() {
        // Arrange
        String token = "invalid-token";
        when(sharedFileRepository.findByToken(token)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> fileSharingService.revokeShare(token, mock(User.class)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldThrowWhenRevokingUnauthorizedShare() {
        // Arrange
        String token = "valid-token";
        User owner = User.builder().id(1).build();
        User otherUser = User.builder().id(2).build();

        SharedFile share = SharedFile.builder()
                .token(token)
                .sharedBy(owner)
                .build();

        when(sharedFileRepository.findByToken(token)).thenReturn(Optional.of(share));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> fileSharingService.revokeShare(token, otherUser));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void shouldHandleDatabaseErrorDuringRevoke() {
        // Arrange
        String token = "valid-token";
        User user = User.builder().id(1).build();
        SharedFile share = SharedFile.builder()
                .token(token)
                .sharedBy(user)
                .build();

        when(sharedFileRepository.findByToken(token)).thenReturn(Optional.of(share));
        doThrow(new DataIntegrityViolationException("DB error")).when(sharedFileRepository).delete(share);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> fileSharingService.revokeShare(token, user));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    }

}