package com.fatima.unsaid;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ReviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        TextView tvThought = findViewById(R.id.tvThought);
        String thought = getIntent().getStringExtra("thought");
        tvThought.setText(thought);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}