package com.atomquest.goalportal.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CycleRequest {
    private int year;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isActive;
}
