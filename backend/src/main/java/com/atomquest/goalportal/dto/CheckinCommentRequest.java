package com.atomquest.goalportal.dto;

import com.atomquest.goalportal.enums.Quarter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckinCommentRequest {

    @NotNull
    private Quarter quarter;

    @NotBlank
    private String comment;
}
