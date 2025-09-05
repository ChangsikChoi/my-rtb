package com.example.ad_manager.service;

import com.example.ad_manager.entity.CampaignEntity;
import com.example.ad_manager.mapper.CampaignMapper;
import com.example.ad_manager.model.dto.CampaignCreateReqDto;
import com.example.ad_manager.model.dto.CampaignCreateResDto;
import com.example.ad_manager.redis.CampaignRedisService;
import com.example.ad_manager.repository.CampaignRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class CampaignService {

  private final CampaignRepository campaignRepository;
  private final CampaignRedisService campaignRedisService;
  private final CampaignMapper campaignMapper;

  public CampaignCreateResDto createCampaign(CampaignCreateReqDto dto) {
    if (campaignRepository.existsByName(dto.name())) {
      throw new IllegalArgumentException("campaign name is already exists");
    }
    // rdb 저장
    CampaignEntity savedCampaign = campaignRepository.save(campaignMapper.dtoToEntity(dto));
    // redis 저장
    campaignRedisService.save(campaignMapper.entityToRedisEntity(savedCampaign));

    return campaignMapper.entityToDto(savedCampaign);
  }
}
