package com.example.ad_manager.redis;

import static com.example.ad_manager.fixture.CampaignTestFixtures.campaignRedisEntity;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.ad_manager.config.RedisLuaScriptConfig;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = CampaignRedisServiceIntegrationTest.TestApplication.class)
class CampaignRedisServiceIntegrationTest {

  @Container
  @ServiceConnection
  static GenericContainer<?> redis = new GenericContainer<>("redis:7")
      .withExposedPorts(6379);
  @Autowired
  private CampaignRedisService campaignRedisService;
  @Autowired
  private StringRedisTemplate redisTemplate;

  @BeforeEach
  void setUp() {
    redisTemplate.execute((RedisCallback<Void>) connection -> {
      connection.serverCommands().flushAll();
      return null;
    });
  }

  @Test
  void givenNewCampaign_whenActivate_thenStoreProjectionBudgetAndCampaignList() {
    CampaignRedisEntity campaign = campaignRedisEntity("campaign-1");

    campaignRedisService.activate(campaign);

    Map<Object, Object> hash = redisTemplate.opsForHash()
        .entries(RedisKeys.campaignKey("campaign-1"));
    String totalBudget = redisTemplate.opsForValue().get(
        RedisKeys.campaignTotalBudgetKey("campaign-1")
    );
    String reservedBudget = redisTemplate.opsForValue().get(
        RedisKeys.campaignReservedBudgetKey("campaign-1")
    );
    Set<String> campaignIds = redisTemplate.opsForSet().members(RedisKeys.CAMPAIGN_LIST_KEY);

    assertThat(hash)
        .containsEntry("id", "campaign-1")
        .containsEntry("name", "test-campaign")
        .containsEntry("target.os", "Android")
        .containsEntry("creative.id", "creative-1");
    assertThat(totalBudget).isEqualTo("100000000");
    assertThat(reservedBudget).isEqualTo("0");
    assertThat(campaignIds).contains("campaign-1");
  }

  @Test
  void givenExistingProjectionAndBudget_whenActivateAgain_thenKeepHashAndBudgetAndRestoreList() {
    redisTemplate.opsForHash().put(RedisKeys.campaignKey("campaign-1"), "id", "campaign-1");
    redisTemplate.opsForHash()
        .put(RedisKeys.campaignKey("campaign-1"), "name", "existing-campaign");
    redisTemplate.opsForHash().put(
        RedisKeys.campaignKey("campaign-1"), "creative.id", "existing-creative"
    );
    redisTemplate.opsForValue().set(RedisKeys.campaignTotalBudgetKey("campaign-1"), "777");
    redisTemplate.opsForValue().set(RedisKeys.campaignReservedBudgetKey("campaign-1"), "111");

    campaignRedisService.activate(campaignRedisEntity("campaign-1"));

    Map<Object, Object> hash = redisTemplate.opsForHash()
        .entries(RedisKeys.campaignKey("campaign-1"));
    String totalBudget = redisTemplate.opsForValue().get(
        RedisKeys.campaignTotalBudgetKey("campaign-1")
    );
    String reservedBudget = redisTemplate.opsForValue().get(
        RedisKeys.campaignReservedBudgetKey("campaign-1")
    );
    Set<String> campaignIds = redisTemplate.opsForSet().members(RedisKeys.CAMPAIGN_LIST_KEY);

    assertThat(hash)
        .containsEntry("id", "campaign-1")
        .containsEntry("name", "existing-campaign")
        .containsEntry("creative.id", "existing-creative")
        .doesNotContainEntry("name", "test-campaign");
    assertThat(totalBudget).isEqualTo("777");
    assertThat(reservedBudget).isEqualTo("111");
    assertThat(campaignIds).contains("campaign-1");
  }

  @SpringBootApplication(exclude = {
      DataSourceAutoConfiguration.class,
      HibernateJpaAutoConfiguration.class,
      JpaRepositoriesAutoConfiguration.class
  })
  @Import({
      CampaignRedisService.class,
      CampaignRedisHashMapper.class,
      RedisLuaScriptConfig.class
  })
  static class TestApplication {

  }
}
