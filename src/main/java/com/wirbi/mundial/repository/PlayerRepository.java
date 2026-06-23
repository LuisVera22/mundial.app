package com.wirbi.mundial.repository;

import com.wirbi.mundial.model.Player;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlayerRepository extends MongoRepository<Player, String> {
}
