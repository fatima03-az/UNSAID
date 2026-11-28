package com.fatima.unsaid;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.Calendar;
import java.util.List;

public class InsightsActivity extends AppCompatActivity {

    TextView tvRate, tvChange, tvCompleted, tvPending, tvOverdue, tvDueToday;
    TextView tvChartTitle, tvTrophy, tvTrophySub, tvCaptured;
    RingView ring;
    LinearLayout chipRow, barChart, categoryList;

    String[] periods = {"This Week", "This Month", "This Year"};
    String[] categories = {"Academic", "Personal", "Communication", "Work", "Health"};
    String[] weekLabels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    String[] monthLabels = {"W1", "W2", "W3", "W4", "W5"};
    String[] yearLabels = {"J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"};
    int period = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insights);

        tvRate = findViewById(R.id.tvRate);
        tvChange = findViewById(R.id.tvChange);
        tvCompleted = findViewById(R.id.tvCompleted);
        tvPending = findViewById(R.id.tvPending);
        tvOverdue = findViewById(R.id.tvOverdue);
        tvDueToday = findViewById(R.id.tvDueToday);
        tvChartTitle = findViewById(R.id.tvChartTitle);
        tvTrophy = findViewById(R.id.tvTrophy);
        tvTrophySub = findViewById(R.id.tvTrophySub);
        tvCaptured = findViewById(R.id.tvCaptured);
        ring = findViewById(R.id.ring);
        chipRow = findViewById(R.id.chipRow);
        barChart = findViewById(R.id.barChart);
        categoryList = findViewById(R.id.categoryList);

        NavBar.setup(this, 3);
        buildChips();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
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

        for (int i = 0; i < periods.length; i++) {
            final int index = i;
            boolean isSelected = (i == period);
            TextView chip = new TextView(this);
            chip.setText(periods[i]);
            chip.setTextSize(14);
            chip.setPadding(dp(18), dp(10), dp(18), dp(10));
            chip.setTextColor(isSelected ? background : text);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(20));
            bg.setColor(isSelected ? accent : surface);
            chip.setBackground(bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                period = index;
                buildChips();
                load();
            });
            chipRow.addView(chip);
        }
    }

    // Which bar a moment belongs to (day of the week, week of the month, or month)
    private int bucketIndex(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        if (period == 0) return (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;   // Monday = 0
        if (period == 1) return Math.min(4, (c.get(Calendar.DAY_OF_MONTH) - 1) / 7);
        return c.get(Calendar.MONTH);
    }

    private void load() {
        AppDatabase db = AppDatabase.getInstance(this);
        List<Task> all = db.taskDao().getAll();
        long day = 24L * 60 * 60 * 1000;

        // Start of today
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long startOfToday = today.getTimeInMillis();

        // The chosen period, and the one before it
        Calendar start = (Calendar) today.clone();
        Calendar end = (Calendar) today.clone();
        Calendar prev = (Calendar) today.clone();
        String word;
        if (period == 0) {
            int back = (start.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            start.add(Calendar.DAY_OF_YEAR, -back);
            end = (Calendar) start.clone();
            end.add(Calendar.DAY_OF_YEAR, 7);
            prev = (Calendar) start.clone();
            prev.add(Calendar.DAY_OF_YEAR, -7);
            word = "week";
        } else if (period == 1) {
            start.set(Calendar.DAY_OF_MONTH, 1);
            end = (Calendar) start.clone();
            end.add(Calendar.MONTH, 1);
            prev = (Calendar) start.clone();
            prev.add(Calendar.MONTH, -1);
            word = "month";
        } else {
            start.set(Calendar.DAY_OF_YEAR, 1);
            end = (Calendar) start.clone();
            end.add(Calendar.YEAR, 1);
            prev = (Calendar) start.clone();
            prev.add(Calendar.YEAR, -1);
            word = "year";
        }
        long s = start.getTimeInMillis();
        long e = end.getTimeInMillis();
        long p = prev.getTimeInMillis();

        int bucketCount = period == 0 ? 7 : (period == 1 ? 5 : 12);
        int[] buckets = new int[bucketCount];
        int[] categoryCounts = new int[categories.length];
        int completed = 0, completedPrev = 0, pending = 0, overdue = 0, dueToday = 0;

        for (Task t : all) {
            if (t.done) {
                if (t.completedMillis >= s && t.completedMillis < e) {
                    completed++;
                    buckets[bucketIndex(t.completedMillis)]++;
                } else if (t.completedMillis >= p && t.completedMillis < s) {
                    completedPrev++;
                }
            } else {
                pending++;
                if (t.dueMillis > 0 && t.dueMillis < startOfToday) overdue++;
                else if (t.dueMillis >= startOfToday && t.dueMillis < startOfToday + day) dueToday++;
            }
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(t.category)) categoryCounts[i]++;
            }
        }

        // Number cards
        tvCompleted.setText(String.valueOf(completed));
        tvPending.setText(String.valueOf(pending));
        tvOverdue.setText(String.valueOf(overdue));
        tvDueToday.setText(String.valueOf(dueToday));

        // Productivity: completed in this period compared with what is still pending
        int total = completed + pending;
        int percent = total == 0 ? 0 : completed * 100 / total;
        tvRate.setText(percent + "%");
        ring.setPercent(percent);

        if (completed == 0 && completedPrev == 0) {
            tvChange.setText("No completed tasks yet");
        } else if (completedPrev == 0) {
            tvChange.setText("↑ first completions");
        } else {
            int diff = (completed - completedPrev) * 100 / completedPrev;
            tvChange.setText((diff >= 0 ? "↑ +" : "↓ ") + diff + "% vs last " + word);
        }

        // Bar chart
        String[] labels = period == 0 ? weekLabels : (period == 1 ? monthLabels : yearLabels);
        tvChartTitle.setText(period == 0 ? "Completed per day"
                : (period == 1 ? "Completed per week" : "Completed per month"));
        buildBarChart(buckets, labels);

        // Category bars
        buildCategoryBars(categoryCounts);

        // Trophy card
        if (completed == 0) {
            tvTrophy.setText("No tasks completed this " + word + " yet");
            tvTrophySub.setText("Tick one off in My Tasks to see it here.");
        } else {
            tvTrophy.setText("You completed " + completed
                    + (completed == 1 ? " task" : " tasks") + " this " + word + "!");
            tvTrophySub.setText("Keep going!");
        }

        // Thoughts captured in this period
        int thoughts = 0, items = 0;
        for (Thought th : db.thoughtDao().getAll()) {
            if (th.createdMillis >= s && th.createdMillis < e) {
                thoughts++;
                items += th.itemCount;
            }
        }
        if (thoughts == 0) {
            tvCaptured.setText("No thoughts captured this " + word);
        } else {
            tvCaptured.setText(thoughts + (thoughts == 1 ? " thought" : " thoughts")
                    + " captured · " + items + (items == 1 ? " item" : " items") + " found");
        }
    }

    private void buildBarChart(int[] values, String[] labels) {
        barChart.removeAllViews();
        int max = 0;
        for (int v : values) max = Math.max(max, v);
        float weightSum = max == 0 ? 1f : max;

        int accent = ContextCompat.getColor(this, R.color.unsaid_accent);
        int grey = ContextCompat.getColor(this, R.color.unsaid_text_secondary);

        for (int i = 0; i < values.length; i++) {
            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.CENTER_HORIZONTAL);

            LinearLayout plot = new LinearLayout(this);
            plot.setOrientation(LinearLayout.VERTICAL);
            plot.setWeightSum(weightSum);

            View spacer = new View(this);
            plot.addView(spacer, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, weightSum - values[i]));

            View bar = new View(this);
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(dp(4));
            shape.setColor(accent);
            bar.setBackground(shape);
            plot.addView(bar, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, values[i]));

            column.addView(plot, new LinearLayout.LayoutParams(dp(16), 0, 1f));

            TextView label = new TextView(this);
            label.setText(labels[i]);
            label.setTextColor(grey);
            label.setTextSize(11);
            label.setGravity(Gravity.CENTER);
            label.setPadding(0, dp(6), 0, 0);
            column.addView(label, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            barChart.addView(column, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
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
                    ViewGroup.LayoutParams.WRAP_CONTENT));

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
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView count = new TextView(this);
            count.setText(String.valueOf(counts[i]));
            count.setTextColor(grey);
            count.setTextSize(14);
            count.setGravity(Gravity.END);
            row.addView(count, new LinearLayout.LayoutParams(dp(32),
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            categoryList.addView(row);
        }
    }
}