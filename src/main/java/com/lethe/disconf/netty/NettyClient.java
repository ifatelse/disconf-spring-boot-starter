package com.lethe.disconf.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2023/2/13 15:11
 * @Version : 1.0
 * @Copyright : Copyright (c) 2023 All Rights Reserved
 **/
public class NettyClient {

    private static final Log log = LogFactory.getLog(NettyClient.class);

    public static void start() {

        Bootstrap bootstrap = new Bootstrap();

        EventLoopGroup group = new NioEventLoopGroup();

        final NettyClientHandler nettyClientHandler = new NettyClientHandler();

        bootstrap.group(group)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline()
                                .addLast("lengthFieldBasedFrameDecoder", new LengthFieldBasedFrameDecoder(1024,0,2,0,2))
                                .addLast("decoder", new StringDecoder())
                                .addLast("encoder", new StringEncoder())
                                .addLast("client-idle-handler", new IdleStateHandler(0, 0, 5, TimeUnit.SECONDS))
                                .addLast(nettyClientHandler);
                    }
                });

        bootstrap.connect(new InetSocketAddress("grpc.lethe.com", 10010 + 1000)).addListener(future -> {
            if (future.isSuccess()) {
                log.info("Netty Client Connect Success!");
            } else {
                // 进行重连
            }
        });

    }

}
