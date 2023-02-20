package com.lethe.disconf.netty;

import com.baidu.disconf.client.config.DisClientConfig;
import com.baidu.disconf.core.common.remote.ConfigQueryRequest;
import com.baidu.disconf.core.common.utils.GsonUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2023/2/14 17:08
 * @Version : 1.0
 * @Copyright : Copyright (c) 2023 All Rights Reserved
 **/
public class NettyChannelService {

    private static List<ChannelHandlerContext> clientChannelList = new CopyOnWriteArrayList<>();


    public static void saveChannel(ChannelHandlerContext ctx) {
        clientChannelList.add(ctx);
    }

    public static ChannelHandlerContext getChannel() {
        return clientChannelList.get(0);
    }


    public static void loadDisconfData(String fileName){

        ConfigQueryRequest configQueryRequest = new ConfigQueryRequest();
        configQueryRequest.setAppName(DisClientConfig.getInstance().APP);
        configQueryRequest.setVersion(DisClientConfig.getInstance().VERSION);
        configQueryRequest.setEnv(DisClientConfig.getInstance().ENV);
        configQueryRequest.setFileName(fileName);
        configQueryRequest.setMsgType(ConfigQueryRequest.class.getSimpleName());

        ByteBuf data = Unpooled.wrappedBuffer(GsonUtils.toJson(configQueryRequest).getBytes(CharsetUtil.UTF_8));
        int length = data.readableBytes();
        ByteBuf buffer = Unpooled.buffer(2 + length);
        buffer.writeShort(length);
        buffer.writeBytes(data);
        // 2 + length表示消息长度字段的长度加上实际消息内容的长度。
        // writeShort()方法用于写入消息长度。
        // writeBytes()方法用于写入消息内容。

        getChannel().channel().writeAndFlush(buffer);
    }


}
