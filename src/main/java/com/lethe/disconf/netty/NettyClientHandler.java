package com.lethe.disconf.netty;

import com.baidu.disconf.core.common.remote.ConfigChangeResponse;
import com.baidu.disconf.core.common.remote.ConfigQueryResponse;
import com.baidu.disconf.core.common.remote.HeartBeatRequest;
import com.baidu.disconf.core.common.utils.GsonUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
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

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        String address = toAddressString((InetSocketAddress) ctx.channel().remoteAddress());
        log.info("与服务端:" + address + ",建立连接成功");

        NettyChannelService.saveChannel(ctx);

        // ConfigQueryRequest configQueryRequest = new ConfigQueryRequest();
        // configQueryRequest.setAppName(DisClientConfig.getInstance().APP);
        // configQueryRequest.setVersion(DisClientConfig.getInstance().VERSION);
        // configQueryRequest.setEnv(DisClientConfig.getInstance().ENV);
        // configQueryRequest.setFileName("config.yml");
        // configQueryRequest.setMsgType(ConfigQueryRequest.class.getSimpleName());
        //
        // ctx.channel().writeAndFlush(GsonUtils.toJson(configQueryRequest));

        startHeartbeatTimer(ctx);
    }

    private void startHeartbeatTimer(ChannelHandlerContext ctx) {
        scheduled.scheduleWithFixedDelay(new HeartBeatTask(ctx),1000, 9000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("接受到服务端消息：" + msg);

        Map<String, Object> objectMap = GsonUtils.toObjectMap((String) msg);

        String msgType = (String) objectMap.get("msgType");

        // Class<?> classType = ResponseRegistry.getClassByType(msgType);

        if (Objects.equals(msgType, ConfigQueryResponse.class.getSimpleName())) {
            ConfigQueryResponse response = GsonUtils.fromJson((String) msg, ConfigQueryResponse.class);
            DefaultFuture.received(ctx.channel(), response);
        }

        if (Objects.equals(msgType, ConfigChangeResponse.class.getSimpleName())) {
            ConfigChangeResponse response = GsonUtils.fromJson((String) msg, ConfigChangeResponse.class);
            NettyChannelService.receivedChangeResponse(response);
        }

    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // send heartbeat when read idle.
        if (evt instanceof IdleStateEvent) {
            log.info("已经5秒未收到服务端的消息了！" + new Date());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String address = toAddressString((InetSocketAddress) ctx.channel().remoteAddress());
        log.error(address + "-----" + ctx.channel().isActive() + "----------" + cause);
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
            heartBeatRequest.setMsgType(HeartBeatRequest.class.getSimpleName());

            ByteBuf data = Unpooled.wrappedBuffer(GsonUtils.toJson(heartBeatRequest).getBytes(CharsetUtil.UTF_8));
            int length = data.readableBytes();
            ByteBuf buffer = Unpooled.buffer(2 + length);
            buffer.writeShort(length);
            buffer.writeBytes(data);

            ctx.channel().writeAndFlush(buffer);
        }
    }
}
