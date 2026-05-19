package com.atomquest.goalportal.entity;

import com.atomquest.goalportal.enums.UoM;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "goals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Goal {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_sheet_id", nullable = false)
    private GoalSheet goalSheet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thrust_area_id")
    private ThrustArea thrustArea;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UoM uom;

    private BigDecimal targetValue;
    private LocalDate targetDate;

    @Column(nullable = false)
    private BigDecimal weightage;

    @Column(nullable = false)
    private boolean isShared = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_from_id")
    private Goal sharedFrom;

    @Column(nullable = false)
    private boolean isReadonlyTitle = false;

    @Column(nullable = false)
    private boolean isReadonlyTarget = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
