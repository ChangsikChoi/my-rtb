package com.example.log_consumer.repository;

import com.example.log_consumer.model.BiddingLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BiddingLogRepository extends JpaRepository<BiddingLog, Long> {

  Optional<BiddingLog> findByAuctionId(String auctionId);
}
