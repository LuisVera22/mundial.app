package com.wirbi.mundial.repository;

import com.wirbi.mundial.model.TournamentState;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TournamentStateRepository extends MongoRepository<TournamentState, String> {
}
