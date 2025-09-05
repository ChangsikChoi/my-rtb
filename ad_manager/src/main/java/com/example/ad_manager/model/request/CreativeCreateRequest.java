package com.example.ad_manager.model.request;

public record CreativeCreateRequest(
    String name,
    String imageUrl,
    String clickUrl,
    Integer width,
    Integer height
) {

}