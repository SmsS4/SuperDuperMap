package com.example.superdupermap.setttings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.superdupermap.R;
import com.example.superdupermap.database.AppDatabase;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.example.superdupermap.MainActivity.RESULT_CODE_GOTO_BOOKMARK;
import static com.example.superdupermap.MainActivity.RESULT_CODE_UPDATE_THEME;


public class SettingsActivity extends AppCompatActivity {

    ThreadPoolExecutor threadPoolExecutor;
    private AppDatabase db;

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

                    } else {
                        threadPoolExecutor.execute(() -> db.configDao().setDarkMode(false));
                        ConfigStorage.darkMode = false;
                    }
                    recreate();
                }
        );

    }

    public void pre() {
        if (ConfigStorage.darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        setContentView(R.layout.activity_settings2);
        Activity from = SettingsActivity.this;
        findViewById(R.id.bookmark_nav).setOnClickListener(v -> {
            setResult(RESULT_CODE_GOTO_BOOKMARK);
            finish();
        });

        findViewById(R.id.map_nav).setOnClickListener(v -> {
            setResult(RESULT_CODE_UPDATE_THEME);
            finish();
        });

        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        ((ImageView) findViewById(R.id.setting_nav)).setColorFilter(filter);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CODE_UPDATE_THEME);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pre();
        initFields();
        setClearStorage();
        activitySetDarkMode();
    }
}