package com.daddyno1.clicktrace;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.daddyno1.test.LoginActivity;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "==ClickTrace==";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleClick();
            }
        });
    }

    private void handleClick(){
        Log.e(TAG, ":app-MainActivity#handleClick");
        startActivity(new Intent(this, LoginActivity.class));
    }
}
