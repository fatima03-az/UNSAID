package com.fatima.unsaid;

public class Task {
    public String title;
    public String type;      // Task, Event or Note
    public String category;
    public String priority;  // High, Medium or Low
    public String deadline;  // can be empty

    public Task(String title, String type, String category, String priority, String deadline) {
        this.title = title;
        this.type = type;
        this.category = category;
        this.priority = priority;
        this.deadline = deadline;
    }

    public String getSummary() {
        String s = type + " · " + category;
        if (type.equals("Task")) {
            s += " · " + priority + " priority";
        }
        if (!deadline.isEmpty()) {
            s += " · " + deadline;
        }
        return s;
    }
}
