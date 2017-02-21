package com.bytescheme.tools.youtube;

import com.bytescheme.common.utils.JsonUtils;

/**
 * POJO for a video resource.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class VideoResource {
  private String title;
  private String kind;
  private String videoId;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getVideoId() {
    return videoId;
  }

  public void setVideoId(String videoId) {
    this.videoId = videoId;
  }

  @Override
  public String toString() {
    return JsonUtils.toJson(this);
  }
}
