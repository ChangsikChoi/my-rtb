package com.example.ad_manager.mapper.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ad_manager.entity.CampaignEntity;
import com.example.ad_manager.entity.CreativeEntity;
import com.example.ad_manager.entity.TargetEntity;
import com.example.ad_manager.model.dto.CampaignCreateRequestDto;
import com.example.ad_manager.model.dto.CreativeCreateRequestDto;
import com.example.ad_manager.model.dto.TargetCreateRequestDto;
import com.example.ad_manager.model.dto.CampaignResponseDto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CampaignEntityMapperTest {

  private final CampaignEntityMapper campaignEntityMapper = new CampaignEntityMapper();

  @Test
  void givenCampaignCreateRequestDto_whenDtoToEntity_thenMapEntityAndNestedObjects() {
    CampaignCreateRequestDto requestDto = CampaignCreateRequestDto.builder()
        .name("test-campaign-entity")
        .targetCpm(BigDecimal.valueOf(10.5))
        .budget(BigDecimal.valueOf(100.0))
        .startDate(LocalDateTime.of(2026, 3, 23, 0, 0, 0))
        .endDate(LocalDateTime.of(2026, 3, 24, 23, 59, 59))
        .target(TargetCreateRequestDto.builder()
            .os("Android")
            .country("KR")
            .minAge(20)
            .maxAge(40)
            .build())
        .creative(CreativeCreateRequestDto.builder()
            .name("test-creative")
            .imageUrl("https://example.com/a.png")
            .clickUrl("https://example.com")
            .width(300)
            .height(250)
            .build())
        .build();

    CampaignEntity result = campaignEntityMapper.dtoToEntity(requestDto);

    assertThat(result.getName()).isEqualTo("test-campaign-entity");
    assertThat(result.getTargetCpm()).isEqualByComparingTo("10.5");
    assertThat(result.getBudget()).isEqualByComparingTo("100.0");
    assertThat(result.getStartDate()).isEqualTo(LocalDateTime.of(2026, 3, 23, 0, 0, 0));
    assertThat(result.getEndDate()).isEqualTo(LocalDateTime.of(2026, 3, 24, 23, 59, 59));
    assertThat(result.getTarget().getOs()).isEqualTo("Android");
    assertThat(result.getTarget().getCountry()).isEqualTo("KR");
    assertThat(result.getTarget().getMinAge()).isEqualTo(20);
    assertThat(result.getTarget().getMaxAge()).isEqualTo(40);
    assertThat(result.getCreative().getName()).isEqualTo("test-creative");
    assertThat(result.getCreative().getImageUrl()).isEqualTo("https://example.com/a.png");
    assertThat(result.getCreative().getClickUrl()).isEqualTo("https://example.com");
    assertThat(result.getCreative().getWidth()).isEqualTo(300);
    assertThat(result.getCreative().getHeight()).isEqualTo(250);
  }

  @Test
  void givenCampaignEntity_whenEntityToResponseDto_thenMapNestedDtos() {
    CampaignEntity entity = CampaignEntity.builder()
        .name("test-campaign-entity")
        .targetCpm(BigDecimal.valueOf(10.5))
        .budget(BigDecimal.valueOf(100.0))
        .startDate(LocalDateTime.of(2026, 3, 23, 0, 0, 0))
        .endDate(LocalDateTime.of(2026, 3, 24, 23, 59, 59))
        .active(true)
        .target(TargetEntity.builder()
            .os("Android")
            .country("KR")
            .minAge(20)
            .maxAge(40)
            .build())
        .creative(CreativeEntity.builder()
            .name("test-creative")
            .imageUrl("https://example.com/a.png")
            .clickUrl("https://example.com")
            .width(300)
            .height(250)
            .build())
        .build();

    CampaignResponseDto result = campaignEntityMapper.entityToResponseDto(entity);

    assertThat(result.name()).isEqualTo("test-campaign-entity");
    assertThat(result.targetCpm()).isEqualByComparingTo("10.5");
    assertThat(result.budget()).isEqualByComparingTo("100.0");
    assertThat(result.startDate()).isEqualTo(LocalDateTime.of(2026, 3, 23, 0, 0, 0));
    assertThat(result.endDate()).isEqualTo(LocalDateTime.of(2026, 3, 24, 23, 59, 59));
    assertThat(result.active()).isTrue();
    assertThat(result.target().os()).isEqualTo("Android");
    assertThat(result.target().country()).isEqualTo("KR");
    assertThat(result.target().minAge()).isEqualTo(20);
    assertThat(result.target().maxAge()).isEqualTo(40);
    assertThat(result.creative().name()).isEqualTo("test-creative");
    assertThat(result.creative().imageUrl()).isEqualTo("https://example.com/a.png");
    assertThat(result.creative().clickUrl()).isEqualTo("https://example.com");
    assertThat(result.creative().width()).isEqualTo(300);
    assertThat(result.creative().height()).isEqualTo(250);
  }
}
