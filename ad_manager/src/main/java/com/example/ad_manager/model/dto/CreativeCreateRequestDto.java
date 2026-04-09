package com.example.ad_manager.model.dto;

import lombok.Builder;

@Builder
public record CreativeCreateRequestDto(
    String name,
    String imageUrl,
    String clickUrl,
    Integer width,
    Integer height
) {

}
