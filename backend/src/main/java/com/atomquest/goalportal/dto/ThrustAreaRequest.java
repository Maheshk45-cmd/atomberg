package com.atomquest.goalportal.dto;

import lombok.Data;

@Data
public class ThrustAreaRequest {
    private String name;
    private String description;
    private boolean isActive;
}
