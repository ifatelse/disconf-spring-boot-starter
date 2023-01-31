package com.lethe.disconf.spring;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.lethe.disconf.utils.DisconfThreadFactory;
import org.springframework.beans.factory.BeanFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AnnoValueRegistry {

    private static final long CLEAN_INTERVAL_IN_SECONDS = 5;
    private final Map<BeanFactory, Multimap<String, AnnoValue>> registry = Maps.newConcurrentMap();
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final Object LOCK = new Object();

    public void register(BeanFactory beanFactory, String key, AnnoValue annoValue) {
        if (!registry.containsKey(beanFactory)) {
            synchronized (LOCK) {
                if (!registry.containsKey(beanFactory)) {
                    registry.put(beanFactory, LinkedListMultimap.<String, AnnoValue>create());
                }
            }
        }

        registry.get(beanFactory).put(key, annoValue);

        // lazy initialize
        if (initialized.compareAndSet(false, true)) {
            initialize();
        }
    }

    public Collection<AnnoValue> get(BeanFactory beanFactory, String key) {
        Multimap<String, AnnoValue> beanFactorySpringValues = registry.get(beanFactory);
        if (beanFactorySpringValues == null) {
            return null;
        }
        return beanFactorySpringValues.get(key);
    }

    private void initialize() {
        Executors.newSingleThreadScheduledExecutor(DisconfThreadFactory.create("SpringValueRegistry", true)).scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            scanAndClean();
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                        }
                    }
                }, CLEAN_INTERVAL_IN_SECONDS, CLEAN_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
    }

    private void scanAndClean() {
        Iterator<Multimap<String, AnnoValue>> iterator = registry.values().iterator();
        while (!Thread.currentThread().isInterrupted() && iterator.hasNext()) {
            Multimap<String, AnnoValue> annoValues = iterator.next();
            // clear unused spring values
            annoValues.entries().removeIf(annoValue -> !annoValue.getValue().isTargetBeanValid());
        }
    }
}
