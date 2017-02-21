package com.bytescheme.tools.youtube;

import java.util.List;
import java.util.Map;

import com.github.axet.vget.info.VideoFileInfo;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.vget.info.VideoInfo.States;
import com.github.axet.vget.vhs.VimeoInfo;
import com.github.axet.vget.vhs.YouTubeInfo;
import com.github.axet.wget.SpeedInfo;
import com.github.axet.wget.info.DownloadInfo.Part;
import com.google.common.collect.Maps;

/**
 * Download progress processor.
 */
public class DownloadProgress implements Runnable {
  private final VideoInfo videoinfo;
  private final Map<VideoFileInfo, SpeedInfo> map = Maps.newHashMap();
  private long last;

  public DownloadProgress(VideoInfo videoInfo) {
    this.videoinfo = videoInfo;
  }

  public SpeedInfo getSpeedInfo(VideoFileInfo dinfo) {
    SpeedInfo speedInfo = map.get(dinfo);
    if (speedInfo == null) {
      speedInfo = new SpeedInfo();
      speedInfo.start(dinfo.getCount());
      map.put(dinfo, speedInfo);
    }
    return speedInfo;
  }

  @Override
  public void run() {
    List<VideoFileInfo> videoInfos = videoinfo.getInfo();
    switch (videoinfo.getState()) {
      case EXTRACTING:
      case EXTRACTING_DONE:
      case DONE:
        if (videoinfo instanceof YouTubeInfo) {
          YouTubeInfo info = (YouTubeInfo) videoinfo;
          System.out.println(
              String.format("%s %s", videoinfo.getState(), info.getVideoQuality()));
        } else if (videoinfo instanceof VimeoInfo) {
          VimeoInfo info = (VimeoInfo) videoinfo;
          System.out.println(
              String.format("%s %s", videoinfo.getState(), info.getVideoQuality()));
        } else {
          System.out.println("downloading unknown quality");
        }
        videoinfo.getInfo().forEach(videoInfo -> {
          SpeedInfo speedInfo = getSpeedInfo(videoInfo);
          speedInfo.end(videoInfo.getCount());
          System.out
              .println(String.format("file:%d - %s (%s)", videoInfos.indexOf(videoInfo),
                  videoInfo.targetFile, formatSpeed(speedInfo.getAverageSpeed())));
        });
        break;
      case ERROR:
        System.out
            .println(String.format("%s %d", videoinfo.getState(), videoinfo.getDelay()));

        if (videoInfos != null) {
          videoInfos.forEach(info -> {
            System.out.println(String.format("file: %d - %s delay: %d",
                videoInfos.indexOf(info), info.getException(), info.getDelay()));
          });
        }
        break;
      case RETRYING:
        System.out.println(videoinfo.getState() + " " + videoinfo.getDelay());

        if (videoInfos != null) {
          videoInfos.forEach(info -> {
            System.out.println(
                String.format("file: %d - %s %s delay: %d", videoInfos.indexOf(info),
                    info.getState(), info.getException(), info.getDelay()));
          });
        }
        break;
      case DOWNLOADING:
        long now = System.currentTimeMillis();
        if (now - 1000 > last) {
          last = now;
          StringBuilder parts = new StringBuilder();
          videoInfos.forEach(info -> {
            SpeedInfo speedInfo = getSpeedInfo(info);
            speedInfo.step(info.getCount());

            List<Part> pp = info.getParts();
            if (pp != null) {
              // multipart download
              for (Part p : pp) {
                if (p.getState().equals(States.DOWNLOADING)) {
                  parts.append(String.format("part#%d(%.2f) ", p.getNumber(),
                      p.getCount() / (float) p.getLength()));
                }
              }
            }
            System.out.println(
                String.format("file:%d - %s %.2f %s (%s)", videoInfos.indexOf(info),
                    videoinfo.getState(), info.getCount() / (float) info.getLength(),
                    parts, formatSpeed(speedInfo.getCurrentSpeed())));
          });
        }
        break;
      default:
        break;
    }
  }

  private static String formatSpeed(long s) {
    if (s > 0.1 * 1024 * 1024 * 1024) {
      float f = s / 1024f / 1024f / 1024f;
      return String.format("%.1f GB/s", f);
    }
    if (s > 0.1 * 1024 * 1024) {
      float f = s / 1024f / 1024f;
      return String.format("%.1f MB/s", f);
    }
    float f = s / 1024f;
    return String.format("%.1f KB/s", f);
  }
}
