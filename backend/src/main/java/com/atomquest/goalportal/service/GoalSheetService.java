package com.atomquest.goalportal.service;

import com.atomquest.goalportal.dto.GoalRequest;
import com.atomquest.goalportal.entity.*;
import com.atomquest.goalportal.enums.*;
import com.atomquest.goalportal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GoalSheetService {

    private final GoalSheetRepository goalSheetRepository;
    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final CycleRepository cycleRepository;
    private final ThrustAreaRepository thrustAreaRepository;
    private final AuditLogRepository auditLogRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public GoalSheet getOrCreateSheet(UUID employeeId) {
        Cycle cycle = cycleRepository.findByIsActiveTrue()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active cycle"));
        return goalSheetRepository.findByEmployeeIdAndCycleId(employeeId, cycle.getId())
                .orElseGet(() -> {
                    User emp = userRepository.findById(employeeId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
                    if (emp.getManager() == null)
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No manager assigned. Admin must assign manager first.");
                    GoalSheet sheet = GoalSheet.builder()
                            .employee(emp).cycle(cycle)
                            .status(GoalSheetStatus.DRAFT).locked(false).build();
                    return goalSheetRepository.save(sheet);
                });
    }

    @Transactional
    public Goal addGoal(UUID sheetId, GoalRequest req, UUID userId) {
        GoalSheet sheet = goalSheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sheet not found"));
        // [SECURITY] Verify requesting employee owns this sheet
        if (!sheet.getEmployee().getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only add goals to your own sheet");
        if (sheet.getStatus() != GoalSheetStatus.DRAFT)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Goals can only be added in DRAFT state");
        long count = goalRepository.countByGoalSheetId(sheetId);
        if (count >= 8)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot exceed 8 goals");
        ThrustArea ta = thrustAreaRepository.findById(req.getThrustAreaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thrust area not found"));
        if (req.getWeightage().compareTo(new BigDecimal("10")) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum weightage per goal is 10%");
        }
        Goal goal = Goal.builder()
                .goalSheet(sheet).thrustArea(ta)
                .title(req.getTitle()).description(req.getDescription())
                .uom(req.getUom()).targetValue(req.getTargetValue())
                .targetDate(req.getTargetDate()).weightage(req.getWeightage())
                .isShared(false).isReadonlyTitle(false).isReadonlyTarget(false)
                .build();
        return goalRepository.save(goal);
    }

    @Transactional
    public GoalSheet submitSheet(UUID sheetId, UUID employeeId) {
        GoalSheet sheet = goalSheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sheet not found"));
        // [SECURITY] Verify requesting employee owns this sheet
        if (!sheet.getEmployee().getId().equals(employeeId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only submit your own sheet");
        if (sheet.getStatus() != GoalSheetStatus.DRAFT && sheet.getStatus() != GoalSheetStatus.RETURNED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sheet must be in DRAFT or RETURNED state to submit");
        List<Goal> goals = goalRepository.findByGoalSheetId(sheetId);
        if (goals.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No goals defined");
        BigDecimal total = goals.stream().map(Goal::getWeightage).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(new BigDecimal("100")) != 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total weightage must be 100%. Current: " + total + "%");
        sheet.setStatus(GoalSheetStatus.SUBMITTED);
        sheet.setSubmittedAt(LocalDateTime.now());
        GoalSheet saved = goalSheetRepository.save(sheet);
        // WebSocket notification to manager
        if (sheet.getEmployee().getManager() != null) {
            messagingTemplate.convertAndSend(
                "/topic/manager/" + sheet.getEmployee().getManager().getId(),
                Map.of("type", "GOAL_SUBMITTED", "employeeName", sheet.getEmployee().getName(),
                        "sheetId", sheetId.toString())
            );
        }
        return saved;
    }

    @Transactional
    public GoalSheet approveSheet(UUID sheetId, UUID managerId) {
        GoalSheet sheet = goalSheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sheet not found"));
        if (sheet.getStatus() != GoalSheetStatus.SUBMITTED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sheet must be SUBMITTED to approve");
        // [SECURITY] Verify approving manager is actually this employee's manager
        if (sheet.getEmployee().getManager() == null ||
                !sheet.getEmployee().getManager().getId().equals(managerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the manager of this employee");
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manager not found"));
        sheet.setStatus(GoalSheetStatus.APPROVED);
        sheet.setApprovedAt(LocalDateTime.now());
        sheet.setApprovedBy(manager);
        sheet.setLocked(true);
        return goalSheetRepository.save(sheet);
    }

    @Transactional
    public GoalSheet returnSheet(UUID sheetId, UUID managerId) {
        GoalSheet sheet = goalSheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sheet not found"));
        if (sheet.getStatus() != GoalSheetStatus.SUBMITTED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only SUBMITTED sheets can be returned");
        // [SECURITY] Verify returning manager is actually this employee's manager
        if (sheet.getEmployee().getManager() == null ||
                !sheet.getEmployee().getManager().getId().equals(managerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the manager of this employee");
        sheet.setStatus(GoalSheetStatus.RETURNED);
        // [BUSINESS LOGIC] Clear submittedAt so re-submission timestamp is accurate
        sheet.setSubmittedAt(null);
        return goalSheetRepository.save(sheet);
    }

    @Transactional
    public Goal updateGoalByManager(UUID goalId, GoalRequest req, UUID managerId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found"));
        if (goal.getGoalSheet().getStatus() == GoalSheetStatus.APPROVED)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sheet is locked — use admin override");
        // [SECURITY] Verify goal belongs to an employee under this manager
        User sheetEmployee = goal.getGoalSheet().getEmployee();
        if (sheetEmployee.getManager() == null || !sheetEmployee.getManager().getId().equals(managerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This goal does not belong to your team");
        if (req.getWeightage() != null && req.getWeightage().compareTo(new BigDecimal("10")) < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum weightage per goal is 10%");
        goal.setTargetValue(req.getTargetValue());
        goal.setWeightage(req.getWeightage());
        goal.setTargetDate(req.getTargetDate());
        return goalRepository.save(goal);
    }

    @Transactional
    public Goal updateLockedGoalByAdmin(UUID goalId, GoalRequest req, UUID adminId, String reason) {
        if (reason == null || reason.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin override requires a documented reason");
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found"));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        // [AUDIT] Log targetValue change if modified
        if (req.getTargetValue() != null && !req.getTargetValue().equals(goal.getTargetValue())) {
            auditLogRepository.save(AuditLog.builder()
                    .entityType("goal").entityId(goalId).fieldName("targetValue")
                    .oldValue(goal.getTargetValue() != null ? goal.getTargetValue().toString() : null)
                    .newValue(req.getTargetValue().toString())
                    .changedBy(admin).reason(reason).build());
        }
        // [AUDIT] Log weightage change if modified — was previously silently unaudited
        if (req.getWeightage() != null && !req.getWeightage().equals(goal.getWeightage())) {
            auditLogRepository.save(AuditLog.builder()
                    .entityType("goal").entityId(goalId).fieldName("weightage")
                    .oldValue(goal.getWeightage() != null ? goal.getWeightage().toString() : null)
                    .newValue(req.getWeightage().toString())
                    .changedBy(admin).reason(reason).build());
        }
        if (req.getTargetValue() != null) goal.setTargetValue(req.getTargetValue());
        if (req.getWeightage() != null) goal.setWeightage(req.getWeightage());
        return goalRepository.save(goal);
    }

    public List<GoalSheet> getSheetsForManager(UUID managerId) {
        return goalSheetRepository.findByManagerId(managerId);
    }

    // [ADMIN] Separate method for admin — fetches all sheets, not filtered by manager
    public List<GoalSheet> getAllSheets() {
        return goalSheetRepository.findAll();
    }

    public List<Goal> getGoalsForSheet(UUID sheetId, UUID requesterId, String requesterRole) {
        GoalSheet sheet = goalSheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sheet not found"));
        // [SECURITY] Employee can only see their own sheet's goals
        if ("ROLE_EMPLOYEE".equals(requesterRole) && !sheet.getEmployee().getId().equals(requesterId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        // [SECURITY] Manager can only see goals of their own team
        if ("ROLE_MANAGER".equals(requesterRole)) {
            User emp = sheet.getEmployee();
            if (emp.getManager() == null || !emp.getManager().getId().equals(requesterId))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This sheet does not belong to your team");
        }
        return goalRepository.findByGoalSheetId(sheetId);
    }
}
