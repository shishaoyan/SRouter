package com.ssy.srouter.api.core;

import android.content.Context;
import android.util.Log;

import com.ssy.srouter.annotation.Route;
import com.ssy.srouter.api.callback.InterceptorCallback;
import com.ssy.srouter.api.exception.HandlerException;
import com.ssy.srouter.api.service.InterceptorService;
import com.ssy.srouter.api.template.IInterceptor;
import com.ssy.srouter.api.thread.CancelableCountDownLatch;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.ssy.srouter.api.utils.Consts.TAG;

@Route(path = "/srouter/service/interceptor")
public class InterceptorServiceImpl implements InterceptorService {

    private static boolean interceptorHasInit;
    private static final Object interceptorInitLock = new Object();

    @Override
    public void doInterceptions(final Postcard postcard, final InterceptorCallback callback) {

        if (null != Warehouse.interceptors && Warehouse.interceptors.size() > 0) {
            checkInterceptorsInitStatus();
            if (!interceptorHasInit) {
                callback.onInterrupt(new HandlerException("Interceptors initialization takes too much time."));
                return;
            }
            LogisticsCenter.mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    CancelableCountDownLatch interceptorCounter = new CancelableCountDownLatch(Warehouse.interceptors.size());
                    _excute(0, interceptorCounter, postcard);
                    try {
                        interceptorCounter.await(postcard.getTimeout(), TimeUnit.SECONDS);
                        if (interceptorCounter.getCount()>0){
                            callback.onInterrupt(new HandlerException("The interceptor processing timed out."));
                        }else if (null!=postcard.getTag()){
                            callback.onInterrupt(new HandlerException(postcard.getTag().toString()));

                        }else {
                            callback.onContinue(postcard);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        callback.onInterrupt(e);
                    }

                }
            });
        }else {
            callback.onContinue(postcard);
        }

    }

    private static void _excute(final int index, final CancelableCountDownLatch interceptorCounter, final Postcard postcard) {
        if (index < Warehouse.interceptors.size()) {
            IInterceptor iInterceptor = Warehouse.interceptors.get(index);
            iInterceptor.process(postcard, new InterceptorCallback() {
                @Override
                public void onContinue(Postcard postcard) {
                    interceptorCounter.countDown();
                    _excute(index + 1, interceptorCounter, postcard);
                }

                @Override
                public void onInterrupt(Throwable throwable) {
                    postcard.setTag(null == throwable ? new HandlerException("No message") : throwable.getMessage());
                    interceptorCounter.cancel();

                }
            });
        }
    }

    @Override
    public void init(final Context context) {

        LogisticsCenter.mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!Warehouse.interceptorsIndex.isEmpty()) {
                    for (Map.Entry<Integer, Class<? extends IInterceptor>> entry : Warehouse.interceptorsIndex.entrySet()) {
                        Class<? extends IInterceptor> interceptorClass = entry.getValue();
                        try {
                            IInterceptor iInterceptor = interceptorClass.getConstructor().newInstance();
                            iInterceptor.init(context);
                            Warehouse.interceptors.add(iInterceptor);
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
                    interceptorHasInit = true;
                    Log.i(TAG, "SRouter interceptors init over.");

                    synchronized (interceptorInitLock) {
                        interceptorInitLock.notifyAll();
                    }
                }
            }
        });

    }

    private static void checkInterceptorsInitStatus() {
        synchronized (interceptorInitLock) {
            while (!interceptorHasInit) {
                try {
                    interceptorInitLock.wait(10 * 1000);
                } catch (InterruptedException e) {
                    throw new HandlerException(TAG + "Interceptor init cost too much time error! reason = [" + e.getMessage() + "]");
                }
            }
        }
    }
}
