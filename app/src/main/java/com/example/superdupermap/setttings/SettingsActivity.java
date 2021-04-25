package com.example.superdupermap.setttings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;


import com.example.superdupermap.R;
import com.example.superdupermap.database.AppDatabase;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class SettingsActivity extends AppCompatActivity {

    private AppDatabase db;
    private Button dark_mode;
    ThreadPoolExecutor threadPoolExecutor;
    private static Object lock = new Object();
    private boolean itsMe = false;

    public void initFields() {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        threadPoolExecutor = new ThreadPoolExecutor(
                0, 2, 15, TimeUnit.MINUTES, queue
        );
        db = AppDatabase.getDatabase(getApplicationContext());
    }

    public void setClearStorage() {
        findViewById(R.id.clear_storage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Clear Storage")
                        .setMessage("Are you sure?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                Toast.makeText(v.getContext(), "Storage Cleaned", Toast.LENGTH_SHORT).show();
                                threadPoolExecutor.execute(() -> db.bookmarkDao().clear());

                            }
                        }).setNegativeButton(android.R.string.no, null).show();
            }
        });
    }

    public void activitySetDarkMode() {
        Switch swtch = findViewById(R.id.switch1);

        if (ConfigStorage.darkMode) {
            swtch.setChecked(true);
        }
        swtch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (!ConfigStorage.darkMode) {
                        threadPoolExecutor.execute(() -> db.configDao().setDarkMode(true));
                        ConfigStorage.darkMode = true;
                        finish();
                        startActivity(getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));

                    } else {
                        threadPoolExecutor.execute(() -> db.configDao().setDarkMode(false));
                        ConfigStorage.darkMode = false;
                        finish();
                        startActivity(getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                    }
                }
        );

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ConfigStorage.darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        setContentView(R.layout.activity_settings2);
        initFields();
        setClearStorage();
        activitySetDarkMode();


    }
}