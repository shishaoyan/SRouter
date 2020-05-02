package com.ssy.srouter.api.launcher;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ssy.srouter.api.callback.InterceptorCallback;
import com.ssy.srouter.api.callback.NavigationCallback;
import com.ssy.srouter.api.core.LogisticsCenter;
import com.ssy.srouter.api.core.Postcard;
import com.ssy.srouter.api.exception.HandlerException;
import com.ssy.srouter.api.exception.InitException;
import com.ssy.srouter.api.exception.NoRouteFoundException;
import com.ssy.srouter.api.service.AutowiredService;
import com.ssy.srouter.api.service.InterceptorService;
import com.ssy.srouter.api.thread.DefaultPoolExecutor;
import com.ssy.srouter.api.utils.Consts;
import com.ssy.srouter.api.utils.TextUtils;

import java.util.concurrent.ThreadPoolExecutor;

import androidx.core.app.ActivityCompat;

public class _SRouter {
    private static Context mContext;
    private volatile static boolean hasInit = false;
    private volatile static ThreadPoolExecutor executor = DefaultPoolExecutor.getInstance();
    private static Handler mHandler;
    private volatile static _SRouter instance = null;
    private static InterceptorService interceptorService;

    public static boolean init(Application application) {
        mContext = application;
        LogisticsCenter.init(mContext, executor);
        mHandler = new Handler(Looper.getMainLooper());
        hasInit = true;
        return true;


    }

    public static void afterInit() {
        interceptorService = (InterceptorService) SRouter.getInstance().build("/srouter/service/interceptor").navigation();

    }

    protected static _SRouter getInstance() {
        if (!hasInit) {
            throw new InitException("ARouterCore::Init::Invoke init(context) first!");
        } else {
            if (instance == null) {
                synchronized (_SRouter.class) {
                    if (instance == null) {
                        instance = new _SRouter();
                    }
                }
            }
            return instance;
        }
    }

    public static void inject(Object thiz) {
        AutowiredService autowiredService = ((AutowiredService) SRouter.getInstance().build("/srouter/service/autowired").navigation());
        if (null != autowiredService) {
            autowiredService.autowrite(thiz);
        }

    }

//    public Postcard buidl(Uri url) {
//        if (url == null || TextUtils.isEmpty(url.toString())) {
//            throw new HandlerException(Consts.TAG + "Parameter invalid!");
//        } else {
//            return new Postcard(url.getPath(), extractGroup(url.getPath()));
//        }
//
//    }

    /**
     * Extract the default group from path.
     */
    private String extractGroup(String path) {
        if (TextUtils.isEmpty(path) || !path.startsWith("/")) {
            throw new HandlerException(Consts.TAG + "Extract the default group failed, the path must be start with '/' and contain more than 2 '/'!");
        }

        try {
            String defaultGroup = path.substring(1, path.indexOf("/", 1));
            if (TextUtils.isEmpty(defaultGroup)) {
                throw new HandlerException(Consts.TAG + "Extract the default group failed! There's nothing between 2 '/'!");
            } else {
                return defaultGroup;
            }
        } catch (Exception e) {
            Log.w(Consts.TAG, "Failed to extract default group! " + e.getMessage());
            return null;
        }
    }

//    public <T> T navigation(Class<? extends T> service) {
//        try {
//            Postcard postcard = LogisticsCenter.buildProvider(service.getName());
//            if (postcard==null){
//                return null;
//            }
//            LogisticsCenter.completion(postcard);
//            return postcard.getProvider();
//        } catch (NoRouteFoundException e) {
//            Log.w(Consts.TAG, e.getMessage());
//        }
//    }

    public Postcard build(String path) {
        return build(path, extractGroup(path));

    }

    public Postcard build(String path, String group) {
        return new Postcard(path, group);
    }

    public Object navigation(final Context mContext, final Postcard postcard, final int requestCode, final NavigationCallback callback) {
        LogisticsCenter.completion(postcard);
        if (!postcard.isGreenChannel()) {

            interceptorService.doInterceptions(postcard, new InterceptorCallback() {
                @Override
                public void onContinue(Postcard postcard) {
                    _navigation(mContext, postcard, requestCode, callback);

                }

                @Override
                public void onInterrupt(Throwable throwable) {
                    if (null != callback) {
                        callback.onInterrupt(postcard);
                    }

                    Log.i(Consts.TAG, "Navigation failed, termination by interceptor : " + throwable.getMessage());
                }
            });
        } else {
            return _navigation(mContext, postcard, requestCode, callback);

        }
        return null;
    }

    private Object _navigation(Context context, final Postcard postcard, final int requestCode, final NavigationCallback callback) {
        Log.w(Consts.TAG, "context==null:" + (context == null));
        final Context currentContext = (null == context) ? mContext : context;

        switch (postcard.getType()) {
            case ACTIVITY:
                final Intent intent = new Intent(currentContext, postcard.getDestination());
                intent.putExtras(postcard.getExtras());

                int flags = postcard.getFlags();
                if (-1 != flags) {
                    intent.setFlags(flags);
                } else if (!(currentContext instanceof Activity)) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                String action = postcard.getAction();
                if (!TextUtils.isEmpty(action)) {
                    intent.setAction(action);
                }
                runInMainThread(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(requestCode, currentContext, intent, postcard, callback);
                    }
                });

                break;
            case PROVIDER:
                return postcard.getProvider();
            default:
                break;
        }

        return null;
    }

    private void runInMainThread(Runnable runnable) {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            mHandler.post(runnable);
        } else {
            runnable.run();
        }
    }

    /**
     * Start activity
     *
     * @see ActivityCompat
     */
    private void startActivity(int requestCode, Context currentContext, Intent intent, Postcard postcard, NavigationCallback callback) {
        if (requestCode >= 0) {  // Need start for result
            if (currentContext instanceof Activity) {
                ActivityCompat.startActivityForResult((Activity) currentContext, intent, requestCode, postcard.getOptionsBundle());
            } else {
                Log.w(Consts.TAG, "Must use [navigation(activity, ...)] to support [startActivityForResult]");
            }
        } else {

            ActivityCompat.startActivity(currentContext, intent, postcard.getOptionsBundle());
        }

        if ((-1 != postcard.getEnterAnim() && -1 != postcard.getExitAnim()) && currentContext instanceof Activity) {    // Old version.
            ((Activity) currentContext).overridePendingTransition(postcard.getEnterAnim(), postcard.getExitAnim());
        }

        if (null != callback) { // Navigation over.
            callback.onArrival(postcard);
        }
    }
}
