package com.example.superdupermap.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BookmarkDao {
    @Query("SELECT * FROM bookmark")
    List<Bookmark> getAll();

    @Insert
    void insert(Bookmark... bookmarks);

}
