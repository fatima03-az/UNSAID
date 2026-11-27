package com.fatima.unsaid;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public class DailySummary {

    // The time of the daily summary (20:00 = 8:00 PM)
    static final int SUMMARY_HOUR = 20;
    static final int SUMMARY_MINUTE = 0;

    public static void schedule(Context context) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, SUMMARY_HOUR);
        c.set(Calendar.MINUTE, SUMMARY_MINUTE);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_YEAR, 1);   // already past today, so use tomorrow
        }

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = pendingIntent(context);
        long time = c.getTimeInMillis();

        boolean exactAllowed = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        try {
            if (exactAllowed) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
        }
    }

    public static void cancel(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent(context));
    }

    private static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, DailySummaryReceiver.class);
        return PendingIntent.getBroadcast(context, 7777, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}