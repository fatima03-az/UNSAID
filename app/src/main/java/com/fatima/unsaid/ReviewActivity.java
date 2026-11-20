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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReviewActivity extends AppCompatActivity {

    List<Task> tasks;
    LinearLayout taskList;
    String[] categories = {"Academic", "Personal", "Communication", "Work", "Health"};
    String[] priorities = {"High", "Medium", "Low"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        taskList = findViewById(R.id.taskList);
        String thought = getIntent().getStringExtra("thought");
        tasks = SimpleParser.parse(thought);

        render();

        findViewById(R.id.btnSave).setOnClickListener(v -> saveAll());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
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
        if (tasks.isEmpty()) {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show();
            return;
        }
        TaskDao dao = AppDatabase.getInstance(this).taskDao();
        List<Long> ids = dao.insertAll(tasks);
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            t.id = ids.get(i).intValue();
            ReminderHelper.schedule(this, t);
        }
        Toast.makeText(this, tasks.size() + " items saved", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, TasksActivity.class));
        finish();
    }
}