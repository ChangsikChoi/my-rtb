package com.example.ad_manager.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record TargetCreateResponse(
    String id,
    String os,
    String country,
    Integer minAge,
    Integer maxAge,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt
) {

}
