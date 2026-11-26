package com.fatima.unsaid;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GeminiClient {

    // Models to try in order. If Google changes names, edit this list only.
    private static final String[] MODELS = {
            "gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.8-flash"};
    private static final String BASE =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final List<String> TYPES = Arrays.asList("Task", "Event", "Note");
    private static final List<String> CATEGORIES =
            Arrays.asList("Academic", "Personal", "Communication", "Work", "Health");
    private static final List<String> PRIORITIES = Arrays.asList("High", "Medium", "Low");

    public static String lastError = "";

    // Returns null when anything goes wrong, so the app can use the offline parser
    public static List<Task> organize(String thought) {
        lastError = "";
        String key = BuildConfig.GEMINI_API_KEY;
        if (key == null || key.isEmpty()) {
            lastError = "No API key found";
            return null;
        }
        String prompt = buildPrompt(thought);

        for (String model : MODELS) {
            for (int attempt = 0; attempt < 1; attempt++) {
                try {
                    int[] code = new int[1];
                    String text = call(model, key, prompt, code);
                    if (text != null) {
                        List<Task> result = parse(text);
                        if (result != null) return result;
                        lastError = "AI answer could not be read";
                        break;   // try the next model
                    }
                    // Busy or rate-limited: wait a moment and try again
                    if (code[0] == 503 || code[0] == 429 || code[0] == 500) {
                        Thread.sleep(1500);
                        continue;
                    }
                    break;       // other errors: try the next model
                } catch (Exception e) {
                    lastError = String.valueOf(e);
                    break;
                }
            }
        }
        return null;
    }

    private static String buildPrompt(String thought) {
        String today = new SimpleDateFormat("EEEE, yyyy-MM-dd", Locale.ENGLISH)
                .format(new Date());
        return "You organize a person's messy thoughts into separate items. "
                + "Today is " + today + ". Return ONLY a JSON array. Each item has: "
                + "\"title\" (short, starts with a verb, no date words), "
                + "\"type\" (Task, Event or Note), "
                + "\"category\" (one of Academic, Personal, Communication, Work, Health), "
                + "\"priority\" (High, Medium or Low), "
                + "\"date\" (YYYY-MM-DD if a deadline is stated or implied, otherwise an empty string). "
                + "Use Note for feelings or ideas that need no action. "
                + "Thoughts: " + thought;
    }

    // Sends the request. Returns the AI's text, or null (with lastError and code set)
    private static String call(String model, String key, String prompt, int[] code)
            throws Exception {
        JSONObject part = new JSONObject().put("text", prompt);
        JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
        JSONObject body = new JSONObject()
                .put("contents", new JSONArray().put(content))
                .put("generationConfig",
                        new JSONObject().put("responseMimeType", "application/json"));

        URL url = new URL(BASE + model + ":generateContent");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(25000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-goog-api-key", key);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        code[0] = conn.getResponseCode();
        if (code[0] != 200) {
            StringBuilder err = new StringBuilder();
            try (BufferedReader er = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String l;
                while ((l = er.readLine()) != null) err.append(l);
            } catch (Exception ignored) {
            }
            String msg = err.toString();
            lastError = "HTTP " + code[0] + " " + (msg.length() > 150 ? msg.substring(0, 150) : msg);
            return null;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        return new JSONObject(sb.toString())
                .getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts")
                .getJSONObject(0).getString("text");
    }

    private static List<Task> parse(String text) {
        try {
            text = text.replace("```json", "").replace("```", "").trim();
            JSONArray items = new JSONArray(text);
            List<Task> result = new ArrayList<>();
            for (int i = 0; i < items.length(); i++) {
                JSONObject o = items.getJSONObject(i);
                String title = o.optString("title", "").trim();
                if (title.isEmpty()) continue;

                String type = pick(o.optString("type"), TYPES, "Task");
                String category = pick(o.optString("category"), CATEGORIES, "Personal");
                String priority = pick(o.optString("priority"), PRIORITIES, "Medium");

                Task task = new Task(title, type, category, priority, "");
                applyDate(task, o.optString("date", ""));
                result.add(task);
            }
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            return null;
        }
    }

    private static String pick(String value, List<String> allowed, String fallback) {
        for (String a : allowed) {
            if (a.equalsIgnoreCase(value.trim())) return a;
        }
        return fallback;
    }

    private static void applyDate(Task task, String date) {
        if (date == null || date.length() < 10) return;
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                    .parse(date.substring(0, 10));
            if (parsed == null) return;

            Calendar due = Calendar.getInstance();
            due.setTime(parsed);
            due.set(Calendar.HOUR_OF_DAY, 9);
            due.set(Calendar.MINUTE, 0);
            due.set(Calendar.SECOND, 0);
            due.set(Calendar.MILLISECOND, 0);

            Calendar startOfToday = Calendar.getInstance();
            startOfToday.set(Calendar.HOUR_OF_DAY, 0);
            startOfToday.set(Calendar.MINUTE, 0);
            startOfToday.set(Calendar.SECOND, 0);
            startOfToday.set(Calendar.MILLISECOND, 0);
            if (due.before(startOfToday)) return;   // ignore dates in the past

            task.deadline = new SimpleDateFormat("EEE, d MMM", Locale.getDefault())
                    .format(due.getTime());
            if (due.getTimeInMillis() > System.currentTimeMillis()) {
                task.dueMillis = due.getTimeInMillis();   // reminder only if still in the future
            }
        } catch (Exception ignored) {
        }
    }
}