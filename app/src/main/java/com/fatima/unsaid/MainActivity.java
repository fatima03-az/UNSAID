package com.fatima.unsaid;

import android.app.AlarmManager;
import android.net.Uri;
import android.provider.Settings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText etThought;
    MaterialButton btnOrganize;
    TextView tvSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etThought = findViewById(R.id.etThought);
        btnOrganize = findViewById(R.id.btnOrganize);
        tvSummary = findViewById(R.id.tvSummary);

        // Android 13 and newer must ask permission to show notifications
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }



        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
            }
        }
        btnOrganize.setOnClickListener(v -> {
            String thought = etThought.getText().toString().trim();
            if (thought.isEmpty()) {
                Toast.makeText(this, "Write something first", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, ReviewActivity.class);
                intent.putExtra("thought", thought);
                startActivity(intent);
            }
        });

        tvSummary.setOnClickListener(v ->
                startActivity(new Intent(this, TasksActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<Task> all = AppDatabase.getInstance(this).taskDao().getAll();
        int pending = 0;
        for (Task t : all) {
            if (!t.done) pending++;
        }
        if (pending == 0) {
            tvSummary.setText("No pending tasks · tap to open My Tasks");
        } else {
            tvSummary.setText(pending + " pending · tap to open My Tasks");
        }
    }
}