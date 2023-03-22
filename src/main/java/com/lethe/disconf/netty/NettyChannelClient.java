package com.lethe.disconf.netty;

import com.baidu.disconf.client.common.model.DisConfCommonModel;
import com.baidu.disconf.core.common.remote.*;
import com.baidu.disconf.core.common.remote.netty.Message;
import com.baidu.disconf.core.common.utils.ClassLoaderUtil;
import com.baidu.disconf.core.common.utils.FileUtils;
import com.lethe.disconf.internals.ConfigRepositoryManager;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2023/2/14 17:08
 * @Version : 1.0
 * @Copyright : Copyright (c) 2023 All Rights Reserved
 **/
public class NettyChannelClient {

    private static final Log log = LogFactory.getLog(NettyClientHandler.class);

    private static List<ChannelHandlerContext> clientChannelList = new CopyOnWriteArrayList<>();


    public static void saveChannel(ChannelHandlerContext ctx) {
        clientChannelList.add(ctx);
    }

    public static ChannelHandlerContext getChannel() {
        return clientChannelList.get(0);
    }

    static {
        ResponseRegistry.init();
    }


    public static void loadDisconfData(String fileName, DisConfCommonModel disConfCommonModel){

        ConfigQueryRequest configQueryRequest = new ConfigQueryRequest();
        configQueryRequest.setAppName(disConfCommonModel.getApp());
        configQueryRequest.setVersion(disConfCommonModel.getVersion());
        configQueryRequest.setEnv(disConfCommonModel.getEnv());
        configQueryRequest.setFileName(fileName);

        Message message = new Message();
        message.setMsgType(ConfigQueryRequest.class.getSimpleName());
        message.setData(configQueryRequest);


        DefaultFuture defaultFuture = new DefaultFuture(getChannel().channel(), configQueryRequest);

        sendMsg(message);

        ConfigQueryResponse response = (ConfigQueryResponse) defaultFuture.get();

        String classPath = ClassLoaderUtil.getClassPath();

        try {
            FileUtils.writeStringToFile(new File(classPath + "\\" + response.getFileName()), response.getValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void executeConfigListen(String fileName, DisConfCommonModel disConfCommonModel) {

        ConfigChangeRequest configChangeRequest = new ConfigChangeRequest();
        configChangeRequest.setAppName(disConfCommonModel.getApp());
        configChangeRequest.setVersion(disConfCommonModel.getVersion());
        configChangeRequest.setEnv(disConfCommonModel.getEnv());
        configChangeRequest.setFileName(fileName);

        Message message = new Message();
        message.setMsgType(ConfigChangeRequest.class.getSimpleName());
        message.setData(configChangeRequest);

        sendMsg(message);

    }

    private static void sendMsg(Message message) {
        // ByteBuf data = Unpooled.wrappedBuffer(GsonUtils.toJson(message).getBytes(CharsetUtil.UTF_8));
        // int length = data.readableBytes();
        // ByteBuf buffer = Unpooled.buffer(2 + length);
        // buffer.writeShort(length);
        // buffer.writeBytes(data);
        // 2 + length表示消息长度字段的长度加上实际消息内容的长度。
        // writeShort()方法用于写入消息长度。
        // writeBytes()方法用于写入消息内容。

        getChannel().channel().writeAndFlush(message);
    }


    public static void receivedChangeResponse(Response response) {
        ConfigChangeResponse configChangeResponse = (ConfigChangeResponse) response;
        String fileName = configChangeResponse.getFileName();
        if (StringUtils.isBlank(fileName)) {
            return;
        }

        DisConfCommonModel disConfCommonModel = ConfigRepositoryManager.getInstance().getRemoteConfigRepository().disconfCenterFile.getDisConfCommonModel();

        ConfigQueryRequest configQueryRequest = new ConfigQueryRequest();
        configQueryRequest.setAppName(disConfCommonModel.getApp());
        configQueryRequest.setVersion(disConfCommonModel.getVersion());
        configQueryRequest.setEnv(disConfCommonModel.getEnv());
        configQueryRequest.setFileName(fileName);

        Message message = new Message();
        message.setMsgType(ConfigQueryRequest.class.getSimpleName());
        message.setData(configQueryRequest);


        loadDisconfData(fileName, disConfCommonModel);

        // sendMsg(message);

        System.out.println("===加载环境变量===");

    }


}
