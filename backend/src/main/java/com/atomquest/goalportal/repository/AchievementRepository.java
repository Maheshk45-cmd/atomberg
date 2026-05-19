package com.atomquest.goalportal.repository;

import com.atomquest.goalportal.entity.Achievement;
import com.atomquest.goalportal.enums.Quarter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AchievementRepository extends JpaRepository<Achievement, UUID> {
    List<Achievement> findByGoalId(UUID goalId);
    Optional<Achievement> findByGoalIdAndQuarter(UUID goalId, Quarter quarter);
    List<Achievement> findByGoalGoalSheetId(UUID goalSheetId);

    // [PERFORMANCE] Bulk fetch with eager goal join — avoids lazy-load proxy triggers during report index building
    @org.springframework.data.jpa.repository.Query(
        "SELECT a FROM Achievement a JOIN FETCH a.goal WHERE a.goal.id IN :goalIds"
    )
    List<Achievement> findByGoalIdIn(@org.springframework.data.repository.query.Param("goalIds") List<UUID> goalIds);
}
