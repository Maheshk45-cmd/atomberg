package com.atomquest.goalportal.repository;

import com.atomquest.goalportal.entity.User;
import com.atomquest.goalportal.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByManagerId(UUID managerId);
    List<User> findByDepartment(String department);
    boolean existsByEmail(String email);
}
