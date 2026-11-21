package com.fatima.unsaid;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Gentle fade-in for the airplane, the name and the tagline
        int[] ids = {R.id.ivPlane, R.id.tvName, R.id.tvTagline};
        for (int i = 0; i < ids.length; i++) {
            View v = findViewById(ids[i]);
            v.setAlpha(0f);
            v.animate().alpha(1f).setDuration(900).setStartDelay(i * 350L).start();
        }

        // After about two and a half seconds, open Home
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }, 2600);
    }
}