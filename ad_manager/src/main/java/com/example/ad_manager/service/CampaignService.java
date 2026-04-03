package com.example.ad_manager.service;

import com.example.ad_manager.entity.CampaignEntity;
import com.example.ad_manager.entity.CampaignConstraints;
import com.example.ad_manager.exception.CampaignNotFoundException;
import com.example.ad_manager.exception.CampaignRedisSyncException;
import com.example.ad_manager.exception.CampaignStateConflictException;
import com.example.ad_manager.exception.CampaignTransactionException;
import com.example.ad_manager.exception.DuplicateCampaignNameException;
import com.example.ad_manager.mapper.CampaignMapper;
import com.example.ad_manager.model.dto.CampaignCreateReqDto;
import com.example.ad_manager.model.dto.CampaignCreateResDto;
import com.example.ad_manager.redis.CampaignRedisService;
import com.example.ad_manager.repository.CampaignRepository;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class CampaignService {

  private final CampaignRepository campaignRepository;
  private final CampaignMapper campaignMapper;
  private final CampaignRedisService campaignRedisService;
  private final TransactionTemplate transactionTemplate;
  private final TransactionTemplate requiresNewTransactionTemplate;

  public CampaignService(
      CampaignRepository campaignRepository,
      CampaignMapper campaignMapper,
      CampaignRedisService campaignRedisService,
      PlatformTransactionManager transactionManager
  ) {
    this.campaignRepository = campaignRepository;
    this.campaignMapper = campaignMapper;
    this.campaignRedisService = campaignRedisService;

    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
    this.requiresNewTransactionTemplate.setPropagationBehavior(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW
    );
  }

  @Transactional
  public CampaignCreateResDto createCampaign(CampaignCreateReqDto dto) {
    if (campaignRepository.existsByName(dto.name())) {
      throw new DuplicateCampaignNameException(dto.name());
    }
    try {
      CampaignEntity savedCampaign = campaignRepository.saveAndFlush(
          campaignMapper.dtoToEntity(dto)
      );

      return campaignMapper.entityToDto(savedCampaign);
    } catch (DataIntegrityViolationException e) {
      if (isDuplicateCampaignNameViolation(e)) {
        throw new DuplicateCampaignNameException(dto.name());
      }
      throw e;
    }
  }

  public CampaignCreateResDto activateCampaign(String campaignId) {
    CampaignEntity campaign = requireTransactionResult(
        transactionTemplate.execute(status -> activateCampaignInDatabase(campaignId)),
        "activation"
    );

    try {
      campaignRedisService.activate(campaignMapper.entityToRedisEntity(campaign));
      return campaignMapper.entityToDto(campaign);
    } catch (RuntimeException exception) {
      log.warn("campaign activation redis sync failed. campaignId={}", campaignId, exception);
      compensateActivation(campaignId, exception);
      throw CampaignRedisSyncException.activationFailed(exception);
    }
  }

  public CampaignCreateResDto deactivateCampaign(String campaignId) {
    CampaignEntity campaign = requireTransactionResult(
        transactionTemplate.execute(status -> deactivateCampaignInDatabase(campaignId)),
        "deactivation"
    );

    try {
      campaignRedisService.deactivate(campaignId);
      return campaignMapper.entityToDto(campaign);
    } catch (RuntimeException exception) {
      log.warn("campaign deactivation redis sync failed. campaignId={}", campaignId, exception);
      compensateDeactivation(campaignId, exception);
      throw CampaignRedisSyncException.deactivationFailed(exception);
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

  private CampaignEntity activateCampaignInDatabase(String campaignId) {
    CampaignEntity campaign = campaignRepository.findWithDetailsById(campaignId)
        .orElseThrow(() -> new CampaignNotFoundException(campaignId));

    if (campaign.isActive()) {
      throw CampaignStateConflictException.alreadyActive(campaignId);
    }

    campaign.activate();
    return campaign;
  }

  private CampaignEntity deactivateCampaignInDatabase(String campaignId) {
    CampaignEntity campaign = campaignRepository.findWithDetailsById(campaignId)
        .orElseThrow(() -> new CampaignNotFoundException(campaignId));

    if (!campaign.isActive()) {
      throw CampaignStateConflictException.alreadyInactive(campaignId);
    }

    campaign.deactivate();
    return campaign;
  }

  /**
   * 트랜잭션 결과가 null인 경우 예외를 던지는 헬퍼 메서드
   */
  private CampaignEntity requireTransactionResult(CampaignEntity campaign, String operation) {
    if (campaign == null) {
      if ("activation".equals(operation)) {
        throw CampaignTransactionException.activationFailed();
      }
      throw CampaignTransactionException.deactivationFailed();
    }
    return campaign;
  }

  private void compensateActivation(String campaignId, RuntimeException redisException) {
    try {
      requiresNewTransactionTemplate.executeWithoutResult(status -> {
        CampaignEntity campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalStateException(
                "campaign not found for activation compensation: " + campaignId));
        campaign.deactivate();
      });
    } catch (RuntimeException compensationException) {
      log.error("campaign activation compensation failed. campaignId={}", campaignId,
          compensationException);
      throw CampaignRedisSyncException.activationCompensationFailed(redisException);
    }
  }

  private void compensateDeactivation(String campaignId, RuntimeException redisException) {
    try {
      requiresNewTransactionTemplate.executeWithoutResult(status -> {
        CampaignEntity campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalStateException(
                "campaign not found for deactivation compensation: " + campaignId));
        campaign.activate();
      });
    } catch (RuntimeException compensationException) {
      log.error("campaign deactivation compensation failed. campaignId={}", campaignId,
          compensationException);
      throw CampaignRedisSyncException.deactivationCompensationFailed(redisException);
    }
  }

}
