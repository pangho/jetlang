package org.jetlang.fibers;

import org.jetlang.core.Disposable;
import org.jetlang.core.RunnableInvoker;
import org.jetlang.core.RunnableSchedulerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.Executor;

/// <summary>
/// Process Queue that uses a thread pool for execution.

/// </summary>
public class PoolFiber implements Fiber {
    private boolean _flushPending = false;
    private final Object _lock = new Object();
    private final List<Runnable> _queue = new ArrayList<Runnable>();
    private final Executor _pool;
    private ExecutionState _started = ExecutionState.Created;
    private final RunnableInvoker _executor;
    private final ArrayList<Disposable> _onStop = new ArrayList<Disposable>();
    private final RunnableSchedulerImpl _scheduler;
    private final Runnable _flushRunnable;

    /// <summary>
    /// Construct new instance.
    /// </summary>
    /// <param name="pool"></param>
    /// <param name="executor"></param>
    public PoolFiber(Executor pool, RunnableInvoker executor, Timer timer) {
        _pool = pool;
        _executor = executor;
        _scheduler = new RunnableSchedulerImpl(this, timer);
        _flushRunnable = new Runnable() {
            public void run() {
                flush();
            }
        };
    }

    /// <summary>
    /// Queue command.
    /// </summary>
    /// <param name="commands"></param>
    public void execute(Runnable commands) {
        if (_started == ExecutionState.Stopped) {
            return;
        }

        synchronized (_lock) {
            _queue.add(commands);
            if (_started == ExecutionState.Created) {
                return;
            }
            if (!_flushPending) {
                _pool.execute(_flushRunnable);
                _flushPending = true;
            }
        }
    }

    private void flush() {
        Runnable[] toExecute = ClearCommands();
        if (toExecute != null) {
            _executor.executeAll(toExecute);
            synchronized (_lock) {
                if (_queue.size() > 0) {
                    // don't monopolize thread.
                    _pool.execute(_flushRunnable);
                } else {
                    _flushPending = false;
                }
            }
        }
    }

    private Runnable[] ClearCommands() {
        synchronized (_lock) {
            if (_queue.size() == 0) {
                _flushPending = false;
                return null;
            }
            Runnable[] toReturn = _queue.toArray(new Runnable[_queue.size()]);
            _queue.clear();
            return toReturn;
        }
    }

    public void start() {
        if (_started == ExecutionState.Running) {
            throw new RuntimeException("Already Started");
        }
        _started = ExecutionState.Running;
        //flush any pending events in execute
        Runnable flushPending = new Runnable() {
            public void run() {
            }
        };
        execute(flushPending);
    }


    /// <summary>
    /// Stop consuming events.
    /// </summary>
    public void dispose() {
        _started = ExecutionState.Stopped;
        synchronized (_onStop) {
            for (Disposable r : _onStop.toArray(new Disposable[_onStop.size()]))
                r.dispose();
        }
    }

    public void addOnStop(Disposable runOnStop) {
        synchronized (_onStop) {
            _onStop.add(runOnStop);
        }
    }

    public boolean removeOnStop(Disposable disposable) {
        synchronized (_onStop) {
            return _onStop.remove(disposable);
        }
    }

    public int registeredDisposableSize() {
        synchronized (_onStop) {
            return _onStop.size();
        }
    }

    /// <summary>
    /// Schedules an event to be executes once.
    /// </summary>
    /// <param name="command"></param>
    /// <param name="firstIntervalInMs"></param>
    /// <returns>a controller to dispose the event.</returns>
    public Disposable schedule(Runnable command, long firstIntervalInMs) {
        return _scheduler.schedule(command, firstIntervalInMs);
    }/// <summary>

    /// Schedule an event on a recurring interval.
    /// </summary>
    /// <param name="command"></param>
    /// <param name="firstIntervalInMs"></param>
    /// <param name="regularIntervalInMs"></param>
    /// <returns>controller to dispose timer.</returns>
    public Disposable scheduleOnInterval(Runnable command, long firstIntervalInMs, long regularIntervalInMs) {
        //the timer object is shared so interval timers must be shut down manually.
        final Disposable stopper = _scheduler.scheduleOnInterval(command, firstIntervalInMs, regularIntervalInMs);
        final Disposable wrapper = new Disposable() {
            public void dispose() {
                stopper.dispose();
                removeOnStop(this);
            }
        };
        addOnStop(wrapper);
        return wrapper;
    }
}
