package com.atomquest.goalportal.repository;

import com.atomquest.goalportal.entity.GoalSheet;
import com.atomquest.goalportal.enums.GoalSheetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalSheetRepository extends JpaRepository<GoalSheet, UUID> {
    Optional<GoalSheet> findByEmployeeIdAndCycleId(UUID employeeId, UUID cycleId);
    List<GoalSheet> findByEmployeeId(UUID employeeId);
    List<GoalSheet> findByStatus(GoalSheetStatus status);

    @Query("SELECT gs FROM GoalSheet gs WHERE gs.employee.manager.id = :managerId AND gs.status = :status")
    List<GoalSheet> findByManagerIdAndStatus(UUID managerId, GoalSheetStatus status);

    @Query("SELECT gs FROM GoalSheet gs WHERE gs.employee.manager.id = :managerId")
    List<GoalSheet> findByManagerId(UUID managerId);
}
