package com.fatima.unsaid;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    LinearLayout historyList;
    TextView tvEmpty;
    String query = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyList = findViewById(R.id.historyList);
        tvEmpty = findViewById(R.id.tvEmpty);

        EditText etSearch = findViewById(R.id.etSearch);
        GradientDrawable box = new GradientDrawable();
        box.setCornerRadius(24 * getResources().getDisplayMetrics().density);
        box.setColor(ContextCompat.getColor(this, R.color.unsaid_surface));
        etSearch.setBackground(box);
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) {
                query = s.toString().trim().toLowerCase();
                loadThoughts();
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadThoughts();
    }

    private void loadThoughts() {
        ThoughtDao dao = AppDatabase.getInstance(this).thoughtDao();
        List<Thought> all = dao.getAll();
        historyList.removeAllViews();
        int count = 0;
        SimpleDateFormat format = new SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault());

        for (Thought t : all) {
            if (!query.isEmpty() && !t.text.toLowerCase().contains(query)) continue;
            count++;

            View card = getLayoutInflater().inflate(R.layout.item_thought, historyList, false);
            ((TextView) card.findViewById(R.id.tvDate))
                    .setText(format.format(new Date(t.createdMillis)));
            ((TextView) card.findViewById(R.id.tvText)).setText(t.text);
            ((TextView) card.findViewById(R.id.tvCount))
                    .setText(t.itemCount + (t.itemCount == 1 ? " item found" : " items found"));

            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, ReviewActivity.class);
                intent.putExtra("thought", t.text);
                startActivity(intent);
            });

            card.findViewById(R.id.tvDelete).setOnClickListener(v -> {
                dao.delete(t);
                loadThoughts();
            });

            historyList.addView(card);
        }
        tvEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }
}