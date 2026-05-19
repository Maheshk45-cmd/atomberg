package com.atomquest.goalportal.repository;

import com.atomquest.goalportal.entity.Cycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CycleRepository extends JpaRepository<Cycle, UUID> {
    Optional<Cycle> findByIsActiveTrue();
}
