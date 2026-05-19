package com.atomquest.goalportal.dto;

import com.atomquest.goalportal.enums.UoM;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class SharedGoalRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private UoM uom;

    private BigDecimal targetValue;
    private LocalDate targetDate;

    @NotNull
    @DecimalMin("10.0")
    private BigDecimal minWeightage;

    @NotNull
    private UUID thrustAreaId;

    // List of employee user IDs to push to
    @NotEmpty
    private List<UUID> recipientIds;
}
