package com.atomquest.goalportal.dto;

import com.atomquest.goalportal.enums.UoM;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class GoalRequest {

    @NotNull
    private UUID thrustAreaId;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private UoM uom;

    private BigDecimal targetValue;

    private LocalDate targetDate;

    @NotNull
    @DecimalMin("10.0")
    @DecimalMax("100.0")
    private BigDecimal weightage;
}
