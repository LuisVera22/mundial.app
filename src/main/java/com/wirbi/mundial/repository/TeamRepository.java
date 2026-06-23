package com.wirbi.mundial.repository;

import com.wirbi.mundial.model.Team;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TeamRepository extends MongoRepository<Team, String> {
}
