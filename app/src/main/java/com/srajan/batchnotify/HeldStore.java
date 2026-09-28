package com.srajan.batchnotify;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Saves held notifications to a file so they survive app restarts. */
public final class HeldStore {
    private static final String FILE = "held.json";

    public static synchronized void add(Context c, String pkg, String title, String text, long time) {
        JSONArray arr = load(c);
        try {
            arr.put(new JSONObject()
                    .put("pkg", pkg).put("title", title).put("text", text).put("time", time));
        } catch (JSONException ignored) { }
        save(c, arr);
    }

    /** Returns everything held and empties the store. */
    public static synchronized JSONArray drain(Context c) {
        JSONArray arr = load(c);
        save(c, new JSONArray());
        return arr;
    }

    /** Read without clearing (for the "waiting" screen). */
    public static synchronized JSONArray peek(Context c) {
        return load(c);
    }

    public static synchronized int count(Context c) {
        return load(c).length();
    }

    private static JSONArray load(Context c) {
        File f = new File(c.getFilesDir(), FILE);
        if (!f.exists()) return new JSONArray();
        try {
            return new JSONArray(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static void save(Context c, JSONArray arr) {
        try {
            Files.write(new File(c.getFilesDir(), FILE).toPath(),
                    arr.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) { }
    }
}
