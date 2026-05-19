package com.atomquest.goalportal.controller;

import com.atomquest.goalportal.dto.AchievementRequest;
import com.atomquest.goalportal.entity.Achievement;
import com.atomquest.goalportal.enums.Quarter;
import com.atomquest.goalportal.repository.UserRepository;
import com.atomquest.goalportal.service.AchievementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/goals/{goalId}/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;
    private final UserRepository userRepository;

    @PostMapping("/{quarter}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Achievement> logAchievement(@PathVariable UUID goalId,
                                                       @PathVariable Quarter quarter,
                                                       @Valid @RequestBody AchievementRequest req,
                                                       Authentication auth) {
        UUID userId = userRepository.findByEmail(auth.getName()).orElseThrow().getId();
        return ResponseEntity.ok(achievementService.logAchievement(goalId, quarter, req, userId));
    }

    @GetMapping
    public ResponseEntity<List<Achievement>> getAll(@PathVariable UUID goalId) {
        return ResponseEntity.ok(achievementService.getAchievementsForGoal(goalId));
    }
}
