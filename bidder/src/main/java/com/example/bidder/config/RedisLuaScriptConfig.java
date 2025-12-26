package com.example.bidder.config;

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

  @Value("classpath:lua/reserve_budget.lua")
  private Resource reserveBudgetLua;

  @Value("classpath:lua/confirm_budget.lua")
  private Resource confirmBudgetLua;

  @Value("classpath:lua/refund_budget.lua")
  private Resource refundBudgetLua;

  @Bean
  public DefaultRedisScript<Boolean> reserveBudgetLuaScript() throws IOException {
    DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();

    try (InputStream is = reserveBudgetLua.getInputStream()) {
      String scriptText = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
      script.setScriptText(scriptText);
    }

    script.setResultType(Boolean.class);
    return script;
  }

  @Bean
  public DefaultRedisScript<Boolean> confirmBudgetLuaScript() throws IOException {
    DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();

    try (InputStream is = confirmBudgetLua.getInputStream()) {
      String scriptText = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
      script.setScriptText(scriptText);
    }

    script.setResultType(Boolean.class);
    return script;
  }

  @Bean
  public DefaultRedisScript<Boolean> refundBudgetLuaScript() throws IOException {
    DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();

    try (InputStream is = refundBudgetLua.getInputStream()) {
      String scriptText = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
      script.setScriptText(scriptText);
    }

    script.setResultType(Boolean.class);
    return script;
  }

}
