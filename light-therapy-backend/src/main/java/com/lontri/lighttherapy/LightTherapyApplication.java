package com.lontri.lighttherapy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class LightTherapyApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(LightTherapyApplication.class, args);
    }
}
