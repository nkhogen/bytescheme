package com.bytescheme.service.eventscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry class for event scheduler service.
 * @author Naorem Khogendro Singh
 *
 */
@SpringBootApplication
public class Main {
  public static void main(String[] args) throws Exception {
    SpringApplication app = new SpringApplication(Main.class);
    app.setBannerMode(Banner.Mode.OFF);
    app.run(args);
  }
}
