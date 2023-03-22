package com.lethe.disconf.client;

import com.baidu.disconf.client.common.model.DisconfCenterFile;
import com.baidu.disconf.client.watch.ClientInvokeDelegate;
import com.baidu.disconf.client.watch.ConfigChangeListener;
import com.baidu.disconf.core.common.constants.Constants;
import com.baidu.disconf.core.common.remote.ConfigChangeRequest;
import com.baidu.disconf.core.common.remote.ConfigQueryRequest;
import com.baidu.disconf.core.common.remote.ConfigQueryResponse;
import com.baidu.disconf.core.common.remote.netty.Message;
import com.baidu.disconf.core.common.remote.netty.MessageCodec;
import com.baidu.disconf.core.common.utils.ClassLoaderUtil;
import com.baidu.disconf.core.common.utils.FileUtils;
import com.lethe.disconf.internals.ConfigRepositoryManager;
import com.lethe.disconf.netty.DefaultFuture;
import com.lethe.disconf.netty.NettyClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2023/3/16 16:45
 * @Version : 1.0
 * @Copyright : Copyright (c) 2023 All Rights Reserved
 **/
public class NettyClientInvoke implements ClientInvokeDelegate {

    private static final Logger logger = LoggerFactory.getLogger(NettyClientInvoke.class);

    private Channel channel;

    private ConfigChangeListener configChangeListener;

    public NettyClientInvoke() {
        start("127.0.0.1", Constants.NETTY_PORT);
    }

    private void start(String serverIp, int serverPort) {

        Bootstrap bootstrap = new Bootstrap();

        EventLoopGroup group = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());

        final NettyClientHandler nettyClientHandler = new NettyClientHandler(this);

        bootstrap.group(group)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getConnectTimeout())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline()
                                .addLast("codec", new MessageCodec())
                                .addLast("client-idle-handler", new IdleStateHandler(0, 30, 0, TimeUnit.SECONDS))
                                .addLast(nettyClientHandler);
                    }
                });

        long start = System.currentTimeMillis();
        ChannelFuture future = bootstrap.connect(new InetSocketAddress(serverIp, serverPort));

        boolean ret = future.awaitUninterruptibly(getConnectTimeout(), TimeUnit.MILLISECONDS);
        if (ret && future.isSuccess()) {
            channel = future.channel();
            logger.info("connect to listen server success");
        } else if (future.cause() != null) {
            throw new RuntimeException("failed to connect to server " + serverIp + ":" + serverPort + ", error message is:" + future.cause().getMessage(), future.cause());
        } else {
            throw new RuntimeException("failed to connect to server " + serverIp + ":" + serverPort + ", client-side timeout "
                    + getConnectTimeout() + "ms (elapsed: " + (System.currentTimeMillis() - start) + "ms)");
        }
    }


    @Override
    public void configLoad(String fileName) {
        loadConfig(fileName);
    }


    private void loadConfig(String fileName) {

        DisconfCenterFile disconfCenterFile = ConfigRepositoryManager.getInstance().getDisconfCenterFile();

        ConfigQueryRequest configQueryRequest = new ConfigQueryRequest();
        configQueryRequest.setAppName(disconfCenterFile.getDisConfCommonModel().getApp());
        configQueryRequest.setVersion(disconfCenterFile.getDisConfCommonModel().getVersion());
        configQueryRequest.setEnv(disconfCenterFile.getDisConfCommonModel().getEnv());
        configQueryRequest.setFileName(fileName);
        configQueryRequest.setRequestId(UUID.randomUUID().toString());
        configQueryRequest.setEvent("loadQuery");

        Message message = new Message();
        message.setMsgType(ConfigQueryRequest.class.getSimpleName());
        message.setData(configQueryRequest);

        DefaultFuture defaultFuture = new DefaultFuture(channel, configQueryRequest);

        try {
            ChannelFuture channelFuture = channel.writeAndFlush(message);
            Throwable cause = channelFuture.cause();
            if (cause != null) {
                throw cause;
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to send message " + message + " to " + getRemoteAddress() + ", cause: " + e.getMessage(), e);
        }

        ConfigQueryResponse response = (ConfigQueryResponse) defaultFuture.get();

        String classPath = ClassLoaderUtil.getClassPath();

        try {
            FileUtils.writeStringToFile(new File(classPath + "\\" + response.getFileName()), response.getValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) channel.remoteAddress();
    }

    @Override
    public void configListen(ConfigChangeListener configChangeListener) {
        logger.info("监听配置变更。。。");

        this.configChangeListener = configChangeListener;

        DisconfCenterFile disconfCenterFile = ConfigRepositoryManager.getInstance().getDisconfCenterFile();

        ConfigChangeRequest configChangeRequest = new ConfigChangeRequest();
        configChangeRequest.setAppName(disconfCenterFile.getDisConfCommonModel().getApp());
        configChangeRequest.setVersion(disconfCenterFile.getDisConfCommonModel().getVersion());
        configChangeRequest.setEnv(disconfCenterFile.getDisConfCommonModel().getEnv());
        configChangeRequest.setFileName(disconfCenterFile.getDisConfCommonModel().getApp());

        Message message = new Message();
        message.setMsgType(ConfigChangeRequest.class.getSimpleName());
        message.setData(configChangeRequest);

        channel.writeAndFlush(message);

    }


    protected int getConnectTimeout() {
        return 10000;
    }

    public void configChange(String fileName) {

        // loadConfig(fileName);

        DisconfCenterFile disconfCenterFile = ConfigRepositoryManager.getInstance().getDisconfCenterFile();

        ConfigQueryRequest configQueryRequest = new ConfigQueryRequest();
        configQueryRequest.setAppName(disconfCenterFile.getDisConfCommonModel().getApp());
        configQueryRequest.setVersion(disconfCenterFile.getDisConfCommonModel().getVersion());
        configQueryRequest.setEnv(disconfCenterFile.getDisConfCommonModel().getEnv());
        configQueryRequest.setFileName(fileName);
        configQueryRequest.setRequestId(UUID.randomUUID().toString());

        configQueryRequest.setEvent("changeQuery");

        Message message = new Message();
        message.setMsgType(ConfigQueryRequest.class.getSimpleName());
        message.setData(configQueryRequest);

        channel.writeAndFlush(message);

    }

    public void configChange(ConfigQueryResponse response) {

        String classPath = ClassLoaderUtil.getClassPath();
        String fileName = response.getFileName();
        try {
            FileUtils.writeStringToFile(new File(classPath + "\\" + fileName), response.getValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        configChangeListener.configChange(fileName);

    }


}
