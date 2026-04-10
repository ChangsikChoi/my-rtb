package com.example.ad_manager.mapper.web;

import static com.example.ad_manager.fixture.CampaignTestFixtures.campaignCreateRequest;
import static com.example.ad_manager.fixture.CampaignTestFixtures.campaignResponse;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.ad_manager.model.dto.CampaignResponseDto;
import com.example.ad_manager.model.request.CampaignCreateRequest;
import com.example.ad_manager.model.response.CampaignResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CampaignWebMapperTest {

  private final CampaignWebMapper campaignWebMapper = new CampaignWebMapper();

  @Test
  void givenCampaignCreateRequest_whenCreateRequestToDto_thenMapAndNormalizeDates() {
    CampaignCreateRequest request = campaignCreateRequest("test-campaign-web");

    var result = campaignWebMapper.createRequestToDto(request);

    assertThat(result.name()).isEqualTo("test-campaign-web");
    assertThat(result.targetCpm()).isEqualByComparingTo("10.5");
    assertThat(result.budget()).isEqualByComparingTo("100.0");
    assertThat(result.startDate()).isEqualTo(LocalDateTime.of(2026, 3, 23, 0, 0, 0));
    assertThat(result.endDate()).isEqualTo(LocalDateTime.of(2026, 3, 24, 23, 59, 59));
    assertThat(result.target().os()).isEqualTo("Android");
    assertThat(result.target().country()).isEqualTo("KR");
    assertThat(result.target().minAge()).isEqualTo(20);
    assertThat(result.target().maxAge()).isEqualTo(40);
    assertThat(result.creative().name()).isEqualTo("test-creative-success");
    assertThat(result.creative().imageUrl()).isEqualTo("https://example.com/a.png");
    assertThat(result.creative().clickUrl()).isEqualTo("https://example.com");
    assertThat(result.creative().width()).isEqualTo(300);
    assertThat(result.creative().height()).isEqualTo(250);
  }

  @Test
  void givenCampaignResponseDto_whenResponseDtoToResponse_thenMapAndConvertDates() {
    CampaignResponseDto responseDto = campaignResponse("campaign-1", "test-campaign-web", true);

    CampaignResponse result = campaignWebMapper.responseDtoToResponse(responseDto);

    assertThat(result.id()).isEqualTo("campaign-1");
    assertThat(result.name()).isEqualTo("test-campaign-web");
    assertThat(result.targetCpm()).isEqualByComparingTo("10.5");
    assertThat(result.budget()).isEqualByComparingTo("100.0");
    assertThat(result.startDate()).isEqualTo(LocalDate.of(2026, 3, 23));
    assertThat(result.endDate()).isEqualTo(LocalDate.of(2026, 3, 24));
    assertThat(result.active()).isTrue();
    assertThat(result.target().id()).isEqualTo("target-1");
    assertThat(result.target().os()).isEqualTo("Android");
    assertThat(result.creative().id()).isEqualTo("creative-1");
    assertThat(result.creative().name()).isEqualTo("test-creative-success");
    assertThat(result.createdAt()).isEqualTo(LocalDateTime.of(2026, 3, 22, 10, 0, 0));
    assertThat(result.updatedAt()).isEqualTo(LocalDateTime.of(2026, 3, 22, 10, 0, 0));
  }
}
