package com.bytescheme.rpc.core;

import java.util.Stack;
import java.util.UUID;

import com.google.common.base.Preconditions;

/**
 * A simple call stack tracker.
 * @author Naorem Khogendro Singh
 *
 */
public final class MethodCallRecorder {
  public interface LogMessageHandler {
    void process(UUID requestId, long elapsedTime, long methodTime);
  }

  private MethodCallRecorder() {

  }

  static class CallStats {
    long startTime = 0L;
    long endTime = 0L;
  }

  static class CallStack {
    UUID requestId = null;
    long startTime = 0L;
    Stack<CallStats> stack = new Stack<CallStats>();
  }

  private static final ThreadLocal<CallStack> callStacks = new ThreadLocal<CallStack>() {
    @Override
    protected CallStack initialValue() {
      return new CallStack();
    }
  };

  public static void init(UUID requestId, LogMessageHandler logMessageHandler) {
    Preconditions.checkNotNull(requestId);
    Preconditions.checkNotNull(logMessageHandler);
    CallStack callStack = callStacks.get();
    callStack.stack.clear();
    callStack.startTime = System.currentTimeMillis();
    callStack.requestId = requestId;
    CallStats callStats = new CallStats();
    callStats.startTime = callStack.startTime;
    callStack.stack.push(callStats);
    logMessageHandler.process(callStack.requestId, 0L, 0L);
  }

  public static void uninit(LogMessageHandler logMessageHandler) {
    try {
      Preconditions.checkNotNull(logMessageHandler);
      CallStack callStack = callStacks.get();
      if (callStack.stack.size() != 1) {
        callStack.stack.clear();
        throw new IllegalStateException("Mismatch method call stack");
      }
      CallStats callStats = callStack.stack.pop();
      callStats.endTime = System.currentTimeMillis();
      logMessageHandler.process(callStack.requestId, callStats.endTime - callStack.startTime,
          callStats.endTime - callStats.startTime);
    } finally {
      callStacks.remove();
    }
  }

  public static void enter(LogMessageHandler logMessageHandler) {
    Preconditions.checkNotNull(logMessageHandler);
    CallStack callStack = callStacks.get();
    if (callStack.requestId == null) {
      throw new IllegalStateException("Call stack has not been initialized");
    }
    CallStats callStats = new CallStats();
    callStats.startTime = System.currentTimeMillis();
    callStack.stack.push(callStats);
    logMessageHandler.process(callStack.requestId, callStats.startTime - callStack.startTime, 0L);
  }

  public static void exit(LogMessageHandler logMessageHandler) {
    Preconditions.checkNotNull(logMessageHandler);
    CallStack callStack = callStacks.get();
    if (callStack.requestId == null) {
      throw new IllegalStateException("Call stack has not been initialized");
    }
    if (callStack.stack.isEmpty()) {
      throw new IllegalStateException("Mismatch method call stack");
    }
    CallStats callStats = callStack.stack.pop();
    callStats.endTime = System.currentTimeMillis();
    logMessageHandler.process(callStack.requestId, callStats.startTime - callStack.startTime,
        callStats.endTime - callStats.startTime);
  }
}
