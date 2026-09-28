package com.srajan.batchnotify;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/** Catches every notification. Holds it, or lets it through if the app is allowed. */
public class BatchListenerService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        Notification n = sbn.getNotification();

        if (!Prefs.isEnabled(this)) return;                        // batching paused
        if (pkg.equals(getPackageName())) return;                  // our own batch notifications
        if (sbn.isOngoing()) return;                               // music, navigation, downloads
        if (Notification.CATEGORY_CALL.equals(n.category)) return;  // never block calls
        if (Notification.CATEGORY_ALARM.equals(n.category)) return; // never block alarms
        if (Prefs.allowlist(this).contains(pkg)) return;            // user's important apps

        // Group summaries carry no real content, just remove them
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) == 0) {
            Bundle ex = n.extras;
            HeldStore.add(this, pkg,
                    str(ex.getCharSequence(Notification.EXTRA_TITLE)),
                    str(ex.getCharSequence(Notification.EXTRA_TEXT)),
                    sbn.getPostTime());
        }
        cancelNotification(sbn.getKey()); // hide it from the user for now
    }

    private static String str(CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }
}
