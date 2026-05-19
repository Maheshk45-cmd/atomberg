package com.atomquest.goalportal.repository;

import com.atomquest.goalportal.entity.ThrustArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ThrustAreaRepository extends JpaRepository<ThrustArea, UUID> {
    List<ThrustArea> findByIsActiveTrue();
}
