package de.othr.traintogether;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TrainTogetherApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainTogetherApplication.class, args);
    }

}
