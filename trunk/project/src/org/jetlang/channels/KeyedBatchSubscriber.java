package org.jetlang.channels;    /// <summary>
/// Channel subscription that drops duplicates based upon a key.
/// </summary>
/// <typeparam name="K"></typeparam>
/// <typeparam name="T"></typeparam>

import org.jetlang.core.Callback;
import org.jetlang.core.ProcessFiber;
import org.jetlang.core.RunnableQueue;

import java.util.HashMap;
import java.util.Map;

public class KeyedBatchSubscriber<K, T> implements Subscribable<T> {
    private final Object _batchLock = new Object();

    private final ProcessFiber _context;
    private final Callback<Map<K, T>> _target;
    private final int _flushIntervalInMs;
    private final Converter<T, K> _keyResolver;

    private Map<K, T> _pending = null;
    private final Runnable _flushRunner;

    /// <summary>
    /// Construct new instance.
    /// </summary>
    /// <param name="keyResolver"></param>
    /// <param name="target"></param>
    /// <param name="context"></param>
    /// <param name="flushIntervalInMs"></param>
    public KeyedBatchSubscriber(Converter<T, K> keyResolver,
                                Callback<Map<K, T>> target,
                                ProcessFiber context, int flushIntervalInMs) {
        _keyResolver = keyResolver;
        _context = context;
        _target = target;
        _flushIntervalInMs = flushIntervalInMs;
        _flushRunner = new Runnable() {
            public void run() {
                flush();
            }
        };
    }

    /// <summary>
    /// received on delivery thread
    /// </summary>
    /// <param name="msg"></param>
    public void onMessage(T msg) {
        synchronized (_batchLock) {
            K key = _keyResolver.Convert(msg);
            if (_pending == null) {
                _pending = new HashMap<K, T>();
                _context.schedule(_flushRunner, _flushIntervalInMs);
            }
            _pending.put(key, msg);
        }
    }

    /// <summary>
    /// Flushed from process thread
    /// </summary>
    private void flush() {
        Map<K, T> toReturn = ClearPending();
        if (toReturn != null) {
            _target.onMessage(toReturn);
        }
    }

    private Map<K, T> ClearPending() {
        synchronized (_batchLock) {
            if (_pending == null || _pending.size() == 0) {
                _pending = null;
                return null;
            }
            Map<K, T> toReturn = _pending;
            _pending = null;
            return toReturn;
        }
    }

    public RunnableQueue getQueue() {
        return _context;
    }


}
