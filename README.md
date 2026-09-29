# Batch Notify

**Get your notifications in batches, not every minute.**

Batch Notify quietly holds notifications from your apps and delivers them together every **3, 4, 6 or 12 hours**. Apps that really matter (like calls or family chats) still reach you right away.

---

## Why use it

- **Fewer interruptions.** One batch every few hours instead of dozens of pings.
- **Deeper focus.** No buzz pulling you out of work, study or rest.
- **Less phone checking.** Nothing new to see, so less reason to pick up the phone.
- **Nothing important missed.** Calls, alarms and your chosen apps always come through.
- **Nothing lost.** Held notifications survive app restarts and phone reboots.
- **Private by design.** The app has no internet permission. Your notifications never leave your phone.
- **Light.** Works quietly in the background with minimal battery use.

---

## How it works

| Situation | What happens |
|---|---|
| Message from a normal app | Hidden instantly. No sound, no banner. Saved for later. |
| Message from an important app | Shows normally, right away. |
| Calls and alarms | Always show normally. |
| Music, maps, downloads | Always show normally. |
| Batch time arrives | One summary per app, e.g. **"Instagram (5)"**. Tap to open the app. |

---

## Install the app

Requires **Android 8.0 or newer**. iOS is not supported (Apple doesn't allow apps to manage notifications).

### 1. Download the APK
1. Open this repo's **Actions** tab.
2. Click the latest **Build APK** run with a green tick.
3. Scroll down to **Artifacts** and download **BatchNotify-apk**.
4. Unzip it. You'll get **app-debug.apk**.
5. Send it to your phone (Google Drive, email, or USB).

### 2. Uninstall any old version
If Batch Notify is already installed, **uninstall it first**. Each build is signed differently, so Android won't install a new one over an old one.

### 3. Pause Play Protect (temporarily)
Play Protect doesn't know this app, so it may block it with **"App not installed"** or **INSTALL_FAILED_VERIFICATION_FAILURE**.

1. Open the **Play Store**.
2. Tap your **profile picture**, then **Play Protect**.
3. Tap the **gear icon** (top right).
4. Turn off **Scan apps with Play Protect**.

> Remember to turn it back **on** after installing (step 5).

**Also check, if install still fails:**
- **Settings, then Security:** turn off **Advanced Protection** if it's on.
- **Xiaomi / Redmi / POCO:** turn off the **security scan before install** option.

### 4. Install
1. Tap **app-debug.apk** on your phone.
2. If asked, allow **Install unknown apps** for that app (Files, Drive or Chrome).
3. Tap **Install**. If warned, choose **Install anyway**.

### 5. Turn Play Protect back on
Go back to Play Protect settings and turn **Scan apps with Play Protect** on again.

---

## First-time setup

1. **Open Batch Notify** and allow notifications when asked.
2. Tap **Open settings** on the green card. Turn on access for **Batch Notify** and confirm.
3. Go back. The green card disappears.
4. **Pick your interval:** 3h, 4h, 6h or 12h.
5. Tap **Important apps** and switch on the ones that should always reach you (e.g. Phone, WhatsApp, your bank).
6. **Set battery to Unrestricted:** long-press the app icon, then App info, then Battery, then Unrestricted.
   - On Xiaomi, Oppo, Vivo, Realme: also turn on **Autostart**.

That's it. Put your phone down.

---

## Using the app

### Home screen
- **The ring** fills up as the next batch gets closer.
- **The big number** shows how many notifications are waiting.
- **Release now** delivers everything immediately (your schedule stays the same).
- **See waiting** lets you peek at held notifications without releasing them.
- **Deliver every** changes the interval. The timer restarts.
- **Batching switch** pauses the app. Held notifications are released, and new ones show normally.

### Important apps
Search for an app and flip its switch. Switched-on apps skip batching.

### Waiting
Shows held notifications grouped by app, with times. Tap **Release all now** to get them.

---

## Tips

- **Start with 3 hours.** Move to 6 or 12 once you're comfortable.
- **Keep the important list short.** Every app you add is one more interruption.
- **Use "Release now"** when you take a break, instead of checking each app.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| "App not installed" | Uninstall the old version, pause Play Protect, try again. |
| Notifications aren't being held | Notification access got turned off. Tap **Open settings** and turn it on. |
| Batches come late or never | Set battery to **Unrestricted**. On Xiaomi/Oppo/Vivo also enable **Autostart**. |
| No summary after release | Allow notifications for Batch Notify in App info. |

**Known limits:** batches may arrive a few minutes late (Android saves battery). Summaries show text only, so buttons like "Reply" are not kept.

---

## For developers

**Stack:** Java (core logic) + Kotlin / Jetpack Compose (UI) + WorkManager. Min SDK 26.

| File | Job |
|---|---|
| `BatchListenerService.java` | Receives every notification. Lets it through, or hides and saves it. |
| `HeldStore.java` | Saves held notifications to a local file. |
| `Scheduler.java` + `DeliveryWorker.java` | Runs every N hours and releases the batch. |
| `Notifier.java` | Builds one summary notification per app. |
| `Prefs.java` | Settings: interval, on/off, important apps. |
| `ui/*.kt` | Home, Important apps and Waiting screens. |

**Build locally:** open the folder in Android Studio, wait for Gradle sync, press **Run**.

**Build on GitHub:** every push runs `.github/workflows/build.yml` and uploads the APK under **Actions, then Artifacts**.

**Quick test from a laptop** (phone connected, USB debugging on):
```
adb shell cmd notification post -t "Test title" test1 "Hello from test"
```
The notification should vanish and the waiting count should go up.
