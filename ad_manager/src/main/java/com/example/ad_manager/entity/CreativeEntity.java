package com.example.ad_manager.entity;

import jakarta.persistence.Column;
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
@Entity(name = "creative")
@EntityListeners(AuditingEntityListener.class)
public class CreativeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String imageUrl;

  @Column(nullable = false)
  private String clickUrl;

  private Integer width;

  private Integer height;

  @CreatedDate
  private LocalDateTime createdAt;
  @LastModifiedDate
  private LocalDateTime updatedAt;

  @Builder
  public CreativeEntity(String name, String imageUrl, String clickUrl, Integer width,
      Integer height) {
    this.name = name;
    this.imageUrl = imageUrl;
    this.clickUrl = clickUrl;
    this.width = width;
    this.height = height;
  }
}
