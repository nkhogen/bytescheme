package com.bytescheme.service.controlboard;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry class
 * 
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
