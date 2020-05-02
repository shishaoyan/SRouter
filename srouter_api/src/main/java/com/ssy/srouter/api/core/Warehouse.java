package com.ssy.srouter.api.core;

import com.ssy.srouter.api.base.UniqueKeyTreeMap;
import com.ssy.srouter.api.template.IInterceptor;
import com.ssy.srouter.api.template.IProvider;
import com.ssy.srouter.api.template.IRouteGroup;
import com.ssy.srouter.model.RouteMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Warehouse {
    public static Map<String, Class<? extends IRouteGroup>> groupsIndex = new HashMap<>();
    static Map<String, RouteMeta> routes = new HashMap<>();

    // Cache provider
    static Map<String, RouteMeta> providersIndex = new HashMap<>();
    static Map<Class, IProvider> providers = new HashMap<>();


    //Cache interceptor
    static Map<Integer, Class<? extends IInterceptor>> interceptorsIndex = new UniqueKeyTreeMap<>("More than one interceptors use same priority [%s]");
    static List<IInterceptor> interceptors = new ArrayList<>();
}
