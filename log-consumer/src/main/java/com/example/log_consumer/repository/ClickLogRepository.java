package com.example.log_consumer.repository;

import com.example.log_consumer.model.ClickLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClickLogRepository extends JpaRepository<ClickLog, Long> {

  Optional<ClickLog> findByAuctionId(String auctionId);
}
