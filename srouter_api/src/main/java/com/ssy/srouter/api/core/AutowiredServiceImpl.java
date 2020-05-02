package com.ssy.srouter.api.core;

import android.content.Context;
import android.util.LruCache;

import com.ssy.srouter.annotation.Route;
import com.ssy.srouter.api.service.AutowiredService;
import com.ssy.srouter.api.template.ISyringe;

import java.util.ArrayList;
import java.util.List;

import static com.ssy.srouter.api.utils.Consts.SUFFIX_AUTOWIRED;


@Route(path = "/srouter/service/autowired")
public class AutowiredServiceImpl implements AutowiredService {

    private LruCache<String, ISyringe> classCache;
    private List<String> blackList;

    @Override
    public void autowrite(Object instance) {
        String className = instance.getClass().getName();
        try {
            if (!blackList.contains(className)) {
                ISyringe autowireHelper = classCache.get(className);
                if (autowireHelper == null) {
                    autowireHelper = (ISyringe) Class.forName(instance.getClass().getName() + SUFFIX_AUTOWIRED).getConstructor().newInstance();
                }
                autowireHelper.inject(instance);
                classCache.put(className, autowireHelper);
            }
        } catch (Exception e) {
            blackList.add(className);
        }
    }

    @Override
    public void init(Context context) {
        classCache = new LruCache<>(66);
        blackList = new ArrayList<>();
    }
}
