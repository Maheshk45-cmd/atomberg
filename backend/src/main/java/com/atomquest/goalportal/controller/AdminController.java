package com.atomquest.goalportal.controller;

import com.atomquest.goalportal.dto.CycleRequest;
import com.atomquest.goalportal.dto.ThrustAreaRequest;
import com.atomquest.goalportal.entity.Cycle;
import com.atomquest.goalportal.entity.ThrustArea;
import com.atomquest.goalportal.service.AdminConfigService;
import com.atomquest.goalportal.service.DemoDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DemoDataService demoDataService;
    private final AdminConfigService adminConfigService;

    @PostMapping("/seed-demo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> seedDemo() {
        return ResponseEntity.ok(demoDataService.seedDemoData());
    }

    // ─── Thrust Area Management ───────────────────────────────────────────────

    @GetMapping("/thrust-areas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ThrustArea>> getAllThrustAreas() {
        return ResponseEntity.ok(adminConfigService.getAllThrustAreas());
    }

    @PostMapping("/thrust-areas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ThrustArea> createThrustArea(@RequestBody ThrustAreaRequest req) {
        return ResponseEntity.ok(adminConfigService.createThrustArea(req));
    }

    @PutMapping("/thrust-areas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ThrustArea> updateThrustArea(@PathVariable UUID id,
                                                        @RequestBody ThrustAreaRequest req) {
        return ResponseEntity.ok(adminConfigService.updateThrustArea(id, req));
    }

    // ─── Cycle Management ─────────────────────────────────────────────────────

    @GetMapping("/cycles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Cycle>> getAllCycles() {
        return ResponseEntity.ok(adminConfigService.getAllCycles());
    }

    @PostMapping("/cycles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Cycle> createCycle(@RequestBody CycleRequest req) {
        return ResponseEntity.ok(adminConfigService.createCycle(req));
    }

    @PutMapping("/cycles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Cycle> updateCycle(@PathVariable UUID id,
                                              @RequestBody CycleRequest req) {
        return ResponseEntity.ok(adminConfigService.updateCycle(id, req));
    }
}
