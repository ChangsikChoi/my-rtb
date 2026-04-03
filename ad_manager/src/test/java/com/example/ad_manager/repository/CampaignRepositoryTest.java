package com.example.ad_manager.repository;

import static com.example.ad_manager.fixture.CampaignTestFixtures.persistableCampaignEntity;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CampaignRepositoryTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired
  private CampaignRepository campaignRepository;

  @Test
  void givenSameNameCampaigns_whenSaveAndFlush_thenThrowDataIntegrityViolationException() {
    campaignRepository.saveAndFlush(persistableCampaignEntity("test-campaign-duplicate", false));

    assertThrows(DataIntegrityViolationException.class, () -> campaignRepository.saveAndFlush(
        persistableCampaignEntity("test-campaign-duplicate", false)
    ));
  }
}
