package com.lethe.disconf.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2022/11/14 18:16
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
public class DisconfThreadFactory implements ThreadFactory {

    private final AtomicLong threadNumber = new AtomicLong(1);

    private static final ThreadGroup threadGroup = new ThreadGroup("Disconf");

    private final String namePrefix;

    private final boolean daemon;

    public static ThreadFactory create(String namePrefix, boolean daemon) {
        return new DisconfThreadFactory(namePrefix, daemon);
    }


    private DisconfThreadFactory(String namePrefix, boolean daemon) {
        this.namePrefix = namePrefix;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(threadGroup, runnable,//
                threadGroup.getName() + "-" + namePrefix + "-" + threadNumber.getAndIncrement());
        thread.setDaemon(daemon);
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }
}
