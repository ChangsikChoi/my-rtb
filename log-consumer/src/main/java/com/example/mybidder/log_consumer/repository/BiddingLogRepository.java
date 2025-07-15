package com.example.mybidder.log_consumer.repository;

import com.example.mybidder.log_consumer.model.BiddingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BiddingLogRepository extends JpaRepository<BiddingLog, Long> {
}
