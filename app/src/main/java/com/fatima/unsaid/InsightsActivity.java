package com.fatima.unsaid;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.Calendar;
import java.util.List;

public class InsightsActivity extends AppCompatActivity {

    TextView tvCompleted, tvPending, tvOverdue, tvDueToday, tvRate, tvWeek;
    ProgressBar pbRate;
    LinearLayout categoryList;
    String[] categories = {"Academic", "Personal", "Communication", "Work", "Health"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insights);

        tvCompleted = findViewById(R.id.tvCompleted);
        tvPending = findViewById(R.id.tvPending);
        tvOverdue = findViewById(R.id.tvOverdue);
        tvDueToday = findViewById(R.id.tvDueToday);
        tvRate = findViewById(R.id.tvRate);
        tvWeek = findViewById(R.id.tvWeek);
        pbRate = findViewById(R.id.pbRate);
        categoryList = findViewById(R.id.categoryList);

        NavBar.setup(this, 3);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void load() {
        AppDatabase db = AppDatabase.getInstance(this);
        List<Task> all = db.taskDao().getAll();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfToday = cal.getTimeInMillis();
        long day = 24L * 60 * 60 * 1000;

        int completed = 0, pending = 0, overdue = 0, dueToday = 0;
        int[] counts = new int[categories.length];

        for (Task t : all) {
            if (t.done) {
                completed++;
            } else {
                pending++;
                if (t.dueMillis > 0 && t.dueMillis < startOfToday) overdue++;
                else if (t.dueMillis >= startOfToday && t.dueMillis < startOfToday + day) dueToday++;
            }
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(t.category)) counts[i]++;
            }
        }

        tvCompleted.setText(String.valueOf(completed));
        tvPending.setText(String.valueOf(pending));
        tvOverdue.setText(String.valueOf(overdue));
        tvDueToday.setText(String.valueOf(dueToday));

        int total = completed + pending;
        int percent = total == 0 ? 0 : completed * 100 / total;
        tvRate.setText(percent + "%");
        pbRate.setProgress(percent);

        buildCategoryBars(counts);

        // This week: thoughts captured in the last 7 days
        long weekAgo = System.currentTimeMillis() - 7 * day;
        int thoughts = 0, items = 0;
        for (Thought th : db.thoughtDao().getAll()) {
            if (th.createdMillis >= weekAgo) {
                thoughts++;
                items += th.itemCount;
            }
        }
        if (thoughts == 0) {
            tvWeek.setText("Nothing captured in the last 7 days");
        } else {
            tvWeek.setText(thoughts + (thoughts == 1 ? " thought" : " thoughts")
                    + " captured · " + items + (items == 1 ? " item" : " items") + " found");
        }
    }

    private void buildCategoryBars(int[] counts) {
        categoryList.removeAllViews();
        int max = 0;
        for (int c : counts) max = Math.max(max, c);

        int textColor = ContextCompat.getColor(this, R.color.unsaid_text);
        int grey = ContextCompat.getColor(this, R.color.unsaid_text_secondary);
        int accent = ContextCompat.getColor(this, R.color.unsaid_accent);

        for (int i = 0; i < categories.length; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 0, 0, dp(10));

            TextView label = new TextView(this);
            label.setText(categories[i]);
            label.setTextColor(textColor);
            label.setTextSize(14);
            row.addView(label, new LinearLayout.LayoutParams(dp(110),
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            LinearLayout track = new LinearLayout(this);
            track.setOrientation(LinearLayout.HORIZONTAL);
            track.setWeightSum(max == 0 ? 1 : max);

            View fill = new View(this);
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(dp(5));
            shape.setColor(accent);
            fill.setBackground(shape);
            track.addView(fill, new LinearLayout.LayoutParams(0, dp(10), counts[i]));

            row.addView(track, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView count = new TextView(this);
            count.setText(String.valueOf(counts[i]));
            count.setTextColor(grey);
            count.setTextSize(14);
            count.setGravity(Gravity.END);
            row.addView(count, new LinearLayout.LayoutParams(dp(32),
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            categoryList.addView(row);
        }
    }
}