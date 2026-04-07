package com.example.ad_manager.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.StreamUtils;

@Configuration
public class RedisLuaScriptConfig {

  @Value("classpath:lua/activate_campaign.lua")
  private Resource activateCampaignLua;

  @Bean
  public DefaultRedisScript<Long> activateCampaignLuaScript() throws IOException {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();

    try (InputStream is = activateCampaignLua.getInputStream()) {
      String scriptText = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
      script.setScriptText(scriptText);
    }

    script.setResultType(Long.class);
    return script;
  }
}
