package com.studyhelper;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.studyhelper.mapper")
public class StudyHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyHelperApplication.class, args);
    }
}
