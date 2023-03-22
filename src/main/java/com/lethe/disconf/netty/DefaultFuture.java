package com.lethe.disconf.netty;

import com.baidu.disconf.core.common.remote.Request;
import com.baidu.disconf.core.common.remote.Response;
import io.netty.channel.Channel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2023/2/22 15:13
 * @Version : 1.0
 * @Copyright : Copyright (c) 2023 All Rights Reserved
 **/
public class DefaultFuture {

    private static final Logger logger = LoggerFactory.getLogger(DefaultFuture.class);

    private static final Map<String, Channel> CHANNELS = new ConcurrentHashMap<>();

    private static final Map<String, DefaultFuture> FUTURES = new ConcurrentHashMap<>();

    private final Channel channel;
    private final Request request;
    private final String requestId;
    private volatile Response response;

    private final long start = System.currentTimeMillis();

    private final Lock lock = new ReentrantLock();
    private final Condition done = lock.newCondition();

    private int timeout = 10000;

    static {
        Thread th = new Thread(new FutureTimeoutScan(), "ResponseTimeoutScanTimer");
        th.setDaemon(true);
        th.start();
    }

    public DefaultFuture(Channel channel, Request request) {
        this.channel = channel;
        this.request = request;
        this.requestId = request.getRequestId();
        // 当前Future和请求信息的映射
        FUTURES.put(requestId, this);
        // 当前Channel和请求信息的映射
        CHANNELS.put(requestId, channel);
    }

    public Object get() {
        if (!isDone()) {
            long start = System.currentTimeMillis();
            lock.lock();
            try {
                while (!isDone()) {
                    // 通过加锁等待
                    done.await(timeout, TimeUnit.MILLISECONDS);
                    if (isDone() || System.currentTimeMillis() - start > timeout) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
            if (!isDone()) {
                throw new RuntimeException("timeout");
            }
        }
        return returnFromResponse();
    }

    private Object returnFromResponse() {
        Response res = response;
        if (res == null) {
            throw new IllegalStateException("response cannot be null");
        }

        if (StringUtils.isBlank(res.getErrorCode())) {
            return res;
        }

        throw new RuntimeException(res.getMessage());

    }

    public static void received(Channel channel, Response response) {
        try {
            DefaultFuture future = FUTURES.remove(response.getRequestId());
            if (future != null) {
                future.doReceived(response);
            }
        } finally {
            CHANNELS.remove(response.getRequestId());
        }
    }

    private void doReceived(Response res) {
        lock.lock();
        try {
            // response赋值
            response = res;
            // 唤醒等到中的
            done.signal();
        } finally {
            lock.unlock();
        }
    }

    public boolean isDone() {
        return response != null;
    }



    private static class FutureTimeoutScan implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    // 扫描FUTURES
                    for (DefaultFuture future : FUTURES.values()) {
                        if (future == null || future.isDone()) {
                            continue;
                        }
                        // 如果future未完成，且超时
                        if (System.currentTimeMillis() - future.getStartTimestamp() > future.getTimeout()) {
                            // create exception response.
                            Response timeoutResponse = new Response();
                            timeoutResponse.setRequestId(future.getRequestId());
                            timeoutResponse.setErrorCode("10010");
                            timeoutResponse.setMessage(", channel: " + future.getChannel() + "TimeOut");
                            // handle response.
                            DefaultFuture.received(future.getChannel(), timeoutResponse);
                        }
                    }
                    Thread.sleep(30);
                } catch (Throwable e) {
                    logger.error("Exception when scan the timeout invocation of remoting.", e);
                }
            }
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public String getRequestId() {
        return requestId;
    }

    private long getStartTimestamp() {
        return start;
    }

    private int getTimeout() {
        return timeout;
    }

}
