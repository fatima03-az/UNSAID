package com.fatima.unsaid;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewActivity extends AppCompatActivity {

    List<Task> tasks = new ArrayList<>();
    String thought;
    LinearLayout taskList;
    TextView tvStatus;
    String[] categories = {"Academic", "Personal", "Communication", "Work", "Health"};
    String[] priorities = {"High", "Medium", "Low"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        taskList = findViewById(R.id.taskList);
        tvStatus = findViewById(R.id.tvStatus);
        thought = getIntent().getStringExtra("thought");

        findViewById(R.id.btnSave).setOnClickListener(v -> saveAll());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Try the cloud AI first. If it fails or takes too long, use the offline parser.
        tvStatus.setText("Organizing your thoughts…");
        final java.util.concurrent.atomic.AtomicBoolean finished =
                new java.util.concurrent.atomic.AtomicBoolean(false);

        // Safety timer: after 15 seconds, stop waiting
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (finished.compareAndSet(false, true) && !isFinishing()) {
                tasks = SimpleParser.parse(thought);
                tvStatus.setText("Organized offline (AI took too long: " + GeminiClient.lastError + ")");
                render();
            }
        }, 55000);

        new Thread(() -> {
            List<Task> ai = GeminiClient.organize(thought);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (!finished.compareAndSet(false, true)) return;
                if (ai != null) {
                    tasks = ai;
                    tvStatus.setText("Organized by AI · tap an item to edit it");
                } else {
                    tasks = SimpleParser.parse(thought);
                    tvStatus.setText("Organized offline (" + GeminiClient.lastError + ")");
                }
                render();
            });
        }).start();
    }

    private void render() {
        taskList.removeAllViews();
        for (Task task : tasks) {
            View card = getLayoutInflater().inflate(R.layout.item_task, taskList, false);
            TextView tvTitle = card.findViewById(R.id.tvTitle);
            TextView tvInfo = card.findViewById(R.id.tvInfo);
            tvTitle.setText(task.title);
            tvInfo.setText(task.getSummary());
            card.setOnClickListener(v -> showEditDialog(task));
            taskList.addView(card);
        }
    }

    private int indexOf(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) return i;
        }
        return 0;
    }

    private void showEditDialog(Task task) {
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_task, null);
        EditText etTitle = view.findViewById(R.id.etTitle);
        Spinner spCategory = view.findViewById(R.id.spCategory);
        Spinner spPriority = view.findViewById(R.id.spPriority);
        Button btnDate = view.findViewById(R.id.btnDate);

        etTitle.setText(task.title);
        spCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, categories));
        spPriority.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, priorities));
        spCategory.setSelection(indexOf(categories, task.category));
        spPriority.setSelection(indexOf(priorities, task.priority));

        final long[] due = {task.dueMillis};
        final String[] deadline = {task.deadline};
        btnDate.setText(deadline[0].isEmpty() ? "Pick a date" : deadline[0]);

        btnDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            if (due[0] > 0) cal.setTimeInMillis(due[0]);
            DatePickerDialog picker = new DatePickerDialog(this, (p, year, month, day) -> {
                Calendar chosen = Calendar.getInstance();
                chosen.set(year, month, day, 9, 0, 0);
                // If 9:00 has already passed today, remind in one minute
                if (chosen.getTimeInMillis() < System.currentTimeMillis()) {
                    chosen.setTimeInMillis(System.currentTimeMillis() + 60000);
                }
                due[0] = chosen.getTimeInMillis();
                deadline[0] = new SimpleDateFormat("EEE, d MMM", Locale.getDefault())
                        .format(chosen.getTime());
                btnDate.setText(deadline[0]);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH));
            picker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            picker.show();
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit item")
                .setView(view)
                .setPositiveButton("Done", (dialog, which) -> {
                    String newTitle = etTitle.getText().toString().trim();
                    if (!newTitle.isEmpty()) task.title = newTitle;
                    task.category = categories[spCategory.getSelectedItemPosition()];
                    task.priority = priorities[spPriority.getSelectedItemPosition()];
                    task.dueMillis = due[0];
                    task.deadline = deadline[0];
                    render();
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Remove", (dialog, which) -> {
                    tasks.remove(task);
                    render();
                })
                .show();
    }

    private void saveAll() {
        try {
            if (tasks.isEmpty()) {
                Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show();
                return;
            }
            TaskDao dao = AppDatabase.getInstance(this).taskDao();
            List<Long> ids = dao.insertAll(tasks);
            AppDatabase.getInstance(this).thoughtDao().insert(
                    new Thought(thought, System.currentTimeMillis(), tasks.size()));

            int reminders = 0;
            String lastTime = "";
            for (int i = 0; i < tasks.size(); i++) {
                Task t = tasks.get(i);
                t.id = ids.get(i).intValue();
                if (ReminderHelper.schedule(this, t)) {
                    reminders++;
                    lastTime = new SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())
                            .format(new Date(t.dueMillis));
                }
            }

            String msg = tasks.size() + " items saved.\n";
            if (reminders > 0) {
                msg += reminders + " reminder(s) set. Last one: " + lastTime;
            } else {
                msg += "No reminders were set (no future date on any item).";
            }

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Saved")
                    .setMessage(msg)
                    .setCancelable(false)
                    .setPositiveButton("OK", (d, w) -> {
                        setResult(RESULT_OK);
                        startActivity(new Intent(this, TasksActivity.class));
                        finish();
                    })
                    .show();
        } catch (Throwable e) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Error found")
                    .setMessage(String.valueOf(e))
                    .setPositiveButton("OK", null)
                    .show();
        }
    }
}