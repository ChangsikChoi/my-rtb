package com.example.bidder.domain.model;

import lombok.Getter;

@Getter
public enum Gender {
  MALE("M"),
  FEMALE("F"),
  OTHER("O");

  private final String code;

  Gender(String code) { this.code = code; }

  public static Gender fromCode(String code) {
    for (Gender gender : Gender.values()) {
      if (gender.getCode().equalsIgnoreCase(code)) {
        return gender;
      }
    }
    return OTHER;
  }
}
