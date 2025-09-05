package com.example.ad_manager.model.dto;

import lombok.Builder;

@Builder
public record CreativeCreateReqDto(
    String name,
    String imageUrl,
    String clickUrl,
    Integer width,
    Integer height
) {

}
