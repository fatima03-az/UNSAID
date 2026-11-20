package com.fatima.unsaid;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.List;

public class TasksActivity extends AppCompatActivity {

    LinearLayout chipRow, taskList;
    TextView tvEmpty;
    String selected = "All";
    String[] categories = {"All", "Academic", "Personal", "Communication", "Work", "Health"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        chipRow = findViewById(R.id.chipRow);
        taskList = findViewById(R.id.taskList);
        tvEmpty = findViewById(R.id.tvEmpty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        buildChips();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void buildChips() {
        chipRow.removeAllViews();
        int accent = ContextCompat.getColor(this, R.color.unsaid_accent);
        int surface = ContextCompat.getColor(this, R.color.unsaid_surface);
        int text = ContextCompat.getColor(this, R.color.unsaid_text);
        int background = ContextCompat.getColor(this, R.color.unsaid_background);

        for (String c : categories) {
            boolean isSelected = c.equals(selected);
            TextView chip = new TextView(this);
            chip.setText(c);
            chip.setTextSize(14);
            chip.setPadding(dp(18), dp(10), dp(18), dp(10));
            chip.setTextColor(isSelected ? background : text);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(20));
            bg.setColor(isSelected ? accent : surface);
            chip.setBackground(bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                selected = c;
                buildChips();
                loadTasks();
            });
            chipRow.addView(chip);
        }
    }

    private void loadTasks() {
        TaskDao dao = AppDatabase.getInstance(this).taskDao();
        List<Task> all = dao.getAll();
        taskList.removeAllViews();
        int count = 0;

        for (Task t : all) {
            if (!selected.equals("All") && !t.category.equals(selected)) continue;
            count++;

            View card = getLayoutInflater().inflate(R.layout.item_my_task, taskList, false);
            TextView tvTitle = card.findViewById(R.id.tvTitle);
            TextView tvInfo = card.findViewById(R.id.tvInfo);
            TextView tvDot = card.findViewById(R.id.tvDot);
            TextView tvDelete = card.findViewById(R.id.tvDelete);
            CheckBox cbDone = card.findViewById(R.id.cbDone);

            tvTitle.setText(t.title);
            tvInfo.setText(t.getSummary());
            tvDot.setTextColor(ContextCompat.getColor(this, priorityColor(t.priority)));

            cbDone.setChecked(t.done);
            applyDoneStyle(tvTitle, t.done);
            cbDone.setOnCheckedChangeListener((button, checked) -> {
                t.done = checked;
                dao.update(t);
                applyDoneStyle(tvTitle, checked);
            });

            tvDelete.setOnClickListener(v -> {
                dao.delete(t);
                loadTasks();
            });

            taskList.addView(card);
        }
        tvEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    private void applyDoneStyle(TextView tv, boolean done) {
        if (done) {
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tv.setAlpha(0.5f);
        } else {
            tv.setPaintFlags(tv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            tv.setAlpha(1f);
        }
    }

    private int priorityColor(String priority) {
        if (priority.equals("High")) return R.color.priority_high;
        if (priority.equals("Medium")) return R.color.priority_medium;
        return R.color.priority_low;
    }
}