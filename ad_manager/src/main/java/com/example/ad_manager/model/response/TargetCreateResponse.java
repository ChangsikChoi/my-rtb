package com.example.ad_manager.model.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record TargetCreateResponse(
    String id,
    String os,
    String country,
    Integer minAge,
    Integer maxAge,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
