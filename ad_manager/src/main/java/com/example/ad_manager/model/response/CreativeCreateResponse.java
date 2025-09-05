package com.example.ad_manager.model.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CreativeCreateResponse(
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