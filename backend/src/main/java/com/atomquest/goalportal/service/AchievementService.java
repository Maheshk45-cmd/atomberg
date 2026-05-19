package com.atomquest.goalportal.service;

import com.atomquest.goalportal.dto.AchievementRequest;
import com.atomquest.goalportal.entity.*;
import com.atomquest.goalportal.enums.*;
import com.atomquest.goalportal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final CycleRepository cycleRepository;
    private final CheckinWindowRepository checkinWindowRepository;

    @Transactional
    public Achievement logAchievement(UUID goalId, Quarter quarter, AchievementRequest req, UUID userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found"));

        // Window enforcement
        Cycle cycle = cycleRepository.findByIsActiveTrue()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active cycle"));
        CheckinWindow window = checkinWindowRepository.findByCycleIdAndQuarter(cycle.getId(), quarter)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Check-in window not configured"));

        LocalDate today = LocalDate.now();
        if (today.isBefore(window.getOpenDate()) || today.isAfter(window.getCloseDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Check-in window for " + quarter + " is closed. Opens: " + window.getOpenDate() + ", Closes: " + window.getCloseDate());
        }

        BigDecimal score = computeScore(goal, req);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Achievement achievement = achievementRepository.findByGoalIdAndQuarter(goalId, quarter)
                .orElseGet(() -> Achievement.builder().goal(goal).quarter(quarter).cycle(cycle).loggedBy(user).build());

        achievement.setActualValue(req.getActualValue());
        achievement.setActualDate(req.getActualDate());
        achievement.setStatus(req.getStatus());
        achievement.setComputedScore(score);
        achievement.setLoggedBy(user);

        Achievement saved = achievementRepository.save(achievement);

        // Sync shared goals
        if (goal.isShared()) {
            List<Goal> linked = goalRepository.findBySharedFromId(goalId);
            for (Goal linked_goal : linked) {
                Achievement linkedAch = achievementRepository.findByGoalIdAndQuarter(linked_goal.getId(), quarter)
                        .orElseGet(() -> Achievement.builder().goal(linked_goal).quarter(quarter).cycle(cycle).loggedBy(user).build());
                linkedAch.setActualValue(req.getActualValue());
                linkedAch.setActualDate(req.getActualDate());
                linkedAch.setStatus(req.getStatus());
                linkedAch.setComputedScore(computeScore(linked_goal, req));
                achievementRepository.save(linkedAch);
            }
        }

        return saved;
    }

    private BigDecimal computeScore(Goal goal, AchievementRequest req) {
        return switch (goal.getUom()) {
            case NUMERIC -> {
                if (goal.getTargetValue() == null || goal.getTargetValue().compareTo(BigDecimal.ZERO) == 0)
                    yield BigDecimal.ZERO;
                yield req.getActualValue().divide(goal.getTargetValue(), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
            case PERCENTAGE -> {
                if (goal.getTargetValue() == null || goal.getTargetValue().compareTo(BigDecimal.ZERO) == 0)
                    yield BigDecimal.ZERO;
                yield req.getActualValue().divide(goal.getTargetValue(), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
            case TIMELINE -> {
                if (req.getActualDate() == null || goal.getTargetDate() == null) yield BigDecimal.ZERO;
                yield !req.getActualDate().isAfter(goal.getTargetDate()) ? new BigDecimal("100") : BigDecimal.ZERO;
            }
            case ZERO_BASED -> req.getStatus() == AchievementStatus.COMPLETED ? new BigDecimal("100") : BigDecimal.ZERO;
        };
    }

    public List<Achievement> getAchievementsForGoal(UUID goalId) {
        return achievementRepository.findByGoalId(goalId);
    }
}
