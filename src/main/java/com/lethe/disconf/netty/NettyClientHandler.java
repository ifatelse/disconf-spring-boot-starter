package com.lethe.disconf.netty;

import com.baidu.disconf.client.common.model.DisconfCenterFile;
import com.baidu.disconf.core.common.remote.ConfigChangeResponse;
import com.baidu.disconf.core.common.remote.ConfigQueryRequest;
import com.baidu.disconf.core.common.remote.ConfigQueryResponse;
import com.baidu.disconf.core.common.remote.HeartBeatRequest;
import com.baidu.disconf.core.common.remote.netty.Message;
import com.baidu.disconf.core.common.utils.ClassLoaderUtil;
import com.baidu.disconf.core.common.utils.FileUtils;
import com.baidu.disconf.core.common.utils.GsonUtils;
import com.lethe.disconf.client.NettyClientInvoke;
import com.lethe.disconf.internals.ConfigRepositoryManager;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2023/2/13 15:21
 * @Version : 1.0
 * @Copyright : Copyright (c) 2023 All Rights Reserved
 **/
@ChannelHandler.Sharable
public class NettyClientHandler extends ChannelDuplexHandler {

    private static final Log log = LogFactory.getLog(NettyClientHandler.class);

    private static final ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(1,
            new DefaultThreadFactory("client-heartbeat", true));

    private NettyClientInvoke nettyClientInvoke;

    public NettyClientHandler(NettyClientInvoke nettyClientInvoke) {
        this.nettyClientInvoke = nettyClientInvoke;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String address = toAddressString((InetSocketAddress) ctx.channel().remoteAddress());
        log.info("与服务端:" + address + ",建立连接成功");
        startHeartbeatTimer(ctx);
    }

    private void startHeartbeatTimer(ChannelHandlerContext ctx) {
        scheduled.scheduleWithFixedDelay(new HeartBeatTask(ctx), 10000, 10000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("接受到服务端消息：" + msg);

        Message message = (Message) msg;
        String msgType = message.getMsgType();

        if (Objects.equals(msgType, ConfigQueryResponse.class.getSimpleName())) {
            ConfigQueryResponse response = GsonUtils.fromJson(GsonUtils.toJson(message.getData()), ConfigQueryResponse.class);
            if (response.getEvent().equals("loadQuery")) {
                DefaultFuture.received(ctx.channel(), response);
            } else {
                nettyClientInvoke.configChange(response);
            }

        }

        if (Objects.equals(msgType, ConfigChangeResponse.class.getSimpleName())) {
            ConfigChangeResponse response = GsonUtils.fromJson(GsonUtils.toJson(message.getData()), ConfigChangeResponse.class);
            // NettyChannelClient.receivedChangeResponse(response);

            String fileName = response.getFileName();

            if (StringUtils.isBlank(fileName)) {
                return;
            }

            nettyClientInvoke.configChange(fileName);
        }

    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    public static String toAddressString(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    class HeartBeatTask implements Runnable {

        ChannelHandlerContext ctx;

        public HeartBeatTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            HeartBeatRequest heartBeatRequest = new HeartBeatRequest();
            Message message = new Message();
            message.setMsgType(HeartBeatRequest.class.getSimpleName());
            message.setData(heartBeatRequest);
            ctx.channel().writeAndFlush(message);
        }
    }

}
