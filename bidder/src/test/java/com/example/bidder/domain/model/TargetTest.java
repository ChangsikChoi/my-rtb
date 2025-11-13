package com.example.bidder.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TargetTest {

  @Test
  void allFieldsNull_returnTrue() {
    Target target = Target.builder()
        .build();
    BidRequest bidRequest = BidRequest.builder()
        .device(Device.builder()
            .os("iOS")
            .country("US")
            .build())
        .user(User.builder()
            .age(25)
            .gender(Gender.OTHER)
            .build())
        .build();

    assertTrue(target.isTargeted(bidRequest));
  }

  @Nested
  class AgeTest {

    @Test
    void age_belowMinAge_returnFalse() {
      Target target = Target.builder()
          .minAge(18)
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .user(User.builder()
              .age(16)
              .build())
          .build();

      assertFalse(target.isTargeted(bidRequest));
    }

    @Test
    void age_aboveMaxAge_returnFalse() {
      Target target = Target.builder()
          .maxAge(30)
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .user(User.builder()
              .age(35)
              .build())
          .build();

      assertFalse(target.isTargeted(bidRequest));
    }

    @Test
    void age_withinRange_returnTrue() {
      Target target = Target.builder()
          .minAge(18)
          .maxAge(30)
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .user(User.builder()
              .age(25)
              .build())
          .build();

      assertTrue(target.isTargeted(bidRequest));
    }

    @Test
    void age_targetMinAgeExistButRequestIsNull_returnFalse() {
      Target target = Target.builder()
          .minAge(18)
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .user(User.builder()
              .age(null)
              .build())
          .build();

      assertFalse(target.isTargeted(bidRequest));
    }

    @Test
    void age_targetMaxAgeExistButRequestIsNull_returnFalse() {
      Target target = Target.builder()
          .maxAge(30)
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .user(User.builder()
              .age(null)
              .build())
          .build();

      assertFalse(target.isTargeted(bidRequest));
    }
  }

  @Nested
  class CountryTest {

    @Test
    void country_notMatched_returnFalse() {
      Target target = Target.builder()
          .country("US")
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .device(Device.builder()
              .country("JP")
              .build())
          .build();

      assertFalse(target.isTargeted(bidRequest));
    }

    @Test
    void country_targetExistButRequestIsNull_returnFalse() {
      Target target = Target.builder()
          .country("US")
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .device(Device.builder()
              .country(null)
              .build())
          .build();

      assertFalse(target.isTargeted(bidRequest));
    }

    @Test
    void country_matched_returnTrue() {
      Target target = Target.builder()
          .country("US")
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .device(Device.builder()
              .country("US")
              .build())
          .build();

      assertTrue(target.isTargeted(bidRequest));
    }

  }

  @Nested
  class GenderTest {

    @Test
    void gender_notMatched_returnFalse() {
      Target target = Target.builder()
          .gender(Gender.MALE)
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .user(User.builder()
              .gender(Gender.FEMALE)
              .build())
          .build();

      assertFalse(target.isTargeted(bidRequest));
    }

    @Test
    void gender_targetExistButRequestIsNull_returnFalse() {
      Target target = Target.builder()
          .gender(Gender.MALE)
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .user(User.builder()
              .gender(null)
              .build())
          .build();

      assertFalse(target.isTargeted(bidRequest));
    }

    @Test
    void gender_matched_returnTrue() {
      Target target = Target.builder()
          .gender(Gender.MALE)
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .user(User.builder()
              .gender(Gender.MALE)
              .build())
          .build();

      assertTrue(target.isTargeted(bidRequest));
    }
  }

  @Nested
  class OsTest {

    @Test
    void os_notMatched_returnFalse() {
      Target target = Target.builder()
          .os("iOS")
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .device(Device.builder()
              .os("Android")
              .build())
          .build();

      assertFalse(target.isTargeted(bidRequest));
    }

    @Test
    void os_targetExistButRequestIsNull_returnFalse() {
      Target target = Target.builder()
          .os("iOS")
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .device(Device.builder()
              .os(null)
              .build())
          .build();

      assertFalse(target.isTargeted(bidRequest));
    }

    @Test
    void os_matched_returnTrue() {
      Target target = Target.builder()
          .os("iOS")
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .device(Device.builder()
              .os("iOS")
              .build())
          .build();

      assertTrue(target.isTargeted(bidRequest));
    }

  }
}