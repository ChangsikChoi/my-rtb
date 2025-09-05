package com.example.ad_manager.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity(name = "target")
@EntityListeners(AuditingEntityListener.class)
public class TargetEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  private String os;

  private String country;

  private Integer minAge;

  private Integer maxAge;

  @CreatedDate
  private LocalDateTime createdAt;
  @LastModifiedDate
  private LocalDateTime updatedAt;


  @Builder
  public TargetEntity(String os, String country, Integer minAge, Integer maxAge) {
    this.os = os;
    this.country = country;
    this.minAge = minAge;
    this.maxAge = maxAge;
  }
}
