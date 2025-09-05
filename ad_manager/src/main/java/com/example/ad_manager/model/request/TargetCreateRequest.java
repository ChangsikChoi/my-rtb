package com.example.ad_manager.model.request;

public record TargetCreateRequest(
    String os,
    String country,
    Integer minAge,
    Integer maxAge
) {

}
