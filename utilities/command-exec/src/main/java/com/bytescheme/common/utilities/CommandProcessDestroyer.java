package com.bytescheme.common.utilities;

import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.exec.ShutdownHookProcessDestroyer;

public class CommandProcessDestroyer extends ShutdownHookProcessDestroyer {
  private final Vector<Process> processes = new Vector<Process>();

  @Override
  public synchronized boolean add(final Process process) {
    boolean isAdded = super.add(process);
    processes.addElement(process);
    return isAdded;
  }

  @Override
  public synchronized boolean remove(Process process) {
    boolean isRemoved = super.remove(process);
    processes.remove(process);
    return isRemoved;
  }

  public synchronized void destroyProcesses() {
    Iterator<Process> iterator = processes.iterator();
    while (iterator.hasNext()) {
      final Process process = iterator.next();
      try {
        process.destroyForcibly();
        if (super.remove(process)) {
          iterator.remove();
        }
      } catch (final Throwable t) {
        System.err.println("Unable to terminate process during process shutdown");
      }
    }
  }
}
