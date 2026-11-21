package com.fatima.unsaid;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderHelper {

    public static boolean schedule(Context context, Task task) {
        if (task.done || task.dueMillis <= System.currentTimeMillis()) return false;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = pendingIntent(context, task);

        // Exact alarms need the user's permission on newer Android versions.
        // If it isn't allowed, we fall back to a normal alarm that never crashes.
        boolean exactAllowed = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        try {
            if (exactAllowed) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueMillis, pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueMillis, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueMillis, pi);
        }
        return true;
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