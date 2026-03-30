package com.example.ad_manager.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ad_manager.entity.CampaignEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    campaignRepository.saveAndFlush(createCampaign("test-campaign-duplicate"));

    assertThatThrownBy(() -> campaignRepository.saveAndFlush(createCampaign("test-campaign-duplicate")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private CampaignEntity createCampaign(String name) {
    return CampaignEntity.builder()
        .name(name)
        .targetCpm(BigDecimal.valueOf(10.5))
        .budget(BigDecimal.valueOf(100))
        .startDate(LocalDateTime.of(2026, 3, 23, 0, 0))
        .endDate(LocalDateTime.of(2026, 3, 24, 23, 59, 59))
        .active(true)
        .owner("test")
        .build();
  }
}
