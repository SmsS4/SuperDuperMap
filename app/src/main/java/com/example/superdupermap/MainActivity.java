package com.example.superdupermap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.content.Intent;
import android.os.Bundle;

import com.example.superdupermap.database.AppDatabase;
import com.example.superdupermap.database.Bookmark;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(MainActivity.this, BookmarkActivity.class);
//        intent.putExtra("id", coin);
        startActivity(intent);


    }
}