package com.atomquest.goalportal.controller;

import com.atomquest.goalportal.entity.*;
import com.atomquest.goalportal.repository.*;
import com.atomquest.goalportal.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;
    private final ThrustAreaRepository thrustAreaRepository;

    @GetMapping("/reports/achievements")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> achievementReport() {
        return ResponseEntity.ok(reportService.buildAchievementReport());
    }

    @GetMapping("/reports/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportReport() {
        List<Map<String, Object>> data = reportService.buildAchievementReport();
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Achievement Report");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Employee");
            header.createCell(1).setCellValue("Manager");
            header.createCell(2).setCellValue("Goal Title");
            header.createCell(3).setCellValue("Status");
            header.createCell(4).setCellValue("Target Value");
            header.createCell(5).setCellValue("Q1 Actual");
            header.createCell(6).setCellValue("Q2 Actual");
            header.createCell(7).setCellValue("Q3 Actual");
            header.createCell(8).setCellValue("Q4 Actual");
            header.createCell(9).setCellValue("Total Score");

            int rowIdx = 1;
            for (Map<String, Object> rowMap : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(String.valueOf(rowMap.get("Employee Name")));
                row.createCell(1).setCellValue(String.valueOf(rowMap.get("Manager")));
                row.createCell(2).setCellValue(String.valueOf(rowMap.get("Goal Title")));
                row.createCell(3).setCellValue(String.valueOf(rowMap.get("Sheet Status")));
                row.createCell(4).setCellValue(String.valueOf(rowMap.get("Target Value")));
                row.createCell(5).setCellValue(String.valueOf(rowMap.get("Q1 Actual") != null ? rowMap.get("Q1 Actual") : ""));
                row.createCell(6).setCellValue(String.valueOf(rowMap.get("Q2 Actual") != null ? rowMap.get("Q2 Actual") : ""));
                row.createCell(7).setCellValue(String.valueOf(rowMap.get("Q3 Actual") != null ? rowMap.get("Q3 Actual") : ""));
                row.createCell(8).setCellValue(String.valueOf(rowMap.get("Q4 Actual") != null ? rowMap.get("Q4 Actual") : ""));
                row.createCell(9).setCellValue(String.valueOf(rowMap.get("Total Score")));
            }
            wb.write(out);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=report.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

    @GetMapping("/reports/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLog>> auditLogs() {
        return ResponseEntity.ok(reportService.getAuditLogs());
    }

    @GetMapping("/reports/predictive/{employeeId}")
    public ResponseEntity<List<Map<String, Object>>> predictive(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(reportService.getPredictiveScores(employeeId));
    }

    @GetMapping("/thrust-areas")
    public ResponseEntity<List<ThrustArea>> thrustAreas() {
        return ResponseEntity.ok(thrustAreaRepository.findByIsActiveTrue());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> allUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
}
