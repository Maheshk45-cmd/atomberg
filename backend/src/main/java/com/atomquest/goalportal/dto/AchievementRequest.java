package com.atomquest.goalportal.dto;

import com.atomquest.goalportal.enums.AchievementStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AchievementRequest {

    @NotNull
    private BigDecimal actualValue;

    private LocalDate actualDate;

    @NotNull
    private AchievementStatus status;
}
