package com.fatima.unsaid;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Thought {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String text;
    public long createdMillis;
    public int itemCount;

    public Thought(String text, long createdMillis, int itemCount) {
        this.text = text;
        this.createdMillis = createdMillis;
        this.itemCount = itemCount;
    }
}