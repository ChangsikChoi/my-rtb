package com.example.log_consumer.repository;

import com.example.log_consumer.model.WinLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WinLogRepository extends JpaRepository<WinLog, Long> {

  Optional<WinLog> findByAuctionId(String auctionId);
}
