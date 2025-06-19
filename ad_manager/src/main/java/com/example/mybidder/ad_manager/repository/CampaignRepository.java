package com.example.mybidder.ad_manager.repository;


import com.example.mybidder.ad_manager.entity.CampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignRepository extends JpaRepository<CampaignEntity, String> {
    boolean existsByName(String name);
}
