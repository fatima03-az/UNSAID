package com.fatima.unsaid;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class CaptureActivity extends AppCompatActivity {

    EditText etThought;
    ActivityResultLauncher<Intent> voiceLauncher;
    ActivityResultLauncher<Intent> reviewLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        etThought = findViewById(R.id.etThought);

        // When the items were saved on the Review screen, close this screen too
        reviewLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) finish();
                });

        // Receives the spoken text and adds it to the box
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

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnMic).setOnClickListener(v -> startVoice());

        findViewById(R.id.btnOrganize).setOnClickListener(v -> {
            String thought = etThought.getText().toString().trim();
            if (thought.isEmpty()) {
                Toast.makeText(this, "Write something first", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, ReviewActivity.class);
                intent.putExtra("thought", thought);
                reviewLauncher.launch(intent);
            }
        });

        // The Voice shortcut on Home opens this screen already listening
        if (savedInstanceState == null && getIntent().getBooleanExtra("voice", false)) {
            startVoice();
        }
    }

    private void startVoice() {
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
    }
}