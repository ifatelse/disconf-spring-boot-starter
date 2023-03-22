package com.lethe.disconf.client;

import com.baidu.disconf.client.common.model.DisconfCenterFile;
import com.baidu.disconf.client.watch.ClientInvokeDelegate;
import com.baidu.disconf.client.watch.ConfigChangeListener;
import com.baidu.disconf.client.watch.grpc.ResponseRegistry;
import com.baidu.disconf.core.common.constants.Constants;
import com.baidu.disconf.core.common.remote.*;
import com.baidu.disconf.core.common.remote.grpc.ConnectionSetupRequest;
import com.baidu.disconf.core.common.utils.FileUtils;
import com.baidu.disconf.core.common.utils.GrpcUtils;
import com.baidu.disconf.core.grpc.auto.BiRequestStreamGrpc;
import com.baidu.disconf.core.grpc.auto.Message;
import com.baidu.disconf.core.grpc.auto.RequestGrpc;
import com.google.common.util.concurrent.ListenableFuture;
import com.lethe.disconf.internals.ConfigRepositoryManager;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2023/3/16 16:56
 * @Version : 1.0
 * @Copyright : Copyright (c) 2023 All Rights Reserved
 **/
public class GrpcClientInvoke implements ClientInvokeDelegate {

    private static final Logger logger = LoggerFactory.getLogger(GrpcClientInvoke.class);

    protected RequestGrpc.RequestFutureStub requestFutureStub;

    protected StreamObserver<Message> messageStreamObserver;

    private ConfigChangeListener configChangeListener;


    public GrpcClientInvoke() {
        ResponseRegistry.init();
        start();
    }

    private void start() {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress("127.0.0.1", Constants.GRPC_PORT)
                .compressorRegistry(CompressorRegistry.getDefaultInstance())
                .decompressorRegistry(DecompressorRegistry.getDefaultInstance())
                .maxInboundMessageSize(10 * 1024 * 1024)
                .keepAliveTime(6 * 60 * 1000, TimeUnit.MILLISECONDS).usePlaintext();

        ManagedChannel managedChannel = channelBuilder.build();

        RequestGrpc.RequestFutureStub requestFutureStub = RequestGrpc.newFutureStub(managedChannel);

        BiRequestStreamGrpc.BiRequestStreamStub biRequestStreamStub = BiRequestStreamGrpc.newStub(managedChannel);
        StreamObserver<Message> messageStreamObserver = bindRequestStream(biRequestStreamStub);


        this.requestFutureStub = requestFutureStub;
        this.messageStreamObserver = messageStreamObserver;

        // 建立连接，为配置变更做准备
        ConnectionSetupRequest setupRequest = new ConnectionSetupRequest();
        sendRequest(setupRequest);

    }


    @Override
    public void configLoad(String fileName) {
        // ConfigQueryRequest request = new ConfigQueryRequest();
        // request.setAppName(disconfCenterFile.getDisConfCommonModel().getApp());
        // request.setVersion(disconfCenterFile.getDisConfCommonModel().getVersion());
        // request.setEnv(disconfCenterFile.getDisConfCommonModel().getEnv());
        // request.setFileName(fileName);
        // request.setRequestId(UUID.randomUUID().toString());
        //
        // ConfigQueryResponse response = (ConfigQueryResponse) request(request);
        //
        // try {
        //     String classPath = disconfCenterFile.getFileDir();
        //     FileUtils.writeStringToFile(new File(classPath + "\\" + response.getFileName()), response.getValue());
        // } catch (IOException e) {
        //     throw new RuntimeException(e);
        // }
        loadConfig(fileName);
    }

    private void loadConfig(String fileName){
        DisconfCenterFile disconfCenterFile = ConfigRepositoryManager.getInstance().getDisconfCenterFile();
        ConfigQueryRequest request = new ConfigQueryRequest();
        request.setAppName(disconfCenterFile.getDisConfCommonModel().getApp());
        request.setVersion(disconfCenterFile.getDisConfCommonModel().getVersion());
        request.setEnv(disconfCenterFile.getDisConfCommonModel().getEnv());
        request.setFileName(fileName);
        request.setRequestId(UUID.randomUUID().toString());

        ConfigQueryResponse response = (ConfigQueryResponse) request(request);

        try {
            String classPath = disconfCenterFile.getFileDir();
            FileUtils.writeStringToFile(new File(classPath + "\\" + response.getFileName()), response.getValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void configListen(ConfigChangeListener configChangeListener) {
        DisconfCenterFile disconfCenterFile = ConfigRepositoryManager.getInstance().getDisconfCenterFile();
        ConfigChangeRequest configChangeRequest = new ConfigChangeRequest();
        configChangeRequest.setAppName(disconfCenterFile.getDisConfCommonModel().getApp());
        configChangeRequest.setVersion(disconfCenterFile.getDisConfCommonModel().getVersion());
        configChangeRequest.setEnv(disconfCenterFile.getDisConfCommonModel().getEnv());
        configChangeRequest.setFileName(disconfCenterFile.getDisConfCommonModel().getApp());

        ConfigChangeResponse response = (ConfigChangeResponse) request(configChangeRequest);

        logger.info("listener response: " + response);
        this.configChangeListener = configChangeListener;

    }

    private StreamObserver<Message> bindRequestStream(BiRequestStreamGrpc.BiRequestStreamStub biRequestStreamStub) {

        return biRequestStreamStub.biRequestStream(new StreamObserver<Message>() {
            @Override
            public void onNext(Message message) {
                logger.info("接受到服务端的message信息为：{}", message);
                String responseType = message.getType();
                Class<?> responseClass = ResponseRegistry.getClassByType(responseType);
                ConfigChangeResponse response = (ConfigChangeResponse) GrpcUtils.parse(message, responseClass);
                String fileName = response.getFileName();
                // 配置发生变更，获取新的配置
                loadConfig(fileName);

                configChangeListener.configChange(fileName);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });

    }


    public Response request(Request request) {
        Message message = GrpcUtils.convert(request);
        ListenableFuture<Message> requestFuture = requestFutureStub.request(message);
        Message grpcResponse;
        try {
            grpcResponse = requestFuture.get(3000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String responseType = grpcResponse.getType();
        Class<?> responseClass = ResponseRegistry.getClassByType(responseType);
        return (Response) GrpcUtils.parse(grpcResponse, responseClass);
    }


    private void sendRequest(Request request) {
        Message message = GrpcUtils.convert(request);
        messageStreamObserver.onNext(message);
    }
}
