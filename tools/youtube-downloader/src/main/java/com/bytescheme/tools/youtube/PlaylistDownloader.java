package com.bytescheme.tools.youtube;

import static com.bytescheme.tools.youtube.DownloadUtils.DownloadType.AUDIO_ONLY;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Entry class for playlist downloader.
 *
 * @author Naorem Khogendro Singh
 *
 */
@Service
@EnableConfigurationProperties(ConfigProperties.class)
public class PlaylistDownloader implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(PlaylistDownloader.class);
  @Autowired
  private ConfigProperties configProperties;

  @Bean
  public Validator configPropertiesValidator() {
    return new Validator() {
      @Override
      public boolean supports(Class<?> clazz) {
        return true;
      }

      @Override
      public void validate(Object target, Errors errors) {
      }
    };
  }

  @Override
  public void run(ApplicationArguments args) {
    if (CollectionUtils.isEmpty(configProperties.getPlaylists())) {
      LOG.info("No playlists found");
      return;
    }
    configProperties.getPlaylists().stream().forEach(playlist -> {
      try {
        long totalProcessed = DownloadUtils.downloadPlaylist(
            configProperties.getOutputDirectory(), configProperties.getCommandFile(),
            playlist, AUDIO_ONLY);
        LOG.info("Total processed : {}", totalProcessed);
      } catch (Exception e) {
        LOG.error(String.format("Failed to download playlist %s", playlist), e);
      }
    });
    OrphanRemover.exportOrphanIds(configProperties.getOutputDirectory());
  }
}
