package com.example.ad_manager.model.dto;

import lombok.Builder;

@Builder
public record TargetCreateReqDto(
    String os,
    String country,
    Integer minAge,
    Integer maxAge
) {

}