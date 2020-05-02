package com.ssy.srouter.api.core;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.ssy.srouter.api.exception.NoRouteFoundException;
import com.ssy.srouter.api.template.IInterceptionGroup;
import com.ssy.srouter.api.template.IInterceptor;
import com.ssy.srouter.api.template.IProvider;
import com.ssy.srouter.api.template.IProviderGroup;
import com.ssy.srouter.api.template.IRouteGroup;
import com.ssy.srouter.api.template.IRouteRoot;
import com.ssy.srouter.api.utils.ClassUtils;
import com.ssy.srouter.model.RouteMeta;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import static com.ssy.srouter.api.core.Warehouse.*;
import static com.ssy.srouter.api.utils.Consts.DOT;
import static com.ssy.srouter.api.utils.Consts.ROUTE_ROOT_PAKCAGE;
import static com.ssy.srouter.api.utils.Consts.SDK_NAME;
import static com.ssy.srouter.api.utils.Consts.SEPARATOR;
import static com.ssy.srouter.api.utils.Consts.SROUTER_SP_CACHE_KEY;
import static com.ssy.srouter.api.utils.Consts.SROUTER_SP_KEY_MAP;
import static com.ssy.srouter.api.utils.Consts.SUFFIX_INTERCEPTORS;
import static com.ssy.srouter.api.utils.Consts.SUFFIX_PROVIDERS;
import static com.ssy.srouter.api.utils.Consts.SUFFIX_ROOT;
import static com.ssy.srouter.api.utils.Consts.TAG;

public class LogisticsCenter {
    private static Context mContext;
    public static ThreadPoolExecutor mExecutor;

    public static void init(Context context, ThreadPoolExecutor executor) {
        mContext = context;
        mExecutor = executor;

        long startInit = System.currentTimeMillis();
        //重点 加载我们生成的注册类
        //1、通过 plugin 来加载
        //2、通过各个dex去查找
        Set<String> routerMap;
        try {
            routerMap = ClassUtils.getFileNameByPackageName(mContext, ROUTE_ROOT_PAKCAGE);
            for (String className : routerMap) {
                if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_ROOT)) {
                    ((IRouteRoot) Class.forName(className).getConstructor().newInstance()).loadInto(groupsIndex);
                    Log.i(TAG, "Load root element finished, cost " + (System.currentTimeMillis() - startInit) + " ms.");
                } else if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_INTERCEPTORS)) {
                    ((IInterceptionGroup) Class.forName(className).getConstructor().newInstance()).loadInto(Warehouse.interceptorsIndex);
                }else if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_PROVIDERS)) {
                    // Load providerIndex
                    ((IProviderGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.providersIndex);
                }
            }
            if (groupsIndex.size() == 0) {
                Log.e(TAG, "No mapping files were found, check your configuration please!");
            } else {
                for (Map.Entry<String, Class<? extends IRouteGroup>> entry : groupsIndex.entrySet()) {
                    Log.i(TAG, "groupsIndex entry:" + entry.getKey() + "--" + entry.getValue());

                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


    }

    public synchronized static void completion(Postcard postcard) {
        if (null == postcard) {
            throw new NoRouteFoundException(TAG + "No postcard!");
        }
        RouteMeta routeMeta = routes.get(postcard.getPath());

        if (routeMeta == null) {
            Class<? extends IRouteGroup> groupMeta = groupsIndex.get(postcard.getGroup());
            if (null == groupMeta) {
                throw new NoRouteFoundException(TAG + "There is no route match the path [" + postcard.getPath() + "], in group [" + postcard.getGroup() + "]");
            } else {
                IRouteGroup iRouteGroup = null;
                try {
                    iRouteGroup = groupMeta.getConstructor().newInstance();
                    iRouteGroup.loadInto(routes);
                    Warehouse.groupsIndex.remove(postcard.getGroup());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                completion(postcard);

            }

        } else {
            postcard.setDestination(routeMeta.getDestination());
            postcard.setType(routeMeta.getType());
            postcard.setPriority(routeMeta.getPriority());
            postcard.setExtra(routeMeta.getExtra());

            switch (routeMeta.getType()) {
                case PROVIDER:  // if the route is provider, should find its instance
                    // Its provider, so it must implement IProvider
                    Class<? extends IProvider> providerMeta = (Class<? extends IProvider>) routeMeta.getDestination();
                    IProvider instance = Warehouse.providers.get(providerMeta);
                    if (null == instance) {
                        try {
                            IProvider iProvider = providerMeta.getConstructor().newInstance();
                            iProvider.init(mContext);
                            providers.put(providerMeta, iProvider);
                            instance = iProvider;
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                    }
                    postcard.setProvider(instance);
                    postcard.greenChannel();
                    break;

                default:
                    break;
            }
        }
    }
}
