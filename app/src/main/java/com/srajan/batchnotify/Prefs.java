package com.srajan.batchnotify;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/** User settings: on/off, batch interval, next batch time, and apps that bypass batching. */
public final class Prefs {

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("batch_prefs", Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context c) { return sp(c).getBoolean("enabled", true); }
    public static void setEnabled(Context c, boolean on) { sp(c).edit().putBoolean("enabled", on).apply(); }

    public static int intervalHours(Context c) { return sp(c).getInt("interval", 3); }
    public static void setIntervalHours(Context c, int hours) { sp(c).edit().putInt("interval", hours).apply(); }

    public static long nextBatchAt(Context c) { return sp(c).getLong("next", 0L); }
    public static void setNextBatchAt(Context c, long time) { sp(c).edit().putLong("next", time).apply(); }

    public static Set<String> allowlist(Context c) {
        return new HashSet<>(sp(c).getStringSet("allow", new HashSet<>()));
    }

    public static void setAllowed(Context c, String pkg, boolean allowed) {
        Set<String> s = allowlist(c);
        if (allowed) s.add(pkg); else s.remove(pkg);
        sp(c).edit().putStringSet("allow", s).apply();
    }
}
