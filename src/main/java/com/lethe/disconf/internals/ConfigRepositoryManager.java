package com.lethe.disconf.internals;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;

import java.util.Map;
import java.util.Queue;

/**
 * @Description :
 * @Author : liudd12
 * @Date : 2022/12/5 16:43
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
public class ConfigRepositoryManager {

    private final Map<String, RemoteConfigRepository> listenerMap = Maps.newConcurrentMap();

    private final Queue<String> confChangeQueue = Queues.newConcurrentLinkedQueue();

    protected static final ConfigRepositoryManager INSTANCE = new ConfigRepositoryManager();


    public static ConfigRepositoryManager getInstance() {
        return INSTANCE;
    }

    public void addListener(String fileName, RemoteConfigRepository remoteConfigRepository) {
        listenerMap.put(fileName, remoteConfigRepository);
    }

    public Map<String, RemoteConfigRepository> listenerMap() {
        return listenerMap;
    }

    public RemoteConfigRepository confRepository(String fileName) {
        return listenerMap.get(fileName);
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

}
