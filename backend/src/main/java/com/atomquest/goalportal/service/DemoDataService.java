package com.atomquest.goalportal.service;

import com.atomquest.goalportal.entity.*;
import com.atomquest.goalportal.enums.*;
import com.atomquest.goalportal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DemoDataService {

    private final UserRepository userRepository;
    private final ThrustAreaRepository thrustAreaRepository;
    private final CycleRepository cycleRepository;
    private final CheckinWindowRepository checkinWindowRepository;
    private final GoalSheetRepository goalSheetRepository;
    private final GoalRepository goalRepository;
    private final AchievementRepository achievementRepository;
    private final CheckinCommentRepository checkinCommentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Map<String, String> seedDemoData() {
        // Clean slate
        checkinCommentRepository.deleteAll();
        achievementRepository.deleteAll();
        goalRepository.deleteAll();
        goalSheetRepository.deleteAll();
        checkinWindowRepository.deleteAll();
        cycleRepository.deleteAll();
        thrustAreaRepository.deleteAll();
        userRepository.deleteAll();

        // Thrust Areas
        ThrustArea innovation = thrustAreaRepository.save(ThrustArea.builder().name("Innovation").description("Drive innovation").isActive(true).build());
        ThrustArea revenue = thrustAreaRepository.save(ThrustArea.builder().name("Revenue Growth").description("Revenue targets").isActive(true).build());
        ThrustArea quality = thrustAreaRepository.save(ThrustArea.builder().name("Quality").description("Product quality").isActive(true).build());
        ThrustArea customer = thrustAreaRepository.save(ThrustArea.builder().name("Customer Success").description("CSAT targets").isActive(true).build());

        // Cycle
        Cycle cycle = cycleRepository.save(Cycle.builder()
                .year(2026).startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31)).isActive(true).build());

        // Check-in Windows
        checkinWindowRepository.save(CheckinWindow.builder().cycle(cycle).quarter(Quarter.Q1).openDate(LocalDate.of(2026, 4, 1)).closeDate(LocalDate.of(2026, 4, 15)).build());
        checkinWindowRepository.save(CheckinWindow.builder().cycle(cycle).quarter(Quarter.Q2).openDate(LocalDate.of(2026, 7, 1)).closeDate(LocalDate.of(2026, 7, 15)).build());
        checkinWindowRepository.save(CheckinWindow.builder().cycle(cycle).quarter(Quarter.Q3).openDate(LocalDate.of(2026, 10, 1)).closeDate(LocalDate.of(2026, 10, 15)).build());
        checkinWindowRepository.save(CheckinWindow.builder().cycle(cycle).quarter(Quarter.Q4).openDate(LocalDate.of(2027, 1, 1)).closeDate(LocalDate.of(2027, 1, 15)).build());

        // Users
        User admin = userRepository.save(User.builder().name("Admin HR").email("admin@atomquest.com")
                .passwordHash(passwordEncoder.encode("Admin@123")).role(Role.ADMIN)
                .department("HR").isActive(true).build());

        User manager = userRepository.save(User.builder().name("Rahul Sharma").email("manager@atomquest.com")
                .passwordHash(passwordEncoder.encode("Manager@123")).role(Role.MANAGER)
                .department("Engineering").isActive(true).build());

        User priya = userRepository.save(User.builder().name("Priya Singh").email("priya@atomquest.com")
                .passwordHash(passwordEncoder.encode("Employee@123")).role(Role.EMPLOYEE)
                .manager(manager).department("Engineering").isActive(true).build());

        User raj = userRepository.save(User.builder().name("Raj Patel").email("raj@atomquest.com")
                .passwordHash(passwordEncoder.encode("Employee@123")).role(Role.EMPLOYEE)
                .manager(manager).department("Engineering").isActive(true).build());

        User anita = userRepository.save(User.builder().name("Anita Desai").email("anita@atomquest.com")
                .passwordHash(passwordEncoder.encode("Employee@123")).role(Role.EMPLOYEE)
                .manager(manager).department("Product").isActive(true).build());

        // Priya's Goal Sheet — APPROVED & LOCKED
        GoalSheet priyaSheet = goalSheetRepository.save(GoalSheet.builder()
                .employee(priya).cycle(cycle).status(GoalSheetStatus.APPROVED)
                .submittedAt(LocalDateTime.now().minusDays(30))
                .approvedAt(LocalDateTime.now().minusDays(28))
                .approvedBy(manager).locked(true).build());

        Goal priyaGoal1 = goalRepository.save(Goal.builder().goalSheet(priyaSheet).thrustArea(quality)
                .title("Reduce Bug Count by 40%").description("Reduce production bugs from 50 to 30 per sprint")
                .uom(UoM.NUMERIC).targetValue(new BigDecimal("30")).weightage(new BigDecimal("40")).build());

        Goal priyaGoal2 = goalRepository.save(Goal.builder().goalSheet(priyaSheet).thrustArea(customer)
                .title("Improve CSAT Score to 90%").description("Customer satisfaction from 78% to 90%")
                .uom(UoM.PERCENTAGE).targetValue(new BigDecimal("90")).weightage(new BigDecimal("35")).build());

        Goal priyaGoal3 = goalRepository.save(Goal.builder().goalSheet(priyaSheet).thrustArea(innovation)
                .title("Launch AI-Powered Feature").description("Ship ML recommendation engine")
                .uom(UoM.ZERO_BASED).weightage(new BigDecimal("25")).build());

        // Priya Q1 Achievements
        achievementRepository.save(Achievement.builder().goal(priyaGoal1).quarter(Quarter.Q1).cycle(cycle)
                .actualValue(new BigDecimal("38")).status(AchievementStatus.ON_TRACK)
                .computedScore(new BigDecimal("78.95")).loggedBy(priya).build());
        achievementRepository.save(Achievement.builder().goal(priyaGoal2).quarter(Quarter.Q1).cycle(cycle)
                .actualValue(new BigDecimal("78")).status(AchievementStatus.ON_TRACK)
                .computedScore(new BigDecimal("86.67")).loggedBy(priya).build());

        // Q1 Manager checkin
        checkinCommentRepository.save(CheckinComment.builder().goalSheet(priyaSheet).manager(manager)
                .quarter(Quarter.Q1)
                .comment("Priya demonstrated strong execution on Bug Reduction (78.95%). CSAT improvement needs attention — currently 78% vs 90% target. Recommend focusing on customer interaction quality in Q2.").build());

        // Raj's Goal Sheet — SUBMITTED
        GoalSheet rajSheet = goalSheetRepository.save(GoalSheet.builder()
                .employee(raj).cycle(cycle).status(GoalSheetStatus.SUBMITTED)
                .submittedAt(LocalDateTime.now().minusDays(2)).locked(false).build());

        goalRepository.save(Goal.builder().goalSheet(rajSheet).thrustArea(revenue)
                .title("Increase Feature Releases by 50%").description("Ship 15 features this year vs 10 last year")
                .uom(UoM.NUMERIC).targetValue(new BigDecimal("15")).weightage(new BigDecimal("60")).build());
        goalRepository.save(Goal.builder().goalSheet(rajSheet).thrustArea(innovation)
                .title("Implement CI/CD Pipeline").description("Automate deployment pipeline")
                .uom(UoM.ZERO_BASED).weightage(new BigDecimal("40")).build());

        // Anita's Goal Sheet — DRAFT
        GoalSheet anitaSheet = goalSheetRepository.save(GoalSheet.builder()
                .employee(anita).cycle(cycle).status(GoalSheetStatus.DRAFT).locked(false).build());

        goalRepository.save(Goal.builder().goalSheet(anitaSheet).thrustArea(customer)
                .title("Launch 2 Product Features").description("Ship 2 major product features by Q3")
                .uom(UoM.NUMERIC).targetValue(new BigDecimal("2")).weightage(new BigDecimal("100")).build());

        return Map.of(
            "status", "Demo data loaded successfully!",
            "admin", "admin@atomquest.com / Admin@123",
            "manager", "manager@atomquest.com / Manager@123",
            "employee1", "priya@atomquest.com / Employee@123",
            "employee2", "raj@atomquest.com / Employee@123",
            "employee3", "anita@atomquest.com / Employee@123"
        );
    }
}
