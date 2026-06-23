package com.wirbi.mundial.repository;

import com.wirbi.mundial.model.Prediction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends MongoRepository<Prediction, String> {
    List<Prediction> findByUserId(String userId);

    Optional<Prediction> findByUserIdAndMatchId(String userId, String matchId);

    List<Prediction> findByMatchId(String matchId);
}
