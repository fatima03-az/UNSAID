package com.fatima.unsaid;

import android.app.AlarmManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText etThought;
    MaterialButton btnOrganize;
    TextView tvSummary;
    ActivityResultLauncher<Intent> voiceLauncher;

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

        // Exact alarms permission (so reminders arrive on time)
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
            }
        }

        // Voice input: receives the spoken text and adds it to the box
        voiceLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> spoken = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (spoken != null && !spoken.isEmpty()) {
                            String old = etThought.getText().toString().trim();
                            String text = spoken.get(0);
                            etThought.setText(old.isEmpty() ? text : old + ". " + text);
                            etThought.setSelection(etThought.getText().length());
                        }
                    }
                });

        findViewById(R.id.btnMic).setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say what's on your mind");
            try {
                voiceLauncher.launch(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Voice input isn't available on this device",
                        Toast.LENGTH_LONG).show();
            }
        });

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
        findViewById(R.id.tvHistory).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

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