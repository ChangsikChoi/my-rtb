package com.example.ad_manager.service;


import com.example.ad_manager.entity.CampaignEntity;
import com.example.ad_manager.model.CampaignCreateReqDto;
import com.example.ad_manager.model.CampaignCreateResponse;
import com.example.ad_manager.redis.Campaign;
import com.example.ad_manager.redis.CampaignRedisLoader;
import com.example.ad_manager.repository.CampaignRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignRedisLoader campaignRedisLoader;

    public CampaignCreateResponse createCampaign(CampaignCreateReqDto campaignCreateReqDto) {
        if (campaignRepository.existsByName(campaignCreateReqDto.name())) {
            throw new IllegalArgumentException("campaign name is already exists");
        }

        CampaignEntity savedCampaign = campaignRepository.save(campaignCreateReqDto.toEntity());

        Campaign campaignForRedis = campaignCreateReqDto.toRedis(savedCampaign.getId());
        campaignRedisLoader.load(campaignForRedis);

        return CampaignCreateResponse.fromEntity(savedCampaign);
    }
}
