package com.example.ad_manager.model.dto;

import lombok.Builder;

@Builder
public record TargetCreateRequestDto(
    String os,
    String country,
    Integer minAge,
    Integer maxAge
) {

}
