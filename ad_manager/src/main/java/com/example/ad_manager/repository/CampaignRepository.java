package com.example.ad_manager.repository;


import com.example.ad_manager.entity.CampaignEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignRepository extends JpaRepository<CampaignEntity, String> {

  boolean existsByName(String name);

  @EntityGraph(attributePaths = {"target", "creative"})
  @Query("select c from campaign c where c.id = :id")
  Optional<CampaignEntity> findWithDetailsById(@Param("id") String id);
}
