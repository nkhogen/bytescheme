package com.bytescheme.tools.youtube;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Spring managed config properties for the downloader.
 *
 * @author Naorem Khogendro Singh
 *
 */
@ConfigurationProperties(prefix = "downloader")
public class ConfigProperties {
  private Path commandFile;
  private Path outputDirectory;
  private Set<String> playlists;
  private Set<String> videoIds;

  public Path getCommandFile() {
    return commandFile;
  }

  public void setCommandFile(String commandFile) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(commandFile),
        "Invalid command file");
    this.commandFile = new File(commandFile).toPath();
  }

  public Path getOutputDirectory() {
    return outputDirectory;
  }

  public void setOutputDirectory(String outputDirectory) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(outputDirectory),
        "Invalid output directory");
    this.outputDirectory = new File(outputDirectory).toPath();
  }

  public Set<String> getPlaylists() {
    return playlists;
  }

  public void setPlaylists(Set<String> playlists) {
    this.playlists = playlists;
  }

  public Set<String> getVideoIds() {
    return videoIds;
  }

  public void setVideoIds(Set<String> videoIds) {
    this.videoIds = videoIds;
  }
}
