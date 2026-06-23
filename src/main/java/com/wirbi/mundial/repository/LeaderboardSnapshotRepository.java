package com.wirbi.mundial.repository;

import com.wirbi.mundial.model.LeaderboardSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LeaderboardSnapshotRepository extends MongoRepository<LeaderboardSnapshot, String> {
    Optional<LeaderboardSnapshot> findTopByOrderByTakenAtDesc();
}
