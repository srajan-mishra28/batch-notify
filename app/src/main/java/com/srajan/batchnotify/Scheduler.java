package com.srajan.batchnotify;

import android.content.Context;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingWorkPolicy;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;

/** Schedules the batch job. WorkManager keeps it alive across reboots , and optional additional daily delivery*/
public final class Scheduler {
    static final String KEY_MANUAL = "manual";
    static final String KEY_DAILY="daily";
    private static final String DAILY_WORK="daily_batch";


    /** reset=true restarts the timer (use when the interval changes). */
    public static void schedule(Context c, boolean reset) {
        int h = Prefs.intervalHours(c);
        if (reset || Prefs.nextBatchAt(c) == 0L) {
            Prefs.setNextBatchAt(c, System.currentTimeMillis() + TimeUnit.HOURS.toMillis(h));
        }
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(DeliveryWorker.class, h, TimeUnit.HOURS)
                .setInitialDelay(h, TimeUnit.HOURS) // first batch after one full interval
                .build();
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(
                "batch",
                reset ? ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE : ExistingPeriodicWorkPolicy.KEEP,
                req);
    }

    /** Schedule the optional additional delivery at the user's daily clock time. */
    public static void scheduleDailyTime(Context c) {
        if (!Prefs.hasDailyTime(c)) return;

        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, Prefs.dailyHour(c));
        next.set(Calendar.MINUTE, Prefs.dailyMinute(c));
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(now)) next.add(Calendar.DAY_OF_YEAR, 1);

        long delay = next.getTimeInMillis() - now.getTimeInMillis();
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(DeliveryWorker.class)
                .setInputData(new Data.Builder().putBoolean(KEY_DAILY, true).build())
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(c).enqueueUniqueWork(
                DAILY_WORK, ExistingWorkPolicy.REPLACE, req);
    }

    public static void cancelDailyTime(Context c) {
        WorkManager.getInstance(c).cancelUniqueWork(DAILY_WORK);
    }


    public static void deliverNow(Context c) {
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(DeliveryWorker.class)
                .setInputData(new Data.Builder().putBoolean(KEY_MANUAL, true).build())
                .build();
        WorkManager.getInstance(c).enqueue(req);
    }
}
