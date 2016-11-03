package com.yunxinlink.notes.sync.download;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executors are used in entire FileDownloader internal for managing different threads.
 * <p>
 * All thread pools in FileDownloader will comply with:
 * <p>
 * The default thread count is 0, and the maximum pool size is {@code nThreads}; When there are less
 * than {@code nThreads} threads running, a new thread is created to handle the request, but when it
 * turn to idle and the interval time of waiting for new task more than {@code DEFAULT_IDLE_SECOND}
 * second, the thread will be terminate to reduce the cost of resources.
 */
public class DownloadExecutors {
    private final static int DEFAULT_IDLE_SECOND = 5;

    public static ThreadPoolExecutor newDefaultThreadPool(int nThreads, String prefix) {
        return newDefaultThreadPool(nThreads, new LinkedBlockingQueue<Runnable>(), prefix);
    }

    public static ThreadPoolExecutor newDefaultThreadPool(int nThreads,
                                                          LinkedBlockingQueue<Runnable> queue,
                                                          String prefix) {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(nThreads, nThreads,
                DEFAULT_IDLE_SECOND, TimeUnit.SECONDS, queue, new DownloadThreadFactory(prefix));
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    static class DownloadThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        
        private final static String DOWNLOADER_PREFIX = "Downloader";

        
        private final String namePrefix;
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        DownloadThreadFactory(String prefix) {
            group = Thread.currentThread().getThreadGroup();
            namePrefix = getThreadPoolName(prefix);
        }

        private static String getThreadPoolName(String name) {
            return DOWNLOADER_PREFIX + "-" + name;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);

            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

}