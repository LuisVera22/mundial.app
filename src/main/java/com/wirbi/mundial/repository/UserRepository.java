package com.wirbi.mundial.repository;

import com.wirbi.mundial.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
}
