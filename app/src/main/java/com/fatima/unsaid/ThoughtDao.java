package com.fatima.unsaid;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ThoughtDao {
    @Insert
    void insert(Thought thought);

    @Query("SELECT * FROM Thought ORDER BY id DESC")
    List<Thought> getAll();

    @Delete
    void delete(Thought thought);
}
