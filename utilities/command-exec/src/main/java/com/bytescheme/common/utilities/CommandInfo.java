package com.bytescheme.common.utilities;

import java.io.Serializable;

import com.google.gson.Gson;

public class CommandInfo implements Serializable {
  private static final long serialVersionUID = 1L;

  private String script;
  private String workingDirectory;
  private Integer exitValue;
  private Long completionWaitTime;
  private Integer executionTimes;
  private Long sleepTime;
  private boolean isSync;

  public String getScript() {
    return script;
  }

  public void setScript(String script) {
    this.script = script;
  }

  public String getWorkingDirectory() {
    return workingDirectory;
  }

  public void setWorkingDirectory(String workingDirectory) {
    this.workingDirectory = workingDirectory;
  }

  public Integer getExitValue() {
    return exitValue;
  }

  public void setExitValue(Integer exitValue) {
    this.exitValue = exitValue;
  }

  public Long getCompletionWaitTime() {
    return completionWaitTime;
  }

  public void setCompletionWaitTime(Long completionWaitTime) {
    this.completionWaitTime = completionWaitTime;
  }

  public Integer getExecutionTimes() {
    return executionTimes;
  }

  public void setExecutionTimes(Integer executionTimes) {
    this.executionTimes = executionTimes;
  }

  public Long getSleepTime() {
    return sleepTime;
  }

  public void setSleepTime(Long sleepTime) {
    this.sleepTime = sleepTime;
  }

  public boolean isSync() {
    return isSync;
  }

  public void setSync(boolean isSync) {
    this.isSync = isSync;
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
