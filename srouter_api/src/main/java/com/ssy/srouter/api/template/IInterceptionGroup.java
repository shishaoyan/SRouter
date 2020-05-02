package com.ssy.srouter.api.template;

import com.ssy.srouter.model.RouteMeta;

import java.util.Map;

public interface IInterceptionGroup {

    void loadInto(Map<Integer, Class<? extends IInterceptor>> interceptor);
}
