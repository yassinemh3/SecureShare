package com.secureshare.securefiles.dto;

import com.secureshare.securefiles.dto.SharedFileDTO;
import com.secureshare.securefiles.file.FileEntity;

import java.time.LocalDateTime;
import java.util.List;

public record FileResponseDTO(
        Long id,
        String originalFilename,
        String contentType,
        long size,
        String uploadedBy,
        LocalDateTime uploadedAt,
        List<SharedFileDTO> sharedFiles
) {
    public static FileResponseDTO fromEntity(FileEntity file) {
        return new FileResponseDTO(
                file.getId(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getUser() != null ? file.getUser().getEmail() : null,
                file.getUploadedAt(),
                file.getSharedFiles().stream()
                        .map(SharedFileDTO::fromEntity)
                        .toList()
        );
    }
}