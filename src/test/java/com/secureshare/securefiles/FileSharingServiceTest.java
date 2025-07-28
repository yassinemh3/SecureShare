package com.secureshare.securefiles;

import com.secureshare.securefiles.dto.*;
import com.secureshare.securefiles.file.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FileSharingServiceTest {

    private SharedFileRepository sharedFileRepository;
    private FileRepository fileRepository;
    private PasswordEncoder passwordEncoder;
    private ShareTokenService tokenService;
    private FileSharingService fileSharingService;

    @BeforeEach
    void setUp() {
        sharedFileRepository = mock(SharedFileRepository.class);
        fileRepository = mock(FileRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        tokenService = mock(ShareTokenService.class);
        fileSharingService = new FileSharingService(
                sharedFileRepository,
                fileRepository,
                passwordEncoder,
                tokenService
        );
    }

    @Test
    void shouldCreateShareWithPassword() {
        // Arrange
        Long fileId = 1L;
        User user = User.builder().id(1).build();
        FileEntity file = FileEntity.builder()
                .id(fileId)
                .originalFilename("document.pdf")
                .build();

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(tokenService.generateSecureToken()).thenReturn("secure-token-123");

        ShareRequestDTO request = new ShareRequestDTO();
        request.setPassword("secret123");
        request.setExpiryMinutes(60L);

        // Act
        ShareResponseDTO response = fileSharingService.createShare(fileId, request.getPassword(), request.getExpiryMinutes(), user);

        // Assert
        assertNotNull(response);
        assertEquals("secure-token-123", response.getToken());
        assertTrue(response.isHasPassword());
        assertNotNull(response.getExpiry());
        assertTrue(response.getExpiry().isAfter(Instant.now()));
        assertEquals("http://localhost:5173/share/access/secure-token-123", response.getShareUrl());
        assertEquals("http://localhost:8080/api/v1/share/qr/secure-token-123", response.getQrCodeUrl());

        ArgumentCaptor<SharedFile> captor = ArgumentCaptor.forClass(SharedFile.class);
        verify(sharedFileRepository).save(captor.capture());

        SharedFile savedShare = captor.getValue();
        assertEquals(file, savedShare.getFile());
        assertEquals(user, savedShare.getSharedBy());
        assertTrue(passwordEncoder.matches("secret123", savedShare.getPassword()));
    }

    @Test
    void shouldCreateShareWithoutPassword() {
        // Arrange
        Long fileId = 2L;
        User user = User.builder().id(1).build();
        FileEntity file = FileEntity.builder().id(fileId).build();

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(tokenService.generateSecureToken()).thenReturn("no-pw-token");

        // Act
        ShareResponseDTO response = fileSharingService.createShare(fileId, null, 30L, user);

        // Assert
        assertNotNull(response);
        assertFalse(response.isHasPassword());
    }

    @Test
    void shouldThrowWhenFileNotFoundForShare() {
        // Arrange
        Long fileId = 99L;
        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () ->
                fileSharingService.createShare(fileId, null, 10L, mock(User.class))
        );
    }

    @Test
    void shouldReturnValidSharedFileWhenNoPassword() {
        // Arrange
        String token = "valid-token";
        SharedFile shared = SharedFile.builder()
                .token(token)
                .expiry(Instant.now().plusSeconds(60))
                .password(null)
                .active(true)
                .build();

        when(sharedFileRepository.findByToken(token)).thenReturn(Optional.of(shared));
        when(tokenService.isValidTokenFormat(token)).thenReturn(true);

        // Act
        Optional<SharedFile> result = fileSharingService.getValidSharedFile(token, null);

        // Assert
        assertTrue(result.isPresent());
    }

    @Test
    void shouldReturnEmptyForInactiveShare() {
        // Arrange
        String token = "inactive-token";
        SharedFile shared = SharedFile.builder()
                .token(token)
                .active(false)
                .build();

        when(sharedFileRepository.findByToken(token)).thenReturn(Optional.of(shared));
        when(tokenService.isValidTokenFormat(token)).thenReturn(true);

        // Act
        Optional<SharedFile> result = fileSharingService.getValidSharedFile(token, null);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyForInvalidTokenFormat() {
        // Arrange
        String token = "invalid-format";
        when(tokenService.isValidTokenFormat(token)).thenReturn(false);

        // Act
        Optional<SharedFile> result = fileSharingService.getValidSharedFile(token, null);

        // Assert
        assertTrue(result.isEmpty());
        verify(sharedFileRepository, never()).findByToken(any());
    }

    @Test
    void shouldGetUserSharedFiles() {
        // Arrange
        User user = User.builder().id(1).build();
        Pageable pageable = PageRequest.of(0, 10);

        FileEntity file = FileEntity.builder()
                .id(1L)
                .originalFilename("report.pdf")
                .build();

        SharedFile sharedFile = SharedFile.builder()
                .id(1L)
                .token("share-token")
                .file(file)
                .expiry(Instant.now().plusSeconds(3600))
                .createdAt(LocalDateTime.now())
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
        assertEquals("http://localhost:5173/share/access/share-token", dto.getShareUrl());
        assertEquals("http://localhost:8080/api/v1/share/qr/share-token", dto.getQrCodeUrl());
    }

    @Test
    void shouldRevokeShareWithSoftDelete() {
        // Arrange
        String token = "revoke-token";
        User user = User.builder().id(1).build();
        SharedFile share = SharedFile.builder()
                .token(token)
                .sharedBy(user)
                .active(true)
                .build();

        when(sharedFileRepository.findByToken(token)).thenReturn(Optional.of(share));

        // Act
        fileSharingService.revokeShare(token, user);

        // Assert
        assertFalse(share.isActive());
        verify(sharedFileRepository).save(share); // Verify soft delete (update)
    }

    @Test
    void shouldThrowWhenRevokingNonExistentShare() {
        // Arrange
        String token = "missing-token";
        when(sharedFileRepository.findByToken(token)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> fileSharingService.revokeShare(token, mock(User.class)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldThrowWhenDatabaseErrorDuringRevoke() {
        // Arrange
        String token = "db-error-token";
        User user = User.builder().id(1).build();
        SharedFile share = SharedFile.builder()
                .token(token)
                .sharedBy(user)
                .build();

        when(sharedFileRepository.findByToken(token)).thenReturn(Optional.of(share));
        doThrow(new DataIntegrityViolationException("DB error")).when(sharedFileRepository).save(share);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> fileSharingService.revokeShare(token, user));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    }

    @Test
    void shouldConvertToDtoCorrectly() {
        // Arrange
        FileEntity file = FileEntity.builder()
                .id(1L)
                .originalFilename("test.txt")
                .build();

        SharedFile sharedFile = SharedFile.builder()
                .id(1L)
                .token("test-token")
                .file(file)
                .expiry(Instant.now().plusSeconds(3600))
                .createdAt(LocalDateTime.now())
                .build();

        // Act
        SharedFileDTO dto = SharedFileDTO.fromEntity(sharedFile);

        // Assert
        assertEquals(sharedFile.getId(), dto.getId());
        assertEquals(sharedFile.getToken(), dto.getToken());
        assertEquals(file.getOriginalFilename(), dto.getFilename());
        assertEquals("http://localhost:5173/share/access/test-token", dto.getShareUrl());
        assertEquals("http://localhost:8080/api/v1/share/qr/test-token", dto.getQrCodeUrl());
    }
}