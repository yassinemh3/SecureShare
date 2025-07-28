package com.secureshare.securefiles.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;

@Setter
@Getter
public class ShareInfoDTO {
    private String filename;
    private boolean hasPassword;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant expiresAt;

    // Constructor
    public ShareInfoDTO(String filename, boolean hasPassword, Instant expiresAt) {
        this.filename = filename;
        this.hasPassword = hasPassword;
        this.expiresAt = expiresAt;
    }

}