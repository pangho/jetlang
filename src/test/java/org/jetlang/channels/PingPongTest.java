package org.jetlang.channels;

import org.jetlang.PerfTimer;
import org.jetlang.core.Callback;
import org.jetlang.fibers.Fiber;
import org.jetlang.fibers.PoolFiberFactory;
import org.jetlang.fibers.ThreadFiber;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * User: mrettig
 * Date: Jul 29, 2008
 * Time: 8:33:07 PM
 */
public class PingPongTest {

    @Test
    public void pingPongWithThreads() throws InterruptedException {

        ThreadFiber pingFiber = new ThreadFiber();
        pingFiber.start();

        ThreadFiber pongFiber = new ThreadFiber();
        pongFiber.start();
        runTest(pingFiber, pongFiber);
    }

    @Test
    public void pingPongWithThreadPool() throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(2);
        PoolFiberFactory factory = new PoolFiberFactory(executor);
        Fiber pingFiber = factory.create();
        pingFiber.start();

        Fiber pongFiber = factory.create();
        pongFiber.start();
        runTest(pingFiber, pongFiber);
        factory.dispose();
        executor.shutdown();
    }


    private void runTest(Fiber pingFiber, Fiber pongFiber) throws InterruptedException {
        final Channel<Integer> pongChannel = new MemoryChannel<Integer>();
        final MemoryChannel<Channel<Integer>> pingChannel = new MemoryChannel<Channel<Integer>>();

        final Integer max = 1000;
        final CountDownLatch reset = new CountDownLatch(1);

        Callback<Integer> onPong = new Callback<Integer>() {
            public void onMessage(Integer count) {
                if (count.equals(max)) {
                    reset.countDown();
                } else {
                    pingChannel.publish(pongChannel);
                }
            }
        };

        Callback<Channel<Integer>> onPing = new Callback<Channel<Integer>>() {
            private int pingCount;

            public void onMessage(Channel<Integer> replyChannel) {
                pingCount++;
                replyChannel.publish(pingCount);
            }
        };

        pingChannel.subscribe(pingFiber, onPing);
        pongChannel.subscribe(pongFiber, onPong);
        PerfTimer timer = new PerfTimer(max);
        try {
            pingChannel.publish(pongChannel);
            boolean result = reset.await(30, TimeUnit.SECONDS);
            assertTrue(result);
        } finally {
            timer.dispose();
            pingFiber.dispose();
            pongFiber.dispose();
        }
    }


}
