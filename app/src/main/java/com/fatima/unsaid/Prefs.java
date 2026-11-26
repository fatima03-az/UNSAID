package com.fatima.unsaid;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("unsaid_settings", Context.MODE_PRIVATE);
    }

    public static boolean remindersOn(Context c) { return sp(c).getBoolean("reminders", true); }
    public static boolean cloudAiOn(Context c) { return sp(c).getBoolean("cloud_ai", true); }
    public static boolean dailySummaryOn(Context c) { return sp(c).getBoolean("daily_summary", false); }

    public static void setRemindersOn(Context c, boolean v) { sp(c).edit().putBoolean("reminders", v).apply(); }
    public static void setCloudAiOn(Context c, boolean v) { sp(c).edit().putBoolean("cloud_ai", v).apply(); }
    public static void setDailySummaryOn(Context c, boolean v) { sp(c).edit().putBoolean("daily_summary", v).apply(); }
}
