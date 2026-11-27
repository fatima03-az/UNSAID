package com.fatima.unsaid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;
import java.util.List;

public class DailySummaryReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "unsaid_summary";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Prefs.dailySummaryOn(context)) return;

        // Count the tasks
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfToday = cal.getTimeInMillis();
        long day = 24L * 60 * 60 * 1000;

        List<Task> all = AppDatabase.getInstance(context).taskDao().getAll();
        int pending = 0, overdue = 0, dueToday = 0, dueTomorrow = 0;
        for (Task t : all) {
            if (t.done) continue;
            pending++;
            if (t.dueMillis > 0) {
                if (t.dueMillis < startOfToday) overdue++;
                else if (t.dueMillis < startOfToday + day) dueToday++;
                else if (t.dueMillis < startOfToday + 2 * day) dueTomorrow++;
            }
        }

        String text;
        if (pending == 0) {
            text = "All clear. Nothing pending.";
        } else {
            StringBuilder sb = new StringBuilder(pending + " pending");
            if (overdue > 0) sb.append(" · ").append(overdue).append(" overdue");
            if (dueToday > 0) sb.append(" · ").append(dueToday).append(" due today");
            if (dueTomorrow > 0) sb.append(" · ").append(dueTomorrow).append(" due tomorrow");
            text = sb.toString();
        }

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "Daily summary", NotificationManager.IMPORTANCE_DEFAULT));
        }

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 7778, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Your Unsaid summary")
                .setContentText(text)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();

        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // No permission: skip showing, but still plan tomorrow's summary
        } else {
            nm.notify(7778, n);
        }

        // Plan the next summary for tomorrow
        DailySummary.schedule(context);
    }
}