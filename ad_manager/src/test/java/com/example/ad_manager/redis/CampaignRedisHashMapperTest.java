package com.example.ad_manager.redis;

import static com.example.ad_manager.fixture.CampaignTestFixtures.campaignRedisEntity;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CampaignRedisHashMapperTest {

  private final CampaignRedisHashMapper campaignRedisHashMapper =
      new CampaignRedisHashMapper(new ObjectMapper());

  @Test
  void givenCampaignRedisEntity_whenToHashArgs_thenReturnFlattenedProjectionHashArgs() {
    CampaignRedisEntity campaign = campaignRedisEntity("campaign-1");

    List<String> hashArgs = campaignRedisHashMapper.toHashArgs(campaign);
    Map<String, String> hash = toHashMap(hashArgs);

    assertThat(hash)
        .containsEntry("id", "campaign-1")
        .containsEntry("name", "test-campaign")
        .containsEntry("targetCpmMicro", "10500000")
        .containsEntry("budgetMicro", "100000000")
        .containsEntry("remainingBudgetMicro", "100000000")
        .containsEntry("startDate", "20260323000000")
        .containsEntry("endDate", "20260324235959")
        .containsEntry("target.os", "Android")
        .containsEntry("target.country", "KR")
        .containsEntry("target.minAge", "20")
        .containsEntry("target.maxAge", "40")
        .containsEntry("creative.id", "creative-1")
        .containsEntry("creative.imageUrl", "https://example.com/a.png")
        .containsEntry("creative.clickUrl", "https://example.com")
        .containsEntry("creative.width", "300")
        .containsEntry("creative.height", "250");
  }

  private Map<String, String> toHashMap(List<String> hashArgs) {
    Map<String, String> hash = new LinkedHashMap<>();
    for (int index = 0; index < hashArgs.size(); index += 2) {
      hash.put(hashArgs.get(index), hashArgs.get(index + 1));
    }
    return hash;
  }
}
