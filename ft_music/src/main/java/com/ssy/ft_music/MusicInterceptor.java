package com.ssy.ft_music;

import android.content.Context;

import com.ssy.srouter.annotation.Interceptor;
import com.ssy.srouter.api.callback.InterceptorCallback;
import com.ssy.srouter.api.core.Postcard;
import com.ssy.srouter.api.template.IInterceptor;

@Interceptor(priority = 7)
public class MusicInterceptor implements IInterceptor {
    Context mContext;

    @Override
    public void process(Postcard postcard, InterceptorCallback callback) {
        if (postcard.getDestination().getCanonicalName().contains("xxx")) {
            callback.onInterrupt(new Throwable("MusicInterceptor error login"));
        } else {
            callback.onContinue(postcard);
        }
    }

    @Override
    public void init(Context context) {
        mContext = context;
    }
}
