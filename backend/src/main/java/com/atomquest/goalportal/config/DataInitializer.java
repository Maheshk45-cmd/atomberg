package com.atomquest.goalportal.config;

import com.atomquest.goalportal.entity.CheckinWindow;
import com.atomquest.goalportal.entity.Cycle;
import com.atomquest.goalportal.entity.ThrustArea;
import com.atomquest.goalportal.entity.User;
import com.atomquest.goalportal.enums.Quarter;
import com.atomquest.goalportal.enums.Role;
import com.atomquest.goalportal.repository.CheckinWindowRepository;
import com.atomquest.goalportal.repository.CycleRepository;
import com.atomquest.goalportal.repository.ThrustAreaRepository;
import com.atomquest.goalportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CycleRepository cycleRepository;
    private final ThrustAreaRepository thrustAreaRepository;
    private final CheckinWindowRepository checkinWindowRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (cycleRepository.count() == 0) {
            Cycle cycle = Cycle.builder()
                    .year(2026)
                    .startDate(LocalDate.of(2026, 1, 1))
                    .endDate(LocalDate.of(2026, 12, 31))
                    .isActive(true)
                    .build();
            cycleRepository.save(cycle);
        }

        // [FIX] Guard windows independently — cycle may exist but windows may be missing after data issues
        if (checkinWindowRepository.count() == 0) {
            Cycle activeCycle = cycleRepository.findByIsActiveTrue().orElse(null);
            if (activeCycle != null) {
                checkinWindowRepository.save(CheckinWindow.builder().cycle(activeCycle).quarter(Quarter.Q1).openDate(LocalDate.of(2026, 4, 1)).closeDate(LocalDate.of(2026, 4, 15)).build());
                checkinWindowRepository.save(CheckinWindow.builder().cycle(activeCycle).quarter(Quarter.Q2).openDate(LocalDate.of(2026, 7, 1)).closeDate(LocalDate.of(2026, 7, 15)).build());
                checkinWindowRepository.save(CheckinWindow.builder().cycle(activeCycle).quarter(Quarter.Q3).openDate(LocalDate.of(2026, 10, 1)).closeDate(LocalDate.of(2026, 10, 15)).build());
                checkinWindowRepository.save(CheckinWindow.builder().cycle(activeCycle).quarter(Quarter.Q4).openDate(LocalDate.of(2027, 1, 1)).closeDate(LocalDate.of(2027, 1, 15)).build());
                System.out.println("Data Initialized: Check-in windows seeded for active cycle.");
            }
        }

        if (thrustAreaRepository.count() == 0) {
            ThrustArea t1 = ThrustArea.builder().name("Customer Excellence").description("Improve customer satisfaction").build();
            ThrustArea t2 = ThrustArea.builder().name("Operational Efficiency").description("Optimize internal processes").build();
            ThrustArea t3 = ThrustArea.builder().name("Innovation").description("Develop new product features").build();
            thrustAreaRepository.save(t1);
            thrustAreaRepository.save(t2);
            thrustAreaRepository.save(t3);
        }

        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .name("Admin User")
                    .email("admin@atomquest.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role(Role.ADMIN)
                    .department("IT")
                    .isActive(true)
                    .build();
            userRepository.save(admin);

            User manager = User.builder()
                    .name("Manager User")
                    .email("manager@atomquest.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role(Role.MANAGER)
                    .department("Engineering")
                    .isActive(true)
                    .build();
            userRepository.save(manager);

            User employee = User.builder()
                    .name("Employee User")
                    .email("employee@atomquest.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role(Role.EMPLOYEE)
                    .manager(manager)
                    .department("Engineering")
                    .isActive(true)
                    .build();
            userRepository.save(employee);
            
            System.out.println("Data Initialized: Admin, Manager, and Employee users created!");
        }
    }
}
