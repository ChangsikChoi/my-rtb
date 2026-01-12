package com.example.log_consumer.repository;

import com.example.log_consumer.model.ImpressionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImpressionLogRepository extends JpaRepository<ImpressionLog, Long> {

}
