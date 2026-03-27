package com.example.ad_manager.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ad_manager.exception.DuplicateCampaignNameException;
import com.example.ad_manager.mapper.CampaignMapper;
import com.example.ad_manager.model.dto.CampaignCreateReqDto;
import com.example.ad_manager.model.dto.CreativeCreateReqDto;
import com.example.ad_manager.model.dto.TargetCreateReqDto;
import com.example.ad_manager.redis.CampaignRedisService;
import com.example.ad_manager.repository.CampaignRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

  @Mock
  private CampaignRepository campaignRepository;

  @Mock
  private CampaignRedisService campaignRedisService;

  @Mock
  private CampaignMapper campaignMapper;

  @InjectMocks
  private CampaignService campaignService;

  @Test
  void givenDuplicateName_whenCreateCampaign_thenThrowDuplicateCampaignNameException() {
    CampaignCreateReqDto request = CampaignCreateReqDto.builder()
        .name("test-campaign-duplicate")
        .targetCpm(BigDecimal.valueOf(10.5))
        .budget(BigDecimal.valueOf(100))
        .startDate(LocalDateTime.of(2026, 3, 23, 0, 0))
        .endDate(LocalDateTime.of(2026, 3, 24, 0, 0))
        .target(TargetCreateReqDto.builder().build())
        .creative(CreativeCreateReqDto.builder()
            .name("test-creative-duplicate")
            .imageUrl("https://example.com/a.png")
            .clickUrl("https://example.com")
            .build())
        .active(true)
        .owner("test")
        .build();

    when(campaignRepository.existsByName("test-campaign-duplicate")).thenReturn(true);

    assertThatThrownBy(() -> campaignService.createCampaign(request))
        .isInstanceOf(DuplicateCampaignNameException.class)
        .hasMessage("campaign name already exists: test-campaign-duplicate");

    verify(campaignRepository, never()).save(org.mockito.ArgumentMatchers.any());
    verify(campaignRedisService, never()).save(org.mockito.ArgumentMatchers.any());
  }
}
