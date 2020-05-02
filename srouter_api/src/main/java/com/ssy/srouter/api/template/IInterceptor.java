package com.ssy.srouter.api.template;

import com.ssy.srouter.api.callback.InterceptorCallback;
import com.ssy.srouter.api.core.Postcard;

public interface IInterceptor extends IProvider {

    void process(Postcard postcard, InterceptorCallback callback);
}
