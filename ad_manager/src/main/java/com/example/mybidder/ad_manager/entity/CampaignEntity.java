package com.example.mybidder.ad_manager.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity(name = "campaign")
@EntityListeners(AuditingEntityListener.class)
public class CampaignEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal targetCpm;

    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal budget;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    private boolean active;

    private String owner;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;


    @Builder
    public CampaignEntity(String name, String region, BigDecimal targetCpm, BigDecimal budget, LocalDateTime startDate,
                          LocalDateTime endDate, boolean active, String owner) {
        this.name = name;
        this.region = region;
        this.targetCpm = targetCpm;
        this.budget = budget;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
        this.owner = owner;
    }
}
