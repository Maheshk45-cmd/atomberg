package com.atomquest.goalportal.service;

import com.atomquest.goalportal.entity.*;
import com.atomquest.goalportal.enums.Quarter;
import com.atomquest.goalportal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final GoalSheetRepository goalSheetRepository;
    private final GoalRepository goalRepository;
    private final AchievementRepository achievementRepository;
    private final AuditLogRepository auditLogRepository;

    public List<Map<String, Object>> buildAchievementReport() {
        List<GoalSheet> sheets = goalSheetRepository.findAll();
        List<Map<String, Object>> rows = new ArrayList<>();

        // [PERFORMANCE FIX] Collect all goal IDs first, then bulk-fetch all achievements in ONE query
        List<Goal> allGoals = sheets.stream()
                .flatMap(s -> goalRepository.findByGoalSheetId(s.getId()).stream())
                .toList();

        List<UUID> goalIds = allGoals.stream().map(Goal::getId).toList();

        // Single DB call instead of goalCount × 4 calls
        Map<String, Achievement> achievementIndex = achievementRepository.findByGoalIdIn(goalIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        a -> a.getGoal().getId() + ":" + a.getQuarter().name(),
                        a -> a,
                        (a1, a2) -> a1 // keep first if duplicate (shouldn't happen)
                ));

        // Build report rows using the in-memory index
        for (GoalSheet sheet : sheets) {
            List<Goal> goals = allGoals.stream()
                    .filter(g -> g.getGoalSheet().getId().equals(sheet.getId()))
                    .toList();
            for (Goal goal : goals) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("Employee", sheet.getEmployee().getName());
                row.put("Department", sheet.getEmployee().getDepartment());
                row.put("Goal Title", goal.getTitle());
                row.put("UoM", goal.getUom().name());
                row.put("Target", goal.getTargetValue());
                row.put("Weightage", goal.getWeightage());
                BigDecimal weighted = BigDecimal.ZERO;
                for (Quarter q : Quarter.values()) {
                    Achievement ach = achievementIndex.get(goal.getId() + ":" + q.name());
                    row.put(q.name() + " Actual", ach != null ? ach.getActualValue() : "-");
                    row.put(q.name() + " Score", ach != null ? ach.getComputedScore() : "-");
                    if (ach != null && ach.getComputedScore() != null) {
                        weighted = weighted.add(ach.getComputedScore()
                                .multiply(goal.getWeightage())
                                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP));
                    }
                }
                row.put("Weighted Score", weighted);
                rows.add(row);
            }
        }
        return rows;
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAllByOrderByChangedAtDesc();
    }

    public List<Map<String, Object>> getPredictiveScores(UUID employeeId) {
        List<GoalSheet> sheets = goalSheetRepository.findByEmployeeId(employeeId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (GoalSheet sheet : sheets) {
            List<Goal> goals = goalRepository.findByGoalSheetId(sheet.getId());
            for (Goal goal : goals) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("goalId", goal.getId());
                row.put("title", goal.getTitle());
                List<Double> scores = new ArrayList<>();
                for (Quarter q : Quarter.values()) {
                    Achievement ach = achievementRepository.findByGoalIdAndQuarter(goal.getId(), q).orElse(null);
                    scores.add(ach != null && ach.getComputedScore() != null ? ach.getComputedScore().doubleValue() : null);
                }
                row.put("scores", scores);
                List<Double> known = scores.stream().filter(Objects::nonNull).toList();
                if (known.size() >= 2) {
                    double trend = (known.get(known.size() - 1) - known.get(0)) / (known.size() - 1);
                    List<Double> projected = new ArrayList<>(known);
                    while (projected.size() < 4) {
                        projected.add(Math.min(100, projected.get(projected.size() - 1) + trend));
                    }
                    row.put("projected", projected);
                    row.put("status", projected.get(3) >= 80 ? "ON_TRACK" : "AT_RISK");
                } else {
                    row.put("projected", null);
                    row.put("status", "INSUFFICIENT_DATA");
                }
                result.add(row);
            }
        }
        return result;
    }
}
