package ru.mssecondteam.reviewservice;

import org.springframework.boot.SpringApplication;

public class TestReviewServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(ReviewServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
