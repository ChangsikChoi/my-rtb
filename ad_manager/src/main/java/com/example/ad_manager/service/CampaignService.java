package com.example.ad_manager.service;

import com.example.ad_manager.entity.CampaignEntity;
import com.example.ad_manager.entity.CampaignConstraints;
import com.example.ad_manager.exception.DuplicateCampaignNameException;
import com.example.ad_manager.mapper.CampaignMapper;
import com.example.ad_manager.model.dto.CampaignCreateReqDto;
import com.example.ad_manager.model.dto.CampaignCreateResDto;
import com.example.ad_manager.redis.CampaignRedisService;
import com.example.ad_manager.repository.CampaignRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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
      throw new DuplicateCampaignNameException(dto.name());
    }

    try {
      // rdb 저장
      CampaignEntity savedCampaign = campaignRepository.saveAndFlush(campaignMapper.dtoToEntity(dto));
      // redis 저장
      campaignRedisService.save(campaignMapper.entityToRedisEntity(savedCampaign));

      return campaignMapper.entityToDto(savedCampaign);
    } catch (DataIntegrityViolationException e) {
      if (isDuplicateCampaignNameViolation(e)) {
        throw new DuplicateCampaignNameException(dto.name());
      }
      throw e;
    }
  }

  private boolean isDuplicateCampaignNameViolation(DataIntegrityViolationException e) {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException constraintViolationException) {
        String constraintName = constraintViolationException.getConstraintName();
        return CampaignConstraints.NAME_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintName);
      }
    }
    return false;
  }
}
