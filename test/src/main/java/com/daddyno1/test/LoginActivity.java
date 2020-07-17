package com.daddyno1.test;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "==ClickTrace==";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button button = findViewById(R.id.testBtn);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Log.e(TAG, ":test-LoginActivity#onClick");
    }
}
