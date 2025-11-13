package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record User (
    Gender gender,
    Integer age
){
}
