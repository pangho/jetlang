package org.jetlang.core;

import java.util.ArrayList;
import java.util.List;

/// <summary>
/// A synchronous execute typically used for testing.

/// </summary>
public class SynchronousRunnableQueue implements RunnableQueue, RunnableExecutor {
    private boolean _running = true;
    private List<Stopable> _onStop = new ArrayList<Stopable>();

    /// <summary>
    /// Queue command
    /// </summary>
    /// <param name="command"></param>
    public void execute(Runnable command) {
        if (_running) {
            command.run();
        }
    }

    public void addOnStop(Stopable runOnStop) {
        _onStop.add(runOnStop);
    }

    public boolean removeOnStop(Stopable stopable) {
        return _onStop.remove(stopable);
    }

    public int stoppableSize() {
        return _onStop.size();
    }

    /// <summary>
    /// start Consuming events.
    /// </summary>
    public void run() {
        _running = true;
    }

    /// <summary>
    /// Stop consuming events.
    /// </summary>
    public void stop() {
        _running = false;
        for (Stopable run : _onStop) {
            run.stop();
        }
    }
}