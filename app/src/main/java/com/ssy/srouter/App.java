package com.ssy.srouter;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;

import com.ssy.srouter.api.launcher.SRouter;

import java.util.HashMap;
import java.util.Map;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SRouter.init(this);


        Map<String, String> map = new HashMap<>();
        map.put("一号小区", "com.ssy.srouter.AActivity");
        map.put("二号小区", "com.ssy.srouter.BActivity");

        String className = map.get("二号小区");
        try {
            Class  BActivity =  Class.forName(className);
            Intent intent = new Intent(this,BActivity);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
