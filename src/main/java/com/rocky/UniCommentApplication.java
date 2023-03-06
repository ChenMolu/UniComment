package com.rocky;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.rocky.mapper")
@SpringBootApplication
public class UniCommentApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniCommentApplication.class, args);
    }

}
