package com.example.ad_manager.entity;


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

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "target_id", referencedColumnName = "id")
  private TargetEntity target;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "creative_id", referencedColumnName = "id")
  private CreativeEntity creative;

  @Builder
  public CampaignEntity(String name, BigDecimal targetCpm, BigDecimal budget,
      LocalDateTime startDate, LocalDateTime endDate, boolean active, String owner,
      TargetEntity target, CreativeEntity creative) {
    this.name = name;
    this.targetCpm = targetCpm;
    this.budget = budget;
    this.startDate = startDate;
    this.endDate = endDate;
    this.active = active;
    this.owner = owner;
    this.target = target;
    this.creative = creative;
  }
}
