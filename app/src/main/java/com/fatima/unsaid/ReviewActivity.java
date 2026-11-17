package com.fatima.unsaid;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class ReviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        LinearLayout taskList = findViewById(R.id.taskList);
        String thought = getIntent().getStringExtra("thought");
        List<Task> tasks = SimpleParser.parse(thought);

        for (Task task : tasks) {
            View card = getLayoutInflater().inflate(R.layout.item_task, taskList, false);
            TextView tvTitle = card.findViewById(R.id.tvTitle);
            TextView tvInfo = card.findViewById(R.id.tvInfo);
            tvTitle.setText(task.title);
            tvInfo.setText(task.getSummary());
            taskList.addView(card);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}