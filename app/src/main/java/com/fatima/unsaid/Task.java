package com.fatima.unsaid;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Task {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String type;      // Task, Event or Note
    public String category;
    public String priority;  // High, Medium or Low
    public String deadline;  // can be empty
    public boolean done;
    public long dueMillis;   // exact due date, 0 if none
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
