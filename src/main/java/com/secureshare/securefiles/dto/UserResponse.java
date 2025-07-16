package com.secureshare.securefiles.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {
    private String firstname;
    private String lastname;
    private String email;
    private String role;
}