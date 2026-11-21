package com.fatima.unsaid;

import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView tvGreeting, tvCardTitle, tvCardSub, tvUpcomingEmpty;
    LinearLayout upcomingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvGreeting = findViewById(R.id.tvGreeting);
        tvCardTitle = findViewById(R.id.tvCardTitle);
        tvCardSub = findViewById(R.id.tvCardSub);
        tvUpcomingEmpty = findViewById(R.id.tvUpcomingEmpty);
        upcomingList = findViewById(R.id.upcomingList);

        NavBar.setup(this, 0);

        // Android 13 and newer must ask permission to show notifications
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        // Exact alarms permission (so reminders arrive on time)
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
            }
        }

        findViewById(R.id.cardTasks).setOnClickListener(v -> openTasks());
        findViewById(R.id.tvSeeAll).setOnClickListener(v -> openTasks());

        findViewById(R.id.tileCapture).setOnClickListener(v ->
                startActivity(new Intent(this, CaptureActivity.class)));

        findViewById(R.id.tileVoice).setOnClickListener(v -> {
            Intent intent = new Intent(this, CaptureActivity.class);
            intent.putExtra("voice", true);
            startActivity(intent);
        });
    }

    private void openTasks() {
        startActivity(new Intent(this, TasksActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Greeting changes with the time of day
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) tvGreeting.setText("Good morning");
        else if (hour < 18) tvGreeting.setText("Good afternoon");
        else tvGreeting.setText("Good evening");

        // Start and end of today
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();
        long endOfDay = startOfDay + 24L * 60 * 60 * 1000;

        List<Task> all = AppDatabase.getInstance(this).taskDao().getAll();
        List<Task> pending = new ArrayList<>();
        int dueToday = 0;
        for (Task t : all) {
            if (t.done) continue;
            pending.add(t);
            if (t.dueMillis >= startOfDay && t.dueMillis < endOfDay) dueToday++;
        }

        if (pending.isEmpty()) {
            tvCardTitle.setText("Nothing pending");
            tvCardSub.setText("Tap to open My Tasks");
        } else {
            tvCardTitle.setText("You have " + pending.size()
                    + (pending.size() == 1 ? " pending task" : " pending tasks"));
            tvCardSub.setText(dueToday + " due today · tap to open");
        }

        // Next three tasks: earliest dates first, tasks without a date last
        pending.sort((a, b) -> Long.compare(
                a.dueMillis == 0 ? Long.MAX_VALUE : a.dueMillis,
                b.dueMillis == 0 ? Long.MAX_VALUE : b.dueMillis));

        upcomingList.removeAllViews();
        int shown = 0;
        for (Task t : pending) {
            if (shown == 3) break;
            View card = getLayoutInflater().inflate(R.layout.item_task, upcomingList, false);
            ((TextView) card.findViewById(R.id.tvTitle)).setText(t.title);
            ((TextView) card.findViewById(R.id.tvInfo)).setText(t.getSummary());
            card.setOnClickListener(v -> openTasks());
            upcomingList.addView(card);
            shown++;
        }
        tvUpcomingEmpty.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
    }
}