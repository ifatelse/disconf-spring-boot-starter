package com.lethe.disconf.netty;

import com.baidu.disconf.core.common.remote.MessageCodec;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
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

        EventLoopGroup group = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());

        final NettyClientHandler nettyClientHandler = new NettyClientHandler();


        bootstrap.group(group)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline()
                                // .addLast("lengthFieldBasedFrameDecoder", new LengthFieldBasedFrameDecoder(1024, 0, 2, 0, 2))
                                // .addLast("decoder", new StringDecoder())
                                // .addLast("encoder", new StringEncoder())
                                .addLast("codec", new MessageCodec())
                                .addLast("client-idle-handler", new IdleStateHandler(0, 30, 0, TimeUnit.SECONDS))
                                .addLast(nettyClientHandler);
                    }
                });

        ChannelFuture future = bootstrap.connect(new InetSocketAddress("127.0.0.1", 10010 + 1000));

        boolean ret = future.awaitUninterruptibly(10000, TimeUnit.MILLISECONDS);
        if (ret && future.isSuccess()) {
            log.info("Netty Client Connect Success!");
            Channel newChannel = future.channel();
        }
    }

}
