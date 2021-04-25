package com.example.superdupermap.database;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Transaction;

@Entity
public class Bookmark {

    public Bookmark(String name, double x, double y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }


    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;

    public double x;

    public double y;
}
