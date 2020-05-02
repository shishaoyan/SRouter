package com.ssy.srouter;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.ssy.srouter.annotation.Route;


@Route(path = "/main/SecondActivity")
public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
    }
}
