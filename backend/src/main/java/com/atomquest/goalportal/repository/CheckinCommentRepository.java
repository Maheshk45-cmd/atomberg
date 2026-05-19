package com.atomquest.goalportal.repository;

import com.atomquest.goalportal.entity.CheckinComment;
import com.atomquest.goalportal.enums.Quarter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckinCommentRepository extends JpaRepository<CheckinComment, UUID> {
    List<CheckinComment> findByGoalSheetId(UUID goalSheetId);
    Optional<CheckinComment> findByGoalSheetIdAndQuarter(UUID goalSheetId, Quarter quarter);
    boolean existsByGoalSheetIdAndQuarter(UUID goalSheetId, Quarter quarter);
}
