package com.bytescheme.common.utilities;

import java.io.Serializable;

import com.google.gson.Gson;

/**
 *
 * @author Naorem Khogendro Singh
 *
 */
public class CommandInfo implements Serializable {
  private static final long serialVersionUID = 1L;

  private String script;
  private String workingDirectory;
  private Integer exitValue;
  private Long completionWaitTime;
  private boolean isWait;
  private boolean invokeShell;

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

  public boolean isWait() {
    return isWait;
  }

  public void setWait(boolean isWait) {
    this.isWait = isWait;
  }

  public boolean isInvokeShell() {
    return invokeShell;
  }

  public void setInvokeShell(boolean invokeShell) {
    this.invokeShell = invokeShell;
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
