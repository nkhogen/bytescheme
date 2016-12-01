package com.bytescheme.common.utilities;

import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.ExecuteException;

/**
 * Command executor.
 *
 * @author Naorem Khogendro Singh
 *
 */
public class CommandExecuteHandler extends DefaultExecuteResultHandler {
	private final CommandInfo commandInfo;
	private long commandStartTime = 0L;
	private long commandEndTime = 0L;

	public CommandExecuteHandler(CommandInfo commandInfo) {
		this.commandInfo = commandInfo;
	}

	public CommandInfo getCommandInfo() {
		return commandInfo;
	}

	public long getCommandStartTime() {
		return commandStartTime;
	}

	public void setCommandStartTime(long commandStartTime) {
		this.commandStartTime = commandStartTime;
	}

	public long getCommandEndTime() {
		return commandEndTime;
	}

	public void setCommandEndTime(long commandEndTime) {
		this.commandEndTime = commandEndTime;
	}

	@Override
	public void onProcessComplete(final int exitValue) {
		commandEndTime = System.currentTimeMillis();
		super.onProcessComplete(exitValue);
	}

	@Override
	public void onProcessFailed(final ExecuteException e) {
		commandEndTime = System.currentTimeMillis();
		super.onProcessFailed(e);
	}
}
