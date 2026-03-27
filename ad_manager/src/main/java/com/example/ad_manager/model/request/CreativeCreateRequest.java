package com.example.ad_manager.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreativeCreateRequest(
    @NotBlank(message = "creative.name is required")
    String name,
    @NotBlank(message = "creative.imageUrl is required")
    String imageUrl,
    @NotBlank(message = "creative.clickUrl is required")
    String clickUrl,
    @Positive(message = "creative.width must be positive")
    Integer width,
    @Positive(message = "creative.height must be positive")
    Integer height
) {

}
