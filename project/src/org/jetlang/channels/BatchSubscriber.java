package org.jetlang.channels;    /// <summary>
/// Batches events for the consuming thread.
/// </summary>
/// <typeparam name="T"></typeparam>

import org.jetlang.core.Callback;
import org.jetlang.fibers.Fiber;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BatchSubscriber<T> extends BaseSubscription<T> {
    private final Object _lock = new Object();
    private final Fiber _queue;
    private final Callback<List<T>> _receive;
    private final int _interval;
    private List<T> _pending;
    private final Runnable _flushRunnable;

    /// <summary>
    /// Construct new instance.
    /// </summary>
    /// <param name="queue"></param>
    /// <param name="channel"></param>
    /// <param name="receive"></param>
    /// <param name="interval"></param>
    public BatchSubscriber(Fiber queue, Callback<List<T>> receive, int interval) {
        super(queue);
        _queue = queue;
        _receive = receive;
        _interval = interval;
        _flushRunnable = new Runnable() {
            public void run() {
                Flush();
            }
        };
    }

    /// <summary>
    /// Receives message and batches as needed.
    /// </summary>
    /// <param name="msg"></param>
    @Override
    protected void onMessageOnProducerThread(T msg) {
        synchronized (_lock) {
            if (_pending == null) {
                _pending = new ArrayList<T>();
                _queue.schedule(_flushRunnable, _interval, TimeUnit.MILLISECONDS);
            }
            _pending.add(msg);
        }
    }

    private void Flush() {
        List<T> toFlush = null;
        synchronized (_lock) {
            if (_pending != null) {
                toFlush = _pending;
                _pending = null;
            }
        }
        if (toFlush != null) {
            _receive.onMessage(toFlush);
        }
    }
}
