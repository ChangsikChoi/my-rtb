package com.example.ad_manager.repository;

import com.example.ad_manager.redis.CampaignRedisEntity;
import org.springframework.data.repository.CrudRepository;

public interface CampaignRedisRepository extends CrudRepository<CampaignRedisEntity, String> {

}
