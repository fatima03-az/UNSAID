package com.fatima.unsaid;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class ReminderHelper {

    public static void schedule(Context context, Task task) {
        if (task.done || task.dueMillis <= System.currentTimeMillis()) return;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueMillis,
                pendingIntent(context, task));
    }

    public static void cancel(Context context, Task task) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent(context, task));
    }

    private static PendingIntent pendingIntent(Context context, Task task) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("title", task.title);
        intent.putExtra("id", task.id);
        return PendingIntent.getBroadcast(context, task.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}