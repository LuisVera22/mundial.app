package com.wirbi.mundial.repository;

import com.wirbi.mundial.model.Match;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MatchRepository extends MongoRepository<Match, String> {
    List<Match> findAllByOrderByKickoffAsc();
}
