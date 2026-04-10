package com.example.ad_manager.service;

import static com.example.ad_manager.fixture.CampaignTestFixtures.persistableCampaignEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.example.ad_manager.entity.CampaignEntity;
import com.example.ad_manager.exception.CampaignRedisSyncException;
import com.example.ad_manager.redis.CampaignRedisEntity;
import com.example.ad_manager.redis.CampaignRedisService;
import com.example.ad_manager.repository.CampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class CampaignServiceIntegrationTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired
  private CampaignService campaignService;

  @MockitoSpyBean
  private CampaignRepository campaignRepository;

  @MockitoBean
  private CampaignRedisService campaignRedisService;

  @BeforeEach
  void setUp() {
    campaignRepository.deleteAll();
  }

  @Test
  void givenRedisActivationFailure_whenActivateCampaign_thenCompensateCampaignToInactive() {
    CampaignEntity savedCampaign = campaignRepository.saveAndFlush(
        persistableCampaignEntity("activation-compensation-campaign", false)
    );
    doThrow(new RuntimeException("redis down"))
        .when(campaignRedisService)
        .activate(org.mockito.ArgumentMatchers.any(CampaignRedisEntity.class));

    assertThrows(
        CampaignRedisSyncException.class,
        () -> campaignService.activateCampaign(savedCampaign.getId())
    );

    CampaignEntity compensatedCampaign = campaignRepository.findById(savedCampaign.getId())
        .orElseThrow();
    assertThat(compensatedCampaign.isActive()).isFalse();
  }

  @Test
  void givenInactiveCampaign_whenActivateCampaign_thenCampaignIsCommittedBeforeRedisSync() {
    CampaignEntity savedCampaign = campaignRepository.saveAndFlush(
        persistableCampaignEntity("activation-transaction-separation-campaign", false)
    );

    doAnswer(invocation -> {
      CampaignEntity activatedCampaign = campaignRepository.findById(savedCampaign.getId())
          .orElseThrow();
      assertThat(activatedCampaign.isActive()).isTrue();
      return null;
    }).when(campaignRedisService).activate(any(CampaignRedisEntity.class));

    campaignService.activateCampaign(savedCampaign.getId());
  }

  @Test
  void givenRedisDeactivationFailure_whenDeactivateCampaign_thenCompensateCampaignToActive() {
    CampaignEntity savedCampaign = campaignRepository.saveAndFlush(
        persistableCampaignEntity("deactivation-compensation-campaign", true)
    );
    doThrow(new RuntimeException("redis down"))
        .when(campaignRedisService).deactivate(savedCampaign.getId());

    assertThrows(
        CampaignRedisSyncException.class,
        () -> campaignService.deactivateCampaign(savedCampaign.getId())
    );

    CampaignEntity compensatedCampaign = campaignRepository.findById(savedCampaign.getId())
        .orElseThrow();
    assertThat(compensatedCampaign.isActive()).isTrue();
  }

  @Test
  void givenCompensationFailure_whenActivateCampaign_thenLeaveCommittedActiveStateInDatabase() {
    CampaignEntity savedCampaign = campaignRepository.saveAndFlush(
        persistableCampaignEntity("activation-compensation-failure-campaign", false)
    );
    doThrow(new RuntimeException("redis down"))
        .when(campaignRedisService)
        .activate(any(CampaignRedisEntity.class));
    doReturn(java.util.Optional.empty())
        .when(campaignRepository)
        .findById(savedCampaign.getId());

    CampaignRedisSyncException exception = assertThrows(
        CampaignRedisSyncException.class,
        () -> campaignService.activateCampaign(savedCampaign.getId())
    );

    assertThat(exception)
        .hasMessage("campaign activation failed during redis sync and compensation failed");

    reset(campaignRepository);

    CampaignEntity inconsistentCampaign = campaignRepository.findById(savedCampaign.getId())
        .orElseThrow();
    assertThat(inconsistentCampaign.isActive()).isTrue();
  }

  @Test
  void givenActiveCampaign_whenDeactivateCampaign_thenCampaignIsCommittedBeforeRedisSync() {
    CampaignEntity savedCampaign = campaignRepository.saveAndFlush(
        persistableCampaignEntity("deactivation-transaction-separation-campaign", true)
    );

    doAnswer(invocation -> {
      CampaignEntity deactivatedCampaign = campaignRepository.findById(savedCampaign.getId())
          .orElseThrow();
      assertThat(deactivatedCampaign.isActive()).isFalse();
      return null;
    }).when(campaignRedisService).deactivate(savedCampaign.getId());

    campaignService.deactivateCampaign(savedCampaign.getId());
  }

  @Test
  void givenCompensationFailure_whenDeactivateCampaign_thenLeaveCommittedInactiveStateInDatabase() {
    CampaignEntity savedCampaign = campaignRepository.saveAndFlush(
        persistableCampaignEntity("deactivation-compensation-failure-campaign", true)
    );
    doThrow(new RuntimeException("redis down"))
        .when(campaignRedisService).deactivate(savedCampaign.getId());
    doReturn(java.util.Optional.empty())
        .when(campaignRepository)
        .findById(savedCampaign.getId());

    CampaignRedisSyncException exception = assertThrows(
        CampaignRedisSyncException.class,
        () -> campaignService.deactivateCampaign(savedCampaign.getId())
    );

    assertThat(exception)
        .hasMessage("campaign deactivation failed during redis sync and compensation failed");

    reset(campaignRepository);

    CampaignEntity inconsistentCampaign = campaignRepository.findById(savedCampaign.getId())
        .orElseThrow();
    assertThat(inconsistentCampaign.isActive()).isFalse();
  }
}
