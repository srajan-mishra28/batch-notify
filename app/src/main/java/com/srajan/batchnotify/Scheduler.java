package com.srajan.batchnotify;

import android.content.Context;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

/** Schedules the batch job. WorkManager keeps it alive across reboots. */
public final class Scheduler {
    static final String KEY_MANUAL = "manual";

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

    public static void deliverNow(Context c) {
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(DeliveryWorker.class)
                .setInputData(new Data.Builder().putBoolean(KEY_MANUAL, true).build())
                .build();
        WorkManager.getInstance(c).enqueue(req);
    }
}
