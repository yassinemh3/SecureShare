package com.secureshare.securefiles.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class ShareResponseDTO {
    private String token;
    private Instant expiry;
    private boolean hasPassword;
    private String shareUrl;
    private String qrCodeUrl;
}