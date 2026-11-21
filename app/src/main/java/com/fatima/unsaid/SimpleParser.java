package com.fatima.unsaid;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SimpleParser {

    private static final String DAYS =
            "monday|tuesday|wednesday|thursday|friday|saturday|sunday";

    // Finds words like "tomorrow", "Friday", "next Monday", "due Friday"
    private static final Pattern DATE = Pattern.compile(
            "(?i)\\b(?:(?:due|by|on|for|before)\\s+)?(today|tonight|tomorrow|day after tomorrow"
                    + "|next\\s+(?:week|" + DAYS + ")"
                    + "|this\\s+(?:weekend|evening|morning|afternoon)"
                    + "|" + DAYS + ")\\b");

    public static List<Task> parse(String input) {
        List<Task> result = new ArrayList<>();
        if (input == null) return result;

        // Split the thought into small pieces
        String[] parts = input.split("(?i)[.!?;\\n]+|,|\\s+and\\s+|\\s+then\\s+|\\s+also\\s+");

        for (String part : parts) {
            String text = part.trim();
            text = text.replaceFirst("(?i)^(and|then|also|so|but|oh|okay|ok)\\s+", "");
            if (text.length() < 3) continue;

            String original = text;

            // Find a deadline and remove it from the title
            String deadline = "";
            Matcher m = DATE.matcher(text);
            if (m.find()) {
                deadline = titleCase(m.group(1));
                text = (text.substring(0, m.start()) + " " + text.substring(m.end())).trim();
            }

            // Decide the type
            String type;
            if (has(original, "need to|have to|has to|must|should|buy|call|submit|prepare|finish"
                    + "|complete|send|write|email|text|remember|pick up|book|pay|clean|study|fix"
                    + "|read|plan|assignment|homework|due|go to|get|make|order|wash|cook|visit"
                    + "|return|renew|practice")) {
                type = "Task";
            } else if (has(original, "meeting|appointment|interview|party|birthday|wedding"
                    + "|dinner|exam|concert")) {
                type = "Event";
            } else {
                type = "Note";
            }

            String title = cleanTitle(text);
            Task task = new Task(title, type, category(original), priority(original, deadline), deadline);
            Calendar due = toDate(deadline);
            if (due != null) {
                task.dueMillis = due.getTimeInMillis();
                task.deadline = new SimpleDateFormat("EEE, d MMM", Locale.getDefault())
                        .format(due.getTime());
            }
            result.add(task);
        }
        return result;
    }

    private static String category(String t) {
        if (has(t, "assignment|homework|exam|quiz|study|presentation|project|lecture|class"
                + "|paper|report|submit|university|college|thesis")) return "Academic";
        if (has(t, "call|text|email|message|reply|meet|phone|contact")) return "Communication";
        if (has(t, "doctor|gym|workout|medicine|dentist|exercise|appointment|hospital")) return "Health";
        if (has(t, "work|office|boss|client|meeting|job")) return "Work";
        return "Personal";
    }

    private static String priority(String t, String deadline) {
        if (has(t, "urgent|asap|important|today|tonight|tomorrow|exam|deadline")) return "High";
        if (!deadline.isEmpty() || has(t, "due")) return "Medium";
        return "Low";
    }

    private static String cleanTitle(String text) {
        String t = text;
        t = t.replaceFirst("(?i)^(i\\s+)?(also\\s+)?(really\\s+)?(need to|have to|has to|must|should|want to|got to|gotta|ought to)\\s+", "");
        t = t.replaceFirst("(?i)^i\\s+have\\s+(a|an|the)\\s+", "");
        t = t.replaceFirst("(?i)^(remember to|don't forget to)\\s+", "");
        t = t.replaceAll("(?i)\\s+(for|by|on|at|due|before)$", "");
        t = t.replaceAll("\\s+", " ").trim();
        if (t.isEmpty()) return capitalize(text);
        return capitalize(t);
    }

    private static boolean has(String text, String words) {
        return Pattern.compile("(?i)\\b(" + words + ")\\b").matcher(text).find();
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private static String titleCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (String word : s.split("\\s+")) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(capitalize(word.toLowerCase()));
        }
        return sb.toString();
    }
    private static Calendar toDate(String word) {
        if (word.isEmpty()) return null;
        String w = word.toLowerCase(Locale.ROOT);

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 9);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        if (w.equals("today") || w.equals("this morning") || w.equals("this afternoon")) {
            // stays today
        } else if (w.equals("tonight") || w.equals("this evening")) {
            c.set(Calendar.HOUR_OF_DAY, 19);
        } else if (w.equals("tomorrow")) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        } else if (w.equals("day after tomorrow")) {
            c.add(Calendar.DAY_OF_YEAR, 2);
        } else if (w.equals("next week")) {
            c.add(Calendar.DAY_OF_YEAR, 7);
        } else if (w.equals("this weekend")) {
            c.add(Calendar.DAY_OF_YEAR, daysUntil(c, Calendar.SATURDAY, false));
        } else {
            boolean next = w.startsWith("next ");
            String day = next ? w.substring(5) : w;
            int target = dayNumber(day);
            if (target < 0) return null;
            c.add(Calendar.DAY_OF_YEAR, daysUntil(c, target, next));
        }
        return c;
    }

    private static int daysUntil(Calendar c, int target, boolean strictlyAfter) {
        int today = c.get(Calendar.DAY_OF_WEEK);
        int d = (target - today + 7) % 7;
        if (strictlyAfter && d == 0) d = 7;
        return d;
    }

    private static int dayNumber(String day) {
        switch (day) {
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            case "sunday": return Calendar.SUNDAY;
        }
        return -1;
    }
}
