package com.atomquest.goalportal.service;

import com.atomquest.goalportal.dto.CheckinCommentRequest;
import com.atomquest.goalportal.entity.*;
import com.atomquest.goalportal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CheckinService {

    private final CheckinCommentRepository checkinCommentRepository;
    private final GoalSheetRepository goalSheetRepository;
    private final UserRepository userRepository;

    @Transactional
    public CheckinComment saveComment(UUID sheetId, CheckinCommentRequest req, UUID managerId) {
        GoalSheet sheet = goalSheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sheet not found"));
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manager not found"));

        // [SECURITY] Manager can only comment on their own team's sheets
        if (sheet.getEmployee().getManager() == null ||
                !sheet.getEmployee().getManager().getId().equals(managerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only add check-in comments for your own team members");

        CheckinComment comment = checkinCommentRepository.findByGoalSheetIdAndQuarter(sheetId, req.getQuarter())
                .orElseGet(() -> CheckinComment.builder()
                        .goalSheet(sheet).manager(manager).quarter(req.getQuarter()).build());
        comment.setComment(req.getComment());
        return checkinCommentRepository.save(comment);
    }

    public List<CheckinComment> getCommentsForSheet(UUID sheetId) {
        return checkinCommentRepository.findByGoalSheetId(sheetId);
    }

    // Returns checkin completion status for all sheets under a manager
    public Map<String, Object> getCompletionDashboard(UUID managerId) {
        List<GoalSheet> sheets = goalSheetRepository.findByManagerId(managerId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (GoalSheet sheet : sheets) {
            Map<String, Object> item = new HashMap<>();
            item.put("employeeId", sheet.getEmployee().getId());
            item.put("employeeName", sheet.getEmployee().getName());
            item.put("sheetId", sheet.getId());
            Map<String, Boolean> quarters = new HashMap<>();
            for (var q : com.atomquest.goalportal.enums.Quarter.values()) {
                quarters.put(q.name(), checkinCommentRepository.existsByGoalSheetIdAndQuarter(sheet.getId(), q));
            }
            item.put("quarters", quarters);
            result.add(item);
        }
        return Map.of("completionData", result);
    }
}
