package com.example.superdupermap.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ConfigDao {

    @Query("UPDATE config SET dark_mode=:dark_mode WHERE name='main_config'")
    void setDarkMode(boolean dark_mode);

    @Query("SELECT dark_mode FROM config WHERE name='main_config'")
    boolean getDarkMode();

    @Query("SELECT * FROM config")
    List<Config> getAll();

    @Insert
    void insert(Config config);
}
