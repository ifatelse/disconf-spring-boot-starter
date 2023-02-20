package com.lethe.disconf.internals;

import com.google.common.collect.Queues;

import java.lang.reflect.Field;
import java.util.Queue;

/**
 * @Description :
 * @Author : liudd12
 * @Date : 2022/12/5 16:43
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
public class ConfigRepositoryManager {

    private final RemoteConfigRepository remoteConfigRepository = null;

    private final Queue<String> confChangeQueue = Queues.newConcurrentLinkedQueue();

    protected static final ConfigRepositoryManager INSTANCE = new ConfigRepositoryManager();


    public static ConfigRepositoryManager getInstance() {
        return INSTANCE;
    }

    public void confChange(String fileName){
        confChangeQueue.add(fileName);
    }

    public Queue<String> confChangeQueue(){
        return confChangeQueue;
    }

    public Boolean hasChangeConf() {
        return confChangeQueue.isEmpty();
    }

    public void loadRemoteConfigRepository(RemoteConfigRepository remoteConfigRepository){
        try {
            Field field = INSTANCE.getClass().getDeclaredField("remoteConfigRepository");
            field.setAccessible(true);
            field.set(INSTANCE, remoteConfigRepository);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteConfigRepository getRemoteConfigRepository() {
        return remoteConfigRepository;
    }
}
