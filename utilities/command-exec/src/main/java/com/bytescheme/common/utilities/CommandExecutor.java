package com.bytescheme.common.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;

/**
 *
 * @author Naorem Khogendro Singh
 *
 */
public class CommandExecutor {
  private static final Logger LOG = LoggerFactory.getLogger(CommandExecutor.class);
  private static final Gson GSON = new Gson();

  private CommandExecuteHandler execute(CommandInfo commandInfo)
      throws ExecuteException, IOException, InterruptedException {
    Preconditions.checkNotNull(commandInfo);
    CommandLine commandLine = CommandLine.parse(commandInfo.getScript());
    final CommandExecuteHandler resultHandler = new CommandExecuteHandler(commandInfo);
    DefaultExecutor executor = new DefaultExecutor() {
      @Override
      protected Process launch(final CommandLine commandLine, final Map<String, String> env,
          final File dir) throws IOException {
        resultHandler.setCommandStartTime(System.currentTimeMillis());
        return super.launch(commandLine, env, dir);
      }
    };
    PumpStreamHandler streamHandler = new PumpStreamHandler();
    executor.setStreamHandler(streamHandler);
    executor.setExitValue(commandInfo.getExitValue());
    ExecuteWatchdog watchdog = new ExecuteWatchdog(commandInfo.getCompletionWaitTime());
    executor.setWatchdog(watchdog);
    executor.setWorkingDirectory(new File(commandInfo.getWorkingDirectory()));
    executor.execute(commandLine, resultHandler);
    return resultHandler;
  }

  private void waitExecute(CommandInfoReader commandInfoReader) {
    Preconditions.checkNotNull(commandInfoReader);
    List<CommandExecuteHandler> resultHandlers = new LinkedList<CommandExecuteHandler>();
    while (commandInfoReader.hasNext()) {
      CommandInfo commandInfo = commandInfoReader.next();
      setDefaults(commandInfo);
      for (int index = 0; index < commandInfo.getExecutionTimes(); index++) {
        try {
          CommandExecuteHandler resultHandler = execute(commandInfo);
          resultHandlers.add(resultHandler);
          if (!commandInfo.isSync()) {
            continue;
          }
          LOG.info("Sync is set. Waiting for command to complete");
          resultHandler.waitFor();
          if (commandInfo.getSleepTime() == null) {
            continue;
          }
          LOG.info("Pausing for {}ms", commandInfo.getSleepTime());
          try {
            TimeUnit.MILLISECONDS.sleep(commandInfo.getSleepTime());
          } catch (InterruptedException e) {
            LOG.error("Interrupted exception while pausing between two commands", e);
          }

        } catch (Exception e) {
          LOG.error(String.format("Failed to submit command %s", GSON.toJson(commandInfo)), e);
        }
      }
    }
    for (CommandExecuteHandler resultHandler : resultHandlers) {
      try {
        resultHandler.waitFor();
      } catch (InterruptedException e) {
        LOG.error("Interrupted while waiting for commands to complete", e);
      }
    }
    for (CommandExecuteHandler resultHandler : resultHandlers) {
      String commandInfo = GSON.toJson(resultHandler.getCommandInfo());
      long duration = resultHandler.getCommandEndTime() - resultHandler.getCommandStartTime();
      ExecuteException exception = resultHandler.getException();
      if (exception != null) {
        LOG.error(String.format("Error occurred executing command %s. It took %dms", commandInfo,
            duration), exception);
      } else {
        LOG.info("Command {} took {}ms", commandInfo, duration);
      }
    }
  }

  private void setDefaults(CommandInfo commandInfo) {
    if (commandInfo.getCompletionWaitTime() == null) {
      commandInfo.setCompletionWaitTime(2000L);
    }
    if (commandInfo.getExecutionTimes() == null) {
      commandInfo.setExecutionTimes(1);
    }
    if (commandInfo.getExitValue() == null) {
      commandInfo.setExitValue(0);
    }
    if (commandInfo.getWorkingDirectory() == null) {
      commandInfo.setWorkingDirectory(".");
    }
  }

  public static void main(String[] args) throws Exception {
    CommandExecutor executor = new CommandExecutor();
    CommandInfoReader reader = new CommandInfoReader(
        new FileInputStream("src/test/resources/commands.json"));
    executor.waitExecute(reader);
  }
}
