package com.bytescheme.service.controlboard.remoteobjects;

import java.util.UUID;

import com.bytescheme.service.controlboard.common.remoteobjects.BaseMockControlBoard;
import com.bytescheme.service.controlboard.video.VideoBroadcastHandler;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.api.client.repackaged.com.google.common.base.Strings;

public class TargetMockControlBoardImpl extends BaseMockControlBoard {

  private static final long serialVersionUID = 1L;
  private final String videoUrlFormat;

  public TargetMockControlBoardImpl(UUID objectId, String videoUrlFormat) {
    super(objectId);
    Preconditions.checkNotNull(!Strings.isNullOrEmpty(videoUrlFormat),
        "Invalid video URL format");
    this.videoUrlFormat = videoUrlFormat;
  }

  @Override
  public String getVideoUrl() {
    String secret = VideoBroadcastHandler.getInstance().generateSecret();
    return String.format(videoUrlFormat, secret);
  }

}
