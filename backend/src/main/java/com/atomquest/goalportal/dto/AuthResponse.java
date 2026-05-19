package com.atomquest.goalportal.dto;

import lombok.*;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String role;
    private String name;
    private String email;
    private String userId;
}
