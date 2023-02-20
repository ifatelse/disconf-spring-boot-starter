package com.lethe.disconf.netty;

import com.baidu.disconf.client.config.DisClientConfig;
import com.baidu.disconf.core.common.remote.ConfigQueryRequest;
import com.baidu.disconf.core.common.remote.ConfigQueryResponse;
import com.baidu.disconf.core.common.utils.ClassLoaderUtil;
import com.baidu.disconf.core.common.utils.GsonUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Date;

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

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("接受到服务端消息：" + msg);

        ConfigQueryResponse configQueryResponse = GsonUtils.fromJson((String) msg, ConfigQueryResponse.class);

        String classPath = ClassLoaderUtil.getClassPath();

        log.info("--------" + classPath);

        FileUtils.writeStringToFile(new File(classPath + "\\" + configQueryResponse.getFileName()), configQueryResponse.getValue());
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

    public static String toAddressString(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }
}
