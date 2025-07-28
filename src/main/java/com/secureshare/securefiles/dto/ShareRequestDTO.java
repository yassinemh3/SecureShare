package com.secureshare.securefiles.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShareRequestDTO {
    private String password;

    @NotNull
    @Min(1)
    private Long expiryMinutes;
}