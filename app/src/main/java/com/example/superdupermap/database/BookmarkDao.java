package com.example.superdupermap.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BookmarkDao {
    @Query("SELECT * FROM bookmark")
    List<Bookmark> getAll();

    @Insert
    void insert(Bookmark... bookmarks);

    @Delete
    void delete(Bookmark bookmark);

    @Query("SELECT * FROM bookmark WHERE UPPER(name) LIKE UPPER(:name)")
    List<Bookmark> search(String name);

    @Query("DELETE FROM bookmark")
    void clear();
}
