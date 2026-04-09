package com.example.ad_manager.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
class CampaignRedisServiceTest {

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private DefaultRedisScript<Long> activateCampaignLuaScript;

  @Mock
  private CampaignRedisHashMapper campaignRedisHashMapper;

  @Mock
  private SetOperations<String, String> setOperations;

  @InjectMocks
  private CampaignRedisService campaignRedisService;

  @Test
  void givenCampaign_whenActivate_thenExecuteLuaScriptWithExpectedKeysAndArgs() {
    CampaignRedisEntity campaign = CampaignRedisEntity.builder()
        .id("campaign-1")
        .remainingBudgetMicro(100000000L)
        .build();
    Map<String, String> hash = new LinkedHashMap<>();
    hash.put("id", "campaign-1");
    hash.put("name", "test-campaign");

    when(campaignRedisHashMapper.toHash(campaign)).thenReturn(hash);
    when(redisTemplate.execute(eq(activateCampaignLuaScript), anyList(), any(Object[].class)))
        .thenReturn(1L);

    campaignRedisService.activate(campaign);

    verify(campaignRedisHashMapper).toHash(campaign);
    verify(redisTemplate).execute(
        eq(activateCampaignLuaScript),
        eq(List.of(
            "campaign:campaign-1",
            "campaign:campaign-1:budget_total",
            "campaign:campaign-1:budget_reserved",
            "campaign:ids"
        )),
        eq("campaign-1"),
        eq("100000000"),
        eq("0"),
        eq("id"),
        eq("campaign-1"),
        eq("name"),
        eq("test-campaign")
    );
  }

  @Test
  void givenLuaExecutionFailure_whenActivate_thenThrowIllegalStateException() {
    CampaignRedisEntity campaign = CampaignRedisEntity.builder()
        .id("campaign-1")
        .remainingBudgetMicro(100000000L)
        .build();

    when(campaignRedisHashMapper.toHash(campaign)).thenReturn(Map.of());
    when(redisTemplate.execute(eq(activateCampaignLuaScript), anyList(), any(Object[].class)))
        .thenReturn(0L);

    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> campaignRedisService.activate(campaign)
    );

    assertThat(exception)
        .hasMessage("campaign activation lua script did not complete successfully: campaign-1");
  }

  @Test
  void givenCampaignId_whenDeactivate_thenRemoveCampaignFromCampaignList() {
    when(redisTemplate.opsForSet()).thenReturn(setOperations);

    campaignRedisService.deactivate("campaign-1");

    verify(redisTemplate).opsForSet();
    verify(setOperations).remove(RedisKeys.CAMPAIGN_LIST_KEY, "campaign-1");
  }
}
