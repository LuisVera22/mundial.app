package com.wirbi.mundial.repository;

import com.wirbi.mundial.model.Scorer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ScorerRepository extends MongoRepository<Scorer, String> {
    List<Scorer> findAllByOrderByPositionAsc();
}
