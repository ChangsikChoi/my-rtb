package com.example.ad_manager.service;

import com.example.ad_manager.entity.CampaignEntity;
import com.example.ad_manager.entity.CampaignConstraints;
import com.example.ad_manager.exception.CampaignNotFoundException;
import com.example.ad_manager.exception.CampaignRedisSyncException;
import com.example.ad_manager.exception.CampaignStateConflictException;
import com.example.ad_manager.exception.CampaignTransactionException;
import com.example.ad_manager.exception.DuplicateCampaignNameException;
import com.example.ad_manager.mapper.entity.CampaignEntityMapper;
import com.example.ad_manager.mapper.redis.CampaignRedisProjectionMapper;
import com.example.ad_manager.model.dto.CampaignCreateRequestDto;
import com.example.ad_manager.model.dto.CampaignResponseDto;
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
  private final CampaignEntityMapper campaignEntityMapper;
  private final CampaignRedisProjectionMapper campaignRedisProjectionMapper;
  private final CampaignRedisService campaignRedisService;
  private final TransactionTemplate transactionTemplate;
  private final TransactionTemplate requiresNewTransactionTemplate;

  public CampaignService(
      CampaignRepository campaignRepository,
      CampaignEntityMapper campaignEntityMapper,
      CampaignRedisProjectionMapper campaignRedisProjectionMapper,
      CampaignRedisService campaignRedisService,
      PlatformTransactionManager transactionManager
  ) {
    this.campaignRepository = campaignRepository;
    this.campaignEntityMapper = campaignEntityMapper;
    this.campaignRedisProjectionMapper = campaignRedisProjectionMapper;
    this.campaignRedisService = campaignRedisService;

    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
    this.requiresNewTransactionTemplate.setPropagationBehavior(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW
    );
  }

  @Transactional
  public CampaignResponseDto createCampaign(CampaignCreateRequestDto dto) {
    if (campaignRepository.existsByName(dto.name())) {
      throw new DuplicateCampaignNameException(dto.name());
    }
    try {
      CampaignEntity campaign = campaignEntityMapper.dtoToEntity(dto);
      campaign.deactivate();

      CampaignEntity savedCampaign = campaignRepository.saveAndFlush(campaign);

      return campaignEntityMapper.entityToResponseDto(savedCampaign);
    } catch (DataIntegrityViolationException e) {
      if (isDuplicateCampaignNameViolation(e)) {
        throw new DuplicateCampaignNameException(dto.name());
      }
      throw e;
    }
  }

  public CampaignResponseDto activateCampaign(String campaignId) {
    CampaignEntity campaign = activateCampaignInTransaction(campaignId);

    try {
      campaignRedisService.activate(campaignRedisProjectionMapper.entityToRedisEntity(campaign));
      return campaignEntityMapper.entityToResponseDto(campaign);
    } catch (RuntimeException exception) {
      log.warn("campaign activation redis sync failed. campaignId={}", campaignId, exception);
      compensateActivation(campaignId, exception);
      throw CampaignRedisSyncException.activationFailed(exception);
    }
  }

  public CampaignResponseDto deactivateCampaign(String campaignId) {
    CampaignEntity campaign = deactivateCampaignInTransaction(campaignId);

    try {
      campaignRedisService.deactivate(campaignId);
      return campaignEntityMapper.entityToResponseDto(campaign);
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

  private CampaignEntity activateCampaignInTransaction(String campaignId) {
    CampaignEntity campaign = transactionTemplate.execute(
        status -> activateCampaignInDatabase(campaignId)
    );

    if (campaign == null) {
      throw CampaignTransactionException.activationFailed();
    }

    return campaign;
  }

  private CampaignEntity deactivateCampaignInTransaction(String campaignId) {
    CampaignEntity campaign = transactionTemplate.execute(
        status -> deactivateCampaignInDatabase(campaignId)
    );

    if (campaign == null) {
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
