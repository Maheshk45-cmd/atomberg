package com.atomquest.goalportal.controller;

import com.atomquest.goalportal.dto.GoalRequest;
import com.atomquest.goalportal.entity.*;
import com.atomquest.goalportal.repository.UserRepository;
import com.atomquest.goalportal.service.GoalSheetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GoalSheetController {

    private final GoalSheetService goalSheetService;
    private final UserRepository userRepository;

    private UUID currentUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow().getId();
    }

    // Employee: get or create their sheet
    @GetMapping("/goal-sheets/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<GoalSheet> getMySheet(Authentication auth) {
        return ResponseEntity.ok(goalSheetService.getOrCreateSheet(currentUserId(auth)));
    }

    // Employee: add a goal
    @PostMapping("/goal-sheets/{sheetId}/goals")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Goal> addGoal(@PathVariable UUID sheetId,
                                        @Valid @RequestBody GoalRequest req,
                                        Authentication auth) {
        return ResponseEntity.ok(goalSheetService.addGoal(sheetId, req, currentUserId(auth)));
    }

    // Employee: submit sheet
    @PostMapping("/goal-sheets/{sheetId}/submit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<GoalSheet> submit(@PathVariable UUID sheetId, Authentication auth) {
        return ResponseEntity.ok(goalSheetService.submitSheet(sheetId, currentUserId(auth)));
    }

    // Manager: get team sheets
    @GetMapping("/goal-sheets/team")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<GoalSheet>> getTeamSheets(Authentication auth) {
        return ResponseEntity.ok(goalSheetService.getSheetsForManager(currentUserId(auth)));
    }

    // Manager: approve sheet
    @PostMapping("/goal-sheets/{sheetId}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<GoalSheet> approve(@PathVariable UUID sheetId, Authentication auth) {
        return ResponseEntity.ok(goalSheetService.approveSheet(sheetId, currentUserId(auth)));
    }

    // Manager: return sheet
    @PostMapping("/goal-sheets/{sheetId}/return")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<GoalSheet> returnSheet(@PathVariable UUID sheetId, Authentication auth) {
        return ResponseEntity.ok(goalSheetService.returnSheet(sheetId, currentUserId(auth)));
    }

    // Manager: edit goal inline before approving
    @PutMapping("/goals/{goalId}/manager-edit")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Goal> managerEdit(@PathVariable UUID goalId,
                                            @RequestBody GoalRequest req,
                                            Authentication auth) {
        return ResponseEntity.ok(goalSheetService.updateGoalByManager(goalId, req, currentUserId(auth)));
    }

    // Admin: edit locked goal
    @PutMapping("/goals/{goalId}/admin-edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Goal> adminEdit(@PathVariable UUID goalId,
                                          @RequestBody GoalRequest req,
                                          @RequestParam(required = false) String reason,
                                          Authentication auth) {
        return ResponseEntity.ok(goalSheetService.updateLockedGoalByAdmin(goalId, req, currentUserId(auth), reason));
    }

    // Get goals for a sheet — authorized by role
    @GetMapping("/goal-sheets/{sheetId}/goals")
    public ResponseEntity<List<Goal>> getGoals(@PathVariable UUID sheetId, Authentication auth) {
        String role = auth.getAuthorities().iterator().next().getAuthority();
        UUID requesterId = currentUserId(auth);
        return ResponseEntity.ok(goalSheetService.getGoalsForSheet(sheetId, requesterId, role));
    }

    // Admin: all sheets
    @GetMapping("/goal-sheets")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GoalSheet>> allSheets() {
        return ResponseEntity.ok(goalSheetService.getAllSheets());
    }

    @GetMapping("/debug/auth")
    public ResponseEntity<String> debugAuth(Authentication auth) {
        return ResponseEntity.ok(auth.getAuthorities().toString());
    }
}
