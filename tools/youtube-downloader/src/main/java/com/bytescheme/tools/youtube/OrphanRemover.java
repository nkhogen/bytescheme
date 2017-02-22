package com.bytescheme.tools.youtube;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.common.utils.JsonUtils;
import com.google.common.base.Preconditions;

/**
 * Generates commands to remove the orphaned IDs.
 *
 * @author Naorem Khogendro Singh
 *
 */
public final class OrphanRemover {
  private static final Logger LOG = LoggerFactory.getLogger(OrphanRemover.class);
  private static final String ORPHAN_IDS = "orphanIds.sh";
  private static final String DELETE_ID_LINE = "rm -rf %s";

  private OrphanRemover() {

  }

  /**
   * Generate orphan IDs
   *
   * @param outputDirectory
   */
  public static void exportOrphanIds(Path outputDirectory) {
    Preconditions.checkNotNull(outputDirectory, "Invalid output directory");
    try (OutputStream outputStream = new FileOutputStream(
        outputDirectory.resolve(ORPHAN_IDS).toFile())) {
      exportOrphanIds(outputDirectory, outputStream);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Unable to open directory %s", outputDirectory), e);
    }
  }

  /**
   * Generate orphan IDs
   *
   * @param outputDirectory
   */
  public static void exportOrphanIds(Path outputDirectory, OutputStream outputStream) {
    Preconditions.checkNotNull(outputDirectory, "Invalid output directory");
    Preconditions.checkNotNull(outputStream, "Invalid output stream");
    PrintWriter writer = null;
    try {
      final PrintWriter printWriter = (writer = new PrintWriter(
          new OutputStreamWriter(outputStream, "UTF-8")));
      Path idsPath = outputDirectory.resolve(DownloadUtils.IDS_FOLDER);
      Path finalPath = outputDirectory.resolve(DownloadUtils.FINAL_OUTPUT_FOLDER);
      Files.walk(idsPath).map(path -> {
        if (path.toFile().isDirectory()) {
          return false;
        }
        VideoResource videoResource = JsonUtils.fromJsonFile(path.toString(),
            VideoResource.class);
        if (videoResource == null) {
          return false;
        }
        try {
          boolean isFound = Files.walk(finalPath).anyMatch(mediaPath -> {
            Path fileName = mediaPath.getFileName();
            if (fileName == null || mediaPath.toFile().isDirectory()) {
              return false;
            }
            return FilenameUtils.removeExtension(fileName.toString())
                .equals(videoResource.getTitle());
          });
          if (!isFound) {
            LOG.info("Adding ID {} for video {}", videoResource.getVideoId(), path);
            printWriter.println(String.format(DELETE_ID_LINE, path));
          }
        } catch (IOException e) {
          throw new RuntimeException(
              String.format("Failed to read the path %s", finalPath), e);
        }
        return true;
      }).filter(success -> success).count();
    } catch (IOException e) {
      throw new RuntimeException("Failed to export orphaned IDs", e);
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }
}
