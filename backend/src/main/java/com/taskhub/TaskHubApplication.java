package com.taskhub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.taskhub.mapper")
@EnableScheduling
public class TaskHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskHubApplication.class, args);
    }
}
