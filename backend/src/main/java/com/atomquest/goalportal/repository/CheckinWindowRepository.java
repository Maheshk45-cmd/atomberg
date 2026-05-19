package com.atomquest.goalportal.repository;

import com.atomquest.goalportal.entity.CheckinWindow;
import com.atomquest.goalportal.enums.Quarter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckinWindowRepository extends JpaRepository<CheckinWindow, UUID> {
    Optional<CheckinWindow> findByCycleIdAndQuarter(UUID cycleId, Quarter quarter);
    List<CheckinWindow> findByCycleId(UUID cycleId);
}
