package com.example.ad_manager.model.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CreativeCreateResDto(
    String id,
    String name,
    String imageUrl,
    String clickUrl,
    Integer width,
    Integer height,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
