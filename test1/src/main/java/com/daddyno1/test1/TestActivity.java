package com.daddyno1.test1;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class TestActivity extends AppCompatActivity {

    public static final String TAG = "==ClickTrace==";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        Button btn = findViewById(R.id.test1_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, ":test1-TestActivity#onClick");
            }
        });
    }
}
