package com.ssy.srouter.api.callback;

import com.ssy.srouter.api.core.Postcard;

public interface InterceptorCallback {

    void onContinue(Postcard postcard);

    void onInterrupt(Throwable throwable);
}
