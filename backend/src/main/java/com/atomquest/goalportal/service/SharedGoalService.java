package com.atomquest.goalportal.service;

import com.atomquest.goalportal.dto.SharedGoalRequest;
import com.atomquest.goalportal.entity.*;
import com.atomquest.goalportal.enums.GoalSheetStatus;
import com.atomquest.goalportal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SharedGoalService {

    private final GoalRepository goalRepository;
    private final GoalSheetRepository goalSheetRepository;
    private final UserRepository userRepository;
    private final ThrustAreaRepository thrustAreaRepository;
    private final CycleRepository cycleRepository;

    @Transactional
    public List<Goal> pushSharedGoal(SharedGoalRequest req) {
        ThrustArea ta = thrustAreaRepository.findById(req.getThrustAreaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thrust area not found"));
        Cycle cycle = cycleRepository.findByIsActiveTrue()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active cycle"));

        List<Goal> createdGoals = new ArrayList<>();

        // Create primary shared goal (reference)
        Goal primaryGoal = Goal.builder()
                .thrustArea(ta).title(req.getTitle()).description(req.getDescription())
                .uom(req.getUom()).targetValue(req.getTargetValue()).targetDate(req.getTargetDate())
                .weightage(req.getMinWeightage()).isShared(true)
                .isReadonlyTitle(true).isReadonlyTarget(true)
                .build();

        for (UUID empId : req.getRecipientIds()) {
            User emp = userRepository.findById(empId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found: " + empId));
            GoalSheet sheet = goalSheetRepository.findByEmployeeIdAndCycleId(empId, cycle.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "No goal sheet found for employee: " + emp.getName()));

            if (sheet.isLocked())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Sheet is locked for: " + emp.getName());

            long count = goalRepository.countByGoalSheetId(sheet.getId());
            if (count >= 8)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Employee " + emp.getName() + " has reached max goal limit (8)");

            // [BUSINESS LOGIC] Validate min weightage per goal
            if (req.getMinWeightage() == null || req.getMinWeightage().compareTo(new java.math.BigDecimal("10")) < 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Minimum shared goal weightage is 10%");

            // [BUSINESS LOGIC] Validate total weightage won't exceed 100%
            java.math.BigDecimal currentTotal = goalRepository.findByGoalSheetId(sheet.getId()).stream()
                    .map(g -> g.getWeightage() != null ? g.getWeightage() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            if (currentTotal.add(req.getMinWeightage()).compareTo(new java.math.BigDecimal("100")) > 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Pushing this goal would exceed 100% total weightage for: " + emp.getName()
                        + " (current: " + currentTotal + "%)");

            Goal g = Goal.builder()
                    .goalSheet(sheet).thrustArea(ta)
                    .title(req.getTitle()).description(req.getDescription())
                    .uom(req.getUom()).targetValue(req.getTargetValue())
                    .targetDate(req.getTargetDate()).weightage(req.getMinWeightage())
                    .isShared(true).isReadonlyTitle(true).isReadonlyTarget(true)
                    .build();
            createdGoals.add(goalRepository.save(g));
        }
        return createdGoals;
    }
}
