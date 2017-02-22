package com.bytescheme.tools.youtube;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.common.utilities.CommandExecutor;
import com.bytescheme.common.utilities.CommandInfoReader;
import com.bytescheme.common.utils.JsonUtils;
import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VideoFileInfo;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.vget.vhs.YouTubeInfo;
import com.github.axet.vget.vhs.YouTubeInfo.StreamAudio;
import com.github.axet.vget.vhs.YouTubeInfo.StreamCombined;
import com.github.axet.vget.vhs.YouTubeInfo.StreamVideo;
import com.github.axet.vget.vhs.YouTubeParser;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * This handles all the downloads.
 *
 * @author Naorem Khogendro Singh
 *
 */
public final class DownloadUtils {
  private static Logger LOG = LoggerFactory.getLogger(DownloadUtils.class);

  public static String PLAYLIST_URL_TEMPLATE = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=10&playlistId=%s&key=AIzaSyAOPiX0V9T2Xve0sZB2ZbWn0vjP7NFh3r8";

  public static String VIDEO_URL_TEMPLATE = "https://www.youtube.com/watch?v=%s";

  public static String RAW_OUTPUT_FOLDER = "raw";

  public static String FINAL_OUTPUT_FOLDER = "final";

  public static String IDS_FOLDER = "ids";

  /**
   * Type of download.
   *
   * @author Naorem Khogendro Singh
   *
   */
  public enum DownloadType {
    AUDIO_ONLY, AUDIO_VIDEO, VIDEO_ONLY
  }

  private DownloadUtils() {

  }

  /**
   * Download the files in a playlist.
   *
   * @param outputDirectory
   * @param commandFile
   * @param playlistId
   * @param downloadType
   * @return
   */
  public static long downloadPlaylist(Path outputDirectory, Path commandFile,
      String playlistId, DownloadType downloadType) {
    return processPlaylist(outputDirectory, playlistId,
        videoDownload -> downloadVideo(outputDirectory, commandFile, videoDownload,
            downloadType));

  }

  /**
   * Download a single file.
   *
   * @param outputDirectory
   * @param commandFile
   * @param videoResource
   * @param downloadType
   * @return
   */
  public static boolean downloadVideo(Path outputDirectory, Path commandFile,
      VideoResource videoResource, DownloadType downloadType) {
    Preconditions.checkNotNull(outputDirectory, "Invalid output directory");
    Preconditions.checkNotNull(videoResource, "Invalid video resource");
    Preconditions.checkNotNull(downloadType, "Invalid download type");
    String url = String.format(VIDEO_URL_TEMPLATE, videoResource.getVideoId());
    LOG.info("Downloading video {}", url);
    YouTubeParser parser = new YouTubeParser() {
      @Override
      public List<YouTubeParser.VideoDownload> extractLinks(YouTubeInfo info) {
        return filterStreamCombined(super.extractLinks(info), downloadType);
      }

      @Override
      public List<YouTubeParser.VideoDownload> extractLinks(YouTubeInfo info,
          AtomicBoolean stop, Runnable notify) {
        return filterStreamCombined(super.extractLinks(info, stop, notify), downloadType);
      }
    };
    try {
      File rawOutput = outputDirectory.resolve(RAW_OUTPUT_FOLDER).toFile();
      if (!rawOutput.exists()) {
        rawOutput.mkdirs();
      }
      VGet vGet = new VGet(new URL(url), rawOutput);
      VideoInfo videoInfo = vGet.getVideo();
      vGet.download(parser, new AtomicBoolean(false), new DownloadProgress(videoInfo));
      for (VideoFileInfo videoFileInfo : videoInfo.getInfo()) {
        File file = videoFileInfo.getTarget();
        if (file != null && file.exists()) {
          videoResource.setTitle(FilenameUtils.removeExtension(file.getName()));
          break;
        }
      }
      return postProcessMediaFiles(outputDirectory, commandFile);
    } catch (Exception e) {
      LOG.error(String.format("Error downloading video %s", url), e);
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  private static long processPlaylist(Path outputDirectory, String playlistId,
      Function<VideoResource, Boolean> callback) {
    Preconditions.checkNotNull(outputDirectory, "Invalid output directory");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(playlistId),
        "Invalid playlist ID");
    Preconditions.checkNotNull(callback, "Invalid callback");
    long totalProcessed = 0L;
    CloseableHttpClient httpClient = null;
    try {
      Path idsOutputPath = outputDirectory.resolve(IDS_FOLDER);
      SSLContext sslContext = new SSLContextBuilder()
          .loadTrustMaterial(null, (certificate, authType) -> true).build();
      httpClient = HttpClients.custom().setSSLContext(sslContext)
          .setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
      String endpoint = String.format(PLAYLIST_URL_TEMPLATE, playlistId);
      String nextPageToken = null;
      do {
        String nextPage = Strings.isNullOrEmpty(nextPageToken) ? endpoint
            : new StringBuilder().append(endpoint).append("&pageToken=")
                .append(nextPageToken).toString();
        try {
          LOG.info("Contacting url {}", nextPage);
          HttpGet httpGet = new HttpGet(nextPage);
          httpGet.setHeader("Accept", "application/json");
          httpGet.setHeader("Content-type", "application/json");
          CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
          int statusCode = httpResponse.getStatusLine().getStatusCode();
          if (statusCode != HttpStatus.SC_OK) {
            throw new RuntimeException(
                String.format("Error code %d for %s", statusCode, endpoint));
          }
          String responseMessage = EntityUtils.toString(httpResponse.getEntity());
          Map<String, Object> map = JsonUtils.GSON.fromJson(responseMessage,
              JsonUtils.MAP_TYPE);
          nextPageToken = (String) map.get("nextPageToken");
          Collection<Object> items = (Collection<Object>) map.get("items");
          totalProcessed += filterVideoResources(idsOutputPath,
              (items.stream().map(item -> {
                Map<String, Object> itemProperties = (Map<String, Object>) item;
                return itemProperties.get("snippet");
              }).map(snippet -> {
                Map<String, Object> snippetProperties = (Map<String, Object>) snippet;
                Map<String, Object> resourceId = (Map<String, Object>) snippetProperties
                    .get("resourceId");
                VideoResource videoResource = new VideoResource();
                videoResource.setKind((String) resourceId.get("kind"));
                videoResource.setVideoId((String) resourceId.get("videoId"));
                videoResource.setTitle((String) snippetProperties.get("title"));
                return videoResource;
              }).collect(Collectors.toList()))).stream().map(videoResource -> {
                if (callback.apply(videoResource)) {
                  saveVideoId(idsOutputPath, videoResource);
                  return true;
                }
                return false;
              }).filter(success -> success).count();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      } while (!Strings.isNullOrEmpty(nextPageToken));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return totalProcessed;
  }

  private static void saveVideoId(Path idsOutputPath, VideoResource videoResource) {
    File idsOutputFile = idsOutputPath.toFile();
    if (!idsOutputFile.exists()) {
      idsOutputFile.mkdirs();
    }
    try (OutputStream outputStream = new FileOutputStream(
        idsOutputPath.resolve(videoResource.getVideoId()).toFile())) {
      outputStream.write(videoResource.toString().getBytes());
    } catch (Exception e) {
      LOG.error(String.format("Error saving ID %s ", videoResource), e);
    }
  }

  private static List<VideoResource> filterVideoResources(Path idsOutputPath,
      List<VideoResource> videoResources) {
    if (CollectionUtils.isEmpty(videoResources)) {
      return Collections.emptyList();
    }
    List<VideoResource> newVideoResources = Lists.newArrayList(videoResources);
    File idsOutputFile = idsOutputPath.toFile();
    if (!idsOutputFile.exists()) {
      return newVideoResources;
    }
    try {
      Files.walk(idsOutputPath).forEach(path -> {
        Path fileName = path.getFileName();
        LOG.debug("Filename {}", fileName);
        if (fileName != null && !path.toFile().isDirectory()) {
          Iterator<VideoResource> videoResourceIter = newVideoResources.iterator();
          while (videoResourceIter.hasNext()) {
            VideoResource videoResource = videoResourceIter.next();
            if (fileName.toString().equals(videoResource.getVideoId())) {
              LOG.info("Already downloaded {}", videoResource);
              videoResourceIter.remove();
            }
          }
        }
      });
      return newVideoResources;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<YouTubeParser.VideoDownload> filterStreamCombined(
      List<YouTubeParser.VideoDownload> videoDownloads, DownloadType downloadType) {
    return videoDownloads.stream().filter(videoDownload -> {
      if (downloadType == DownloadType.AUDIO_ONLY) {
        return videoDownload.stream instanceof StreamAudio;
      }
      if (downloadType == DownloadType.AUDIO_VIDEO) {
        return videoDownload.stream instanceof StreamCombined;
      }
      return videoDownload.stream instanceof StreamVideo;
    }).collect(Collectors.toList());
  }

  private static boolean postProcessMediaFiles(Path inputDirectory, Path commandFile) {
    CommandExecutor executor = new CommandExecutor();
    File finalOutput = inputDirectory.resolve(FINAL_OUTPUT_FOLDER).toFile();
    if (!finalOutput.exists()) {
      finalOutput.mkdirs();
    }
    File rawOutput = inputDirectory.resolve(RAW_OUTPUT_FOLDER).toFile();
    try (CommandInfoReader reader = new CommandInfoReader(
        new FileInputStream(commandFile.toFile()), commandInfo -> {
          commandInfo.setWorkingDirectory(rawOutput.getAbsolutePath());
          return commandInfo;
        })) {
      executor.waitExecute(reader, true);
      FileUtils.forceDelete(rawOutput);
      return true;
    } catch (Exception e) {
      LOG.error("Error processing files ", e);
      return false;
    }
  }
}
