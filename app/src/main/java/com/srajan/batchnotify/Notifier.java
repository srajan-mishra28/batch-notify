package com.srajan.batchnotify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Posts the batch: one summary notification per app. Tap opens that app. */
public final class Notifier {
    private static final String CHANNEL = "batch";
    private static final int MAX_LINES = 6;

    public static void postBatch(Context c, JSONArray items) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL, "Batched notifications", NotificationManager.IMPORTANCE_DEFAULT));

        // Group held items by app
        Map<String, List<String>> byApp = new LinkedHashMap<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.optJSONObject(i);
            if (o == null) continue;
            String line = o.optString("title") + ": " + o.optString("text");
            byApp.computeIfAbsent(o.optString("pkg"), k -> new ArrayList<>()).add(line);
        }

        PackageManager pm = c.getPackageManager();
        for (Map.Entry<String, List<String>> e : byApp.entrySet()) {
            String pkg = e.getKey();
            List<String> lines = e.getValue();

            Notification.InboxStyle style = new Notification.InboxStyle();
            for (int i = 0; i < Math.min(lines.size(), MAX_LINES); i++) style.addLine(lines.get(i));
            if (lines.size() > MAX_LINES) style.setSummaryText("+" + (lines.size() - MAX_LINES) + " more");

            Intent open = pm.getLaunchIntentForPackage(pkg);
            PendingIntent tap = open == null ? null : PendingIntent.getActivity(
                    c, pkg.hashCode(), open,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            Notification n = new Notification.Builder(c, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(label(pm, pkg) + " (" + lines.size() + ")")
                    .setContentText(lines.get(0))
                    .setStyle(style)
                    .setContentIntent(tap)
                    .setAutoCancel(true)
                    .setGroup("batch")
                    .build();
            nm.notify(pkg.hashCode(), n);
        }
    }

    private static String label(PackageManager pm, String pkg) {
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }
}
