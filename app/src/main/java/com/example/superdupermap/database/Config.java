package com.example.superdupermap.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Config {

    public Config(boolean dark_mode){
        this.name = "main_config";
        this.dark_mode = dark_mode;
    }

    @PrimaryKey()
    @NonNull()
    public String name;

    public boolean dark_mode;
}
