package com.secureshare.securefiles.dto;

import com.secureshare.securefiles.file.FileEntity;
import com.secureshare.securefiles.file.SharedFile;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Builder
public class SharedFileDTO {
    private final Long id;
    private final String token;
    private final String filename;
    private final Instant expiryDate;
    private final boolean hasPassword;
    private final String shareUrl;
    private String qrCodeUrl;
    private final LocalDateTime createdAt;
    private final Long fileId;
    private final String fileContentType;
    private final Long fileSize;

    public static SharedFileDTO fromEntity(SharedFile sharedFile) {
        FileEntity file = sharedFile.getFile();
        return SharedFileDTO.builder()
                .id(sharedFile.getId())
                .token(sharedFile.getToken())
                .filename(file.getOriginalFilename())
                .expiryDate(sharedFile.getExpiry())
                .hasPassword(sharedFile.getPassword() != null)
                .shareUrl("http://localhost:5173/share/access/" + sharedFile.getToken())
                .qrCodeUrl("http://localhost:8080/api/v1/share/qr/" + sharedFile.getToken())
                .createdAt(sharedFile.getCreatedAt())
                .fileId(file.getId())
                .fileContentType(file.getContentType())
                .fileSize(file.getSize())
                .build();
    }
}