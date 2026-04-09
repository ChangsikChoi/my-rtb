package com.example.ad_manager.model.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record TargetResponseDto(
    String id,
    String os,
    String country,
    Integer minAge,
    Integer maxAge,
    LocalDateTime createdAt,
    LocalDateTime updatedAt

) {

}
