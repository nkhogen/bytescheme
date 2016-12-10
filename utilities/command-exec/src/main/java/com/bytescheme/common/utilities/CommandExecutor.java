package com.bytescheme.common.utilities;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bytescheme.common.utils.JsonUtils;
import com.google.common.base.Preconditions;

/**
 *
 * @author Naorem Khogendro Singh
 *
 */
public class CommandExecutor {
  private static final Logger LOG = LoggerFactory.getLogger(CommandExecutor.class);
  private final CommandProcessDestroyer processDestroyer = new CommandProcessDestroyer();

  private CommandExecuteHandler execute(CommandInfo commandInfo)
      throws ExecuteException, IOException, InterruptedException {
    Preconditions.checkNotNull(commandInfo);
    CommandLine commandLine = CommandLine.parse(commandInfo.getScript());
    final CommandExecuteHandler resultHandler = new CommandExecuteHandler(commandInfo);
    DefaultExecutor executor = new DefaultExecutor() {
      @Override
      protected Process launch(final CommandLine commandLine,
          final Map<String, String> env, final File dir) throws IOException {
        resultHandler.setCommandStartTime(System.currentTimeMillis());
        return super.launch(commandLine, env, dir);
      }
    };
    executor.setProcessDestroyer(processDestroyer);
    PumpStreamHandler streamHandler = new PumpStreamHandler();
    executor.setStreamHandler(streamHandler);
    executor.setExitValue(commandInfo.getExitValue());
    if (commandInfo.getCompletionWaitTime() != null) {
      ExecuteWatchdog watchdog = new ExecuteWatchdog(commandInfo.getCompletionWaitTime());
      executor.setWatchdog(watchdog);
    }
    executor.setWorkingDirectory(new File(commandInfo.getWorkingDirectory()));
    executor.execute(commandLine, resultHandler);
    return resultHandler;
  }

  public void waitExecute(Iterator<CommandInfo> commandInfoIterator, boolean isWait) {
    Preconditions.checkNotNull(commandInfoIterator);
    List<CommandExecuteHandler> resultHandlers = new LinkedList<CommandExecuteHandler>();
    while (commandInfoIterator.hasNext()) {
      CommandInfo commandInfo = commandInfoIterator.next();
      setDefaults(commandInfo);
      try {
        CommandExecuteHandler resultHandler = execute(commandInfo);
        resultHandlers.add(resultHandler);
        if (commandInfo.isWait()) {
          resultHandler.waitFor();
        }
      } catch (Exception e) {
        LOG.error(
            String.format("Failed to submit command %s", JsonUtils.toJson(commandInfo)),
            e);
      }
    }
    if (isWait) {
      for (CommandExecuteHandler resultHandler : resultHandlers) {
        try {
          resultHandler.waitFor();
        } catch (InterruptedException e) {
          LOG.error("Interrupted while waiting for commands to complete", e);
        }
      }
    }
  }

  private void setDefaults(CommandInfo commandInfo) {
    if (commandInfo.getExitValue() == null) {
      commandInfo.setExitValue(0);
    }
    if (commandInfo.getWorkingDirectory() == null) {
      commandInfo.setWorkingDirectory(".");
    }
  }

  public int getProcessCount() {
    return processDestroyer.size();
  }

  public void destroyProcesses() {
    processDestroyer.destroyProcesses();
  }
}
