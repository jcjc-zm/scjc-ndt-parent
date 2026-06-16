package com.scjc.ndt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.scjc.ndt.mapper")
public class NdtApplication {
    public static void main(String[] args) {
        SpringApplication.run(NdtApplication.class, args);
    }
}