package com.atomquest.goalportal.controller;

import com.atomquest.goalportal.dto.CheckinCommentRequest;
import com.atomquest.goalportal.entity.CheckinComment;
import com.atomquest.goalportal.repository.UserRepository;
import com.atomquest.goalportal.service.CheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;
    private final UserRepository userRepository;

    @PostMapping("/goal-sheets/{sheetId}/checkins")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<CheckinComment> saveComment(@PathVariable UUID sheetId,
                                                       @RequestBody CheckinCommentRequest req,
                                                       Authentication auth) {
        UUID managerId = userRepository.findByEmail(auth.getName()).orElseThrow().getId();
        return ResponseEntity.ok(checkinService.saveComment(sheetId, req, managerId));
    }

    @GetMapping("/goal-sheets/{sheetId}/checkins")
    public ResponseEntity<List<CheckinComment>> getComments(@PathVariable UUID sheetId) {
        return ResponseEntity.ok(checkinService.getCommentsForSheet(sheetId));
    }

    @GetMapping("/checkins/dashboard")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> dashboard(Authentication auth) {
        UUID managerId = userRepository.findByEmail(auth.getName()).orElseThrow().getId();
        return ResponseEntity.ok(checkinService.getCompletionDashboard(managerId));
    }
}
