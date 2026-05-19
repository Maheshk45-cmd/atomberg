package com.atomquest.goalportal.repository;

import com.atomquest.goalportal.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {
    List<Goal> findByGoalSheetId(UUID goalSheetId);
    long countByGoalSheetId(UUID goalSheetId);
    List<Goal> findBySharedFromId(UUID sharedFromId);
}
