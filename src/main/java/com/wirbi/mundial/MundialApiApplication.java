package com.wirbi.mundial;

import com.wirbi.mundial.config.FootballDataProperties;
import com.wirbi.mundial.config.TournamentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({TournamentProperties.class, FootballDataProperties.class})
public class MundialApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MundialApiApplication.class, args);
    }
}
