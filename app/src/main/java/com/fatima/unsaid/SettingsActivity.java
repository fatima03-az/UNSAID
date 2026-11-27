package com.fatima.unsaid;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        NavBar.setup(this, 4);

        MaterialSwitch swReminders = findViewById(R.id.swReminders);
        MaterialSwitch swCloudAi = findViewById(R.id.swCloudAi);
        MaterialSwitch swSummary = findViewById(R.id.swSummary);

        swReminders.setChecked(Prefs.remindersOn(this));
        swCloudAi.setChecked(Prefs.cloudAiOn(this));
        swSummary.setChecked(Prefs.dailySummaryOn(this));

        swReminders.setOnCheckedChangeListener((b, on) -> Prefs.setRemindersOn(this, on));
        swCloudAi.setOnCheckedChangeListener((b, on) -> Prefs.setCloudAiOn(this, on));
        swSummary.setOnCheckedChangeListener((b, on) -> {
            Prefs.setDailySummaryOn(this, on);
            if (on) DailySummary.schedule(this);
            else DailySummary.cancel(this);
        });

        findViewById(R.id.btnClear).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Clear all data?")
                        .setMessage("This deletes every task and every saved thought. It can't be undone.")
                        .setPositiveButton("Delete everything", (d, w) -> clearAll())
                        .setNegativeButton("Cancel", null)
                        .show());
    }

    private void clearAll() {
        AppDatabase db = AppDatabase.getInstance(this);
        List<Task> tasks = db.taskDao().getAll();
        for (Task t : tasks) {
            ReminderHelper.cancel(this, t);
            db.taskDao().delete(t);
        }
        for (Thought t : db.thoughtDao().getAll()) {
            db.thoughtDao().delete(t);
        }
        Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
    }
}