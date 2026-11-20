package com.fatima.unsaid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class ReviewActivity extends AppCompatActivity {

    List<Task> tasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        LinearLayout taskList = findViewById(R.id.taskList);
        String thought = getIntent().getStringExtra("thought");
        tasks = SimpleParser.parse(thought);

        for (Task task : tasks) {
            View card = getLayoutInflater().inflate(R.layout.item_task, taskList, false);
            TextView tvTitle = card.findViewById(R.id.tvTitle);
            TextView tvInfo = card.findViewById(R.id.tvInfo);
            tvTitle.setText(task.title);
            tvInfo.setText(task.getSummary());
            taskList.addView(card);
        }

        findViewById(R.id.btnSave).setOnClickListener(v -> saveAll());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void saveAll() {
        if (tasks.isEmpty()) {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show();
            return;
        }
        AppDatabase.getInstance(this).taskDao().insertAll(tasks);
        Toast.makeText(this, tasks.size() + " items saved", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, TasksActivity.class));
        finish();

    }
}