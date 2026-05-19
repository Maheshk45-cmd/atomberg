package com.atomquest.goalportal.controller;

import com.atomquest.goalportal.dto.SharedGoalRequest;
import com.atomquest.goalportal.entity.Goal;
import com.atomquest.goalportal.service.SharedGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shared-goals")
@RequiredArgsConstructor
public class SharedGoalController {

    private final SharedGoalService sharedGoalService;

    @PostMapping("/push")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Goal>> pushSharedGoal(@RequestBody SharedGoalRequest req) {
        return ResponseEntity.ok(sharedGoalService.pushSharedGoal(req));
    }
}
