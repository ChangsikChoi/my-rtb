package com.example.bidder.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CreativeTest {

  @Nested
  class WidthTest {
    @Test
    void width_requestExistButCreativeNot_returnFalse() {
      Creative creative = Creative.builder().build();
      BidRequest bidRequest = BidRequest.builder()
          .imp(Imp.builder()
              .width(300)
              .build())
          .build();

      assertFalse(creative.isSizeMatched(bidRequest));
    }

    @Test
    void width_requestExistButNotMatched_returnFalse() {
      Creative creative = Creative.builder()
          .width(320)
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .imp(Imp.builder()
              .width(300)
              .build())
          .build();

      assertFalse(creative.isSizeMatched(bidRequest));
    }

    @Test
    void width_creativeExistButRequestNot_returnFalse() {
      Creative creative = Creative.builder()
          .width(320)
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .imp(Imp.builder()
              .build())
          .build();

      assertFalse(creative.isSizeMatched(bidRequest));
    }

    @Test
    void width_matched_returnTrue() {
      Creative creative = Creative.builder()
          .width(320)
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .imp(Imp.builder()
              .width(320)
              .build())
          .build();

      assertTrue(creative.isSizeMatched(bidRequest));
    }
  }

  @Nested
  class HeightTest {
    @Test
    void height_requestExistButCreativeNot_returnFalse() {
      Creative creative = Creative.builder()
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .imp(Imp.builder()
              .height(100)
              .build())
          .build();

      assertFalse(creative.isSizeMatched(bidRequest));
    }

    @Test
    void height_requestExistButNotMatched_returnFalse() {
      Creative creative = Creative.builder()
          .height(150)
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .imp(Imp.builder()
              .height(100)
              .build())
          .build();

      assertFalse(creative.isSizeMatched(bidRequest));
    }

    @Test
    void height_creativeExistButRequestNot_returnFalse() {
      Creative creative = Creative.builder()
          .height(150)
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .imp(Imp.builder()
              .build())
          .build();

      assertFalse(creative.isSizeMatched(bidRequest));
    }

    @Test
    void height_matched_returnTrue() {
      Creative creative = Creative.builder()
          .height(150)
          .build();
      BidRequest bidRequest = BidRequest.builder()
          .imp(Imp.builder()
              .height(150)
              .build())
          .build();

      assertTrue(creative.isSizeMatched(bidRequest));
    }
  }

  @Test
  void widthAndHeight_bothMatched_returnTrue() {
    Creative creative = Creative.builder()
        .width(300)
        .height(250)
        .build();
    BidRequest bidRequest = BidRequest.builder()
        .imp(Imp.builder()
            .width(300)
            .height(250)
            .build())
        .build();

    assertTrue(creative.isSizeMatched(bidRequest));
  }

  @Test
  void widthAndHeight_bothNotMatched_returnFalse() {
    Creative creative = Creative.builder()
        .width(300)
        .height(250)
        .build();
    BidRequest bidRequest = BidRequest.builder()
        .imp(Imp.builder()
            .width(320)
            .height(200)
            .build())
        .build();

    assertFalse(creative.isSizeMatched(bidRequest));
  }

  @Test
  void widthAndHeight_widthNotMatched_returnFalse() {
    Creative creative = Creative.builder()
        .width(300)
        .height(200)
        .build();
    BidRequest bidRequest = BidRequest.builder()
        .imp(Imp.builder()
            .width(320)
            .height(200)
            .build())
        .build();

    assertFalse(creative.isSizeMatched(bidRequest));
  }

  @Test
  void widthAndHeight_heightNotMatched_returnFalse() {
    Creative creative = Creative.builder()
        .width(300)
        .height(250)
        .build();
    BidRequest bidRequest = BidRequest.builder()
        .imp(Imp.builder()
            .width(300)
            .height(200)
            .build())
        .build();

    assertFalse(creative.isSizeMatched(bidRequest));
  }

}