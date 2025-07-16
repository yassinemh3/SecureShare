package com.secureshare.securefiles.dto;

import com.secureshare.securefiles.file.SharedFile;
import java.time.Instant;

public record SharedFileDTO(
        Long id,
        String token,
        Instant expiryDate,
        boolean hasPassword
) {
    public static SharedFileDTO fromEntity(SharedFile sharedFile) {
        return new SharedFileDTO(
                sharedFile.getId(),
                sharedFile.getToken(),
                sharedFile.getExpiry(),
                sharedFile.getPassword() != null
        );
    }
}