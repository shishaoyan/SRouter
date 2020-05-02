package com.ssy.srouter;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.ssy.srouter.annotation.Route;
import com.ssy.srouter.api.launcher.SRouter;

@Route(path = "/main/MainActivity")
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SRouter.getInstance().build("/music/MusicActivity").navigation(MainActivity.this);
            }
        });
    }
}
