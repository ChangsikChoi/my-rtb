package com.example.ad_manager.service;

import static com.example.ad_manager.fixture.CampaignTestFixtures.campaignEntity;
import static com.example.ad_manager.fixture.CampaignTestFixtures.campaignRedisEntity;
import static com.example.ad_manager.fixture.CampaignTestFixtures.campaignRequest;
import static com.example.ad_manager.fixture.CampaignTestFixtures.campaignResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.ad_manager.exception.CampaignNotFoundException;
import com.example.ad_manager.exception.CampaignRedisSyncException;
import com.example.ad_manager.entity.CampaignEntity;
import com.example.ad_manager.entity.CampaignConstraints;
import com.example.ad_manager.exception.CampaignStateConflictException;
import com.example.ad_manager.exception.DuplicateCampaignNameException;
import com.example.ad_manager.mapper.CampaignMapper;
import com.example.ad_manager.model.dto.CampaignCreateReqDto;
import com.example.ad_manager.model.dto.CampaignCreateResDto;
import com.example.ad_manager.redis.CampaignRedisEntity;
import com.example.ad_manager.redis.CampaignRedisService;
import com.example.ad_manager.repository.CampaignRepository;
import java.util.Optional;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

  @Mock
  private CampaignRepository campaignRepository;

  @Mock
  private CampaignMapper campaignMapper;

  @Mock
  private CampaignRedisService campaignRedisService;

  @Mock
  private PlatformTransactionManager transactionManager;

  @InjectMocks
  private CampaignService campaignService;

  @BeforeEach
  void setUp() {
    lenient().when(transactionManager.getTransaction(any()))
        .thenReturn(new SimpleTransactionStatus());
  }

  @Test
  void givenValidRequest_whenCreateCampaign_thenSaveCampaignAndReturnResponse() {
    CampaignCreateReqDto request = campaignRequest("test-campaign-success");
    CampaignEntity entity = campaignEntity(null, "test-campaign-success", false);
    CampaignEntity savedCampaign = campaignEntity("campaign-1", "test-campaign-success", false);
    CampaignCreateResDto response = campaignResponse("campaign-1", false);

    when(campaignRepository.existsByName("test-campaign-success")).thenReturn(false);
    when(campaignMapper.dtoToEntity(request)).thenReturn(entity);
    when(campaignRepository.saveAndFlush(entity)).thenReturn(savedCampaign);
    when(campaignMapper.entityToDto(savedCampaign)).thenReturn(response);

    CampaignCreateResDto result = campaignService.createCampaign(request);

    verify(campaignRepository).saveAndFlush(entity);
    verify(campaignMapper).entityToDto(savedCampaign);
    verifyNoInteractions(campaignRedisService);
    assertThat(result).isSameAs(response);
  }

  @Test
  void givenDuplicateNamePreCheck_whenCreateCampaign_thenThrowDuplicateCampaignNameException() {
    CampaignCreateReqDto request = campaignRequest("test-campaign-duplicate");

    when(campaignRepository.existsByName("test-campaign-duplicate")).thenReturn(true);

    DuplicateCampaignNameException exception = assertThrows(
        DuplicateCampaignNameException.class,
        () -> campaignService.createCampaign(request)
    );

    assertThat(exception).hasMessage("campaign name already exists: test-campaign-duplicate");

    verify(campaignRepository, never()).saveAndFlush(any());
  }

  @Test
  void givenDuplicateNameConstraintViolation_whenCreateCampaign_thenThrowDuplicateCampaignNameException() {
    CampaignCreateReqDto request = campaignRequest("test-campaign-duplicate");
    CampaignEntity entity = campaignEntity(null, "test-campaign-duplicate", false);

    when(campaignRepository.existsByName("test-campaign-duplicate")).thenReturn(false);
    when(campaignMapper.dtoToEntity(request)).thenReturn(entity);
    when(campaignRepository.saveAndFlush(entity))
        .thenThrow(new DataIntegrityViolationException(
            "campaign name unique constraint violation",
            new ConstraintViolationException(
                "duplicate campaign name",
                new SQLException("duplicate key value violates unique constraint"),
                "insert into campaign ...",
                CampaignConstraints.NAME_UNIQUE_CONSTRAINT
            )
        ));

    DuplicateCampaignNameException exception = assertThrows(
        DuplicateCampaignNameException.class,
        () -> campaignService.createCampaign(request)
    );

    assertThat(exception).hasMessage("campaign name already exists: test-campaign-duplicate");
  }

  @Test
  void givenAnotherConstraintViolation_whenCreateCampaign_thenRethrowDataIntegrityViolationException() {
    CampaignCreateReqDto request = campaignRequest("test-campaign-duplicate");
    CampaignEntity entity = campaignEntity(null, "test-campaign-duplicate", false);

    DataIntegrityViolationException exception = new DataIntegrityViolationException(
        "other constraint violation",
        new ConstraintViolationException(
            "some other constraint",
            new SQLException("constraint violation"),
            "insert into campaign ...",
            "uk_some_other_constraint"
        )
    );

    when(campaignRepository.existsByName("test-campaign-duplicate")).thenReturn(false);
    when(campaignMapper.dtoToEntity(request)).thenReturn(entity);
    when(campaignRepository.saveAndFlush(entity)).thenThrow(exception);

    DataIntegrityViolationException thrown = assertThrows(
        DataIntegrityViolationException.class,
        () -> campaignService.createCampaign(request)
    );

    assertThat(thrown).isSameAs(exception);
  }

  @Test
  void givenInactiveCampaign_whenActivateCampaign_thenMarkCampaignActiveAndSyncRedis() {
    CampaignEntity campaign = campaignEntity("campaign-1", false);
    CampaignCreateResDto response = campaignResponse("campaign-1", true);
    CampaignRedisEntity redisEntity = campaignRedisEntity("campaign-1");

    when(campaignRepository.findWithDetailsById("campaign-1")).thenReturn(Optional.of(campaign));
    when(campaignMapper.entityToDto(campaign)).thenReturn(response);
    when(campaignMapper.entityToRedisEntity(campaign)).thenReturn(redisEntity);

    CampaignCreateResDto result = campaignService.activateCampaign("campaign-1");

    verify(campaignRepository).findWithDetailsById("campaign-1");
    verify(campaignMapper).entityToDto(campaign);
    verify(campaignMapper).entityToRedisEntity(campaign);
    verify(campaignRedisService).activate(redisEntity);
    assertThat(result).isSameAs(response);
  }

  @Test
  void givenMissingCampaign_whenActivateCampaign_thenThrowCampaignNotFoundException() {
    when(campaignRepository.findWithDetailsById("campaign-missing")).thenReturn(Optional.empty());

    CampaignNotFoundException exception = assertThrows(
        CampaignNotFoundException.class,
        () -> campaignService.activateCampaign("campaign-missing")
    );

    assertThat(exception).hasMessage("campaign not found: campaign-missing");

    verifyNoInteractions(campaignRedisService);
  }

  @Test
  void givenAlreadyActiveCampaign_whenActivateCampaign_thenThrowCampaignStateConflictException() {
    CampaignEntity campaign = campaignEntity("campaign-1", true);

    when(campaignRepository.findWithDetailsById("campaign-1")).thenReturn(Optional.of(campaign));

    CampaignStateConflictException exception = assertThrows(
        CampaignStateConflictException.class,
        () -> campaignService.activateCampaign("campaign-1")
    );

    assertThat(exception).hasMessage("campaign is already active: campaign-1");

    verifyNoInteractions(campaignRedisService);
  }

  @Test
  void givenRedisSyncFailure_whenActivateCampaign_thenCompensateAndThrowCampaignRedisSyncException() {
    CampaignEntity campaign = campaignEntity("campaign-1", false);
    CampaignRedisEntity redisEntity = campaignRedisEntity("campaign-1");
    RuntimeException redisException = new RuntimeException("redis down");

    when(campaignRepository.findWithDetailsById("campaign-1")).thenReturn(Optional.of(campaign));
    when(campaignRepository.findById("campaign-1")).thenReturn(Optional.of(campaign));
    when(campaignMapper.entityToRedisEntity(campaign)).thenReturn(redisEntity);
    doThrow(redisException).when(campaignRedisService).activate(redisEntity);

    CampaignRedisSyncException exception = assertThrows(
        CampaignRedisSyncException.class,
        () -> campaignService.activateCampaign("campaign-1")
    );

    assertThat(exception).hasMessage("campaign activation failed during redis sync");

    assertThat(campaign.isActive()).isFalse();
    verify(campaignRepository).findById("campaign-1");
  }

  @Test
  void givenCompensationFailure_whenActivateCampaign_thenThrowCompensationFailureException() {
    CampaignEntity campaign = campaignEntity("campaign-1", false);
    CampaignRedisEntity redisEntity = campaignRedisEntity("campaign-1");
    RuntimeException redisException = new RuntimeException("redis down");

    when(campaignRepository.findWithDetailsById("campaign-1")).thenReturn(Optional.of(campaign));
    when(campaignRepository.findById("campaign-1")).thenReturn(Optional.empty());
    when(campaignMapper.entityToRedisEntity(campaign)).thenReturn(redisEntity);
    doThrow(redisException).when(campaignRedisService).activate(redisEntity);

    CampaignRedisSyncException exception = assertThrows(
        CampaignRedisSyncException.class,
        () -> campaignService.activateCampaign("campaign-1")
    );

    assertThat(exception)
        .hasMessage("campaign activation failed during redis sync and compensation failed")
        .hasCause(redisException);

    assertThat(campaign.isActive()).isTrue();
    verify(campaignRepository).findById("campaign-1");
  }

  @Test
  void givenActiveCampaign_whenDeactivateCampaign_thenMarkCampaignInactiveAndSyncRedis() {
    CampaignEntity campaign = campaignEntity("campaign-1", true);
    CampaignCreateResDto response = campaignResponse("campaign-1", false);

    when(campaignRepository.findWithDetailsById("campaign-1")).thenReturn(Optional.of(campaign));
    when(campaignMapper.entityToDto(campaign)).thenReturn(response);

    CampaignCreateResDto result = campaignService.deactivateCampaign("campaign-1");

    verify(campaignRepository).findWithDetailsById("campaign-1");
    verify(campaignMapper).entityToDto(campaign);
    verify(campaignRedisService).deactivate("campaign-1");
    assertThat(result).isSameAs(response);
  }

  @Test
  void givenMissingCampaign_whenDeactivateCampaign_thenThrowCampaignNotFoundException() {
    when(campaignRepository.findWithDetailsById("campaign-missing")).thenReturn(Optional.empty());

    CampaignNotFoundException exception = assertThrows(
        CampaignNotFoundException.class,
        () -> campaignService.deactivateCampaign("campaign-missing")
    );

    assertThat(exception).hasMessage("campaign not found: campaign-missing");

    verifyNoInteractions(campaignRedisService);
  }

  @Test
  void givenAlreadyInactiveCampaign_whenDeactivateCampaign_thenThrowCampaignStateConflictException() {
    CampaignEntity campaign = campaignEntity("campaign-1", false);

    when(campaignRepository.findWithDetailsById("campaign-1")).thenReturn(Optional.of(campaign));

    CampaignStateConflictException exception = assertThrows(
        CampaignStateConflictException.class,
        () -> campaignService.deactivateCampaign("campaign-1")
    );

    assertThat(exception).hasMessage("campaign is already inactive: campaign-1");

    verifyNoInteractions(campaignRedisService);
  }

  @Test
  void givenRedisSyncFailure_whenDeactivateCampaign_thenCompensateAndThrowCampaignRedisSyncException() {
    CampaignEntity campaign = campaignEntity("campaign-1", true);
    RuntimeException redisException = new RuntimeException("redis down");

    when(campaignRepository.findWithDetailsById("campaign-1")).thenReturn(Optional.of(campaign));
    when(campaignRepository.findById("campaign-1")).thenReturn(Optional.of(campaign));
    doThrow(redisException).when(campaignRedisService).deactivate("campaign-1");

    CampaignRedisSyncException exception = assertThrows(
        CampaignRedisSyncException.class,
        () -> campaignService.deactivateCampaign("campaign-1")
    );

    assertThat(exception).hasMessage("campaign deactivation failed during redis sync");

    assertThat(campaign.isActive()).isTrue();
    verify(campaignRepository).findById("campaign-1");
  }

  @Test
  void givenCompensationFailure_whenDeactivateCampaign_thenThrowCompensationFailureException() {
    CampaignEntity campaign = campaignEntity("campaign-1", true);
    RuntimeException redisException = new RuntimeException("redis down");

    when(campaignRepository.findWithDetailsById("campaign-1")).thenReturn(Optional.of(campaign));
    when(campaignRepository.findById("campaign-1")).thenReturn(Optional.empty());
    doThrow(redisException).when(campaignRedisService).deactivate("campaign-1");

    CampaignRedisSyncException exception = assertThrows(
        CampaignRedisSyncException.class,
        () -> campaignService.deactivateCampaign("campaign-1")
    );

    assertThat(exception)
        .hasMessage("campaign deactivation failed during redis sync and compensation failed")
        .hasCause(redisException);

    assertThat(campaign.isActive()).isFalse();
    verify(campaignRepository).findById("campaign-1");
  }


}
