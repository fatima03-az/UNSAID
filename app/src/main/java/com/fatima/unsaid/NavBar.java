package com.fatima.unsaid;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

public class NavBar {

    // active: 0 = Home, 1 = Tasks, 2 = History
    public static void setup(Activity activity, int active) {
        TextView[] tabs = {
                activity.findViewById(R.id.navHome),
                activity.findViewById(R.id.navTasks),
                activity.findViewById(R.id.navHistory)
        };
        Class<?>[] targets = {
                MainActivity.class, TasksActivity.class, HistoryActivity.class
        };

        int accent = ContextCompat.getColor(activity, R.color.unsaid_accent);
        int grey = ContextCompat.getColor(activity, R.color.unsaid_text_secondary);

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            boolean isActive = (i == active);
            tabs[i].setTextColor(isActive ? accent : grey);
            tabs[i].setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);

            tabs[i].setOnClickListener(v -> {
                if (index == active) return;
                Intent intent = new Intent(activity, targets[index]);
                if (index == 0) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                }
                activity.startActivity(intent);
                if (active != 0 && index != 0) activity.finish();
            });
        }
    }
}