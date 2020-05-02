package com.ssy.srouter.api.launcher;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.ssy.srouter.api.callback.NavigationCallback;
import com.ssy.srouter.api.core.Postcard;
import com.ssy.srouter.api.exception.InitException;

import static com.ssy.srouter.api.utils.Consts.TAG;

public class SRouter {

    private volatile static boolean hasInit = false;
    private volatile static SRouter instance = null;

    public static void init(Application application) {
        if (!hasInit) {
            Log.i(TAG, "SRouter init start");
            hasInit = _SRouter.init(application);
            if (hasInit) {
                _SRouter.afterInit();
            }
            Log.i(TAG, "SRouter init over");

        }
    }

    //    public Postcard build(Uri url) {
//        return _SRouter.getInstance().buidl(url);
//    }
    public Postcard build(String path) {
        return _SRouter.getInstance().build(path);
    }

    /**
     * Get instance of router. A
     * All feature U use, will be starts here.
     */
    public static SRouter getInstance() {
        if (!hasInit) {
            throw new InitException("ARouter::Init::Invoke init(context) first!");
        } else {
            if (instance == null) {
                synchronized (SRouter.class) {
                    if (instance == null) {
                        instance = new SRouter();
                    }
                }
            }
            return instance;
        }
    }

    public Object navigation(Context mContext, Postcard postcard, int requestCode, NavigationCallback callback) {
        return _SRouter.getInstance().navigation(mContext, postcard, requestCode, callback);
    }

    public void inject(Object thiz){
        _SRouter.inject(thiz);
    }

}
