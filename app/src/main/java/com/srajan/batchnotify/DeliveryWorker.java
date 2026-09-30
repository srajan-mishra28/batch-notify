package com.srajan.batchnotify;

import android.content.Context;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;

/** Runs every N hours (or on "Release now"): releases everything that was held. */
public class DeliveryWorker extends Worker {

    public DeliveryWorker(Context c, WorkerParameters p) {
        super(c, p);
    }

    @Override
    public Result doWork() {
        Context c = getApplicationContext();
        JSONArray items = HeldStore.drain(c);
        if (items.length() > 0) Notifier.postBatch(c, items);

        boolean manual = getInputData().getBoolean(Scheduler.KEY_MANUAL, false);
        boolean daily = getInputData().getBoolean(Scheduler.KEY_DAILY, false);

        // Interval runs move the next interval clock forward. Manual releases don't.
        if (!manual && !daily) {
            Prefs.setNextBatchAt(c, System.currentTimeMillis()
                    + TimeUnit.HOURS.toMillis(Prefs.intervalHours(c)));
        }

        // A daily run schedules the next occurrence for tomorrow.
        if (daily && Prefs.hasDailyTime()) {
            Scheduler.scheduleDailyTime(c);
        }

        return Result.success();
    }
}
