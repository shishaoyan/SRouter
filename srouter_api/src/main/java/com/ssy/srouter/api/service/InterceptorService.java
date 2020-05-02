package com.ssy.srouter.api.service;

import com.ssy.srouter.api.callback.InterceptorCallback;
import com.ssy.srouter.api.core.Postcard;
import com.ssy.srouter.api.template.IProvider;

public interface InterceptorService extends IProvider {

    void doInterceptions(Postcard postcard, InterceptorCallback callback);
}
