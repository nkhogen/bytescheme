package com.bytescheme.tools.youtube;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring boot application.
 *
 * @author Naorem Khogendro Singh
 *
 */
@SpringBootApplication
public class Main {
  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(Main.class);
    app.setBannerMode(Banner.Mode.OFF);
    app.run(args).close();
  }
}
