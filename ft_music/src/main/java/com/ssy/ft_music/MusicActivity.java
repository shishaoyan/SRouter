package com.ssy.ft_music;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.ssy.srouter.annotation.Route;
import com.ssy.srouter.api.launcher.SRouter;

@Route(path = "/music/MusicActivity")
public class MusicActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        findViewById(R.id.btn_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SRouter.getInstance().build("/login/LoginActivity").navigation(MusicActivity.this);

            }
        });
    }
}
