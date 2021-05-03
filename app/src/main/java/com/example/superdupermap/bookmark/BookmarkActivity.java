package com.example.superdupermap.bookmark;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.superdupermap.MainActivity;
import com.example.superdupermap.MessageWhat;
import com.example.superdupermap.R;
import com.example.superdupermap.database.AppDatabase;
import com.example.superdupermap.database.Bookmark;
import com.example.superdupermap.setttings.ConfigStorage;
import com.example.superdupermap.setttings.SettingsActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BookmarkActivity extends AppCompatActivity {

    ThreadPoolExecutor threadPoolExecutor;
    private Handler handler;
    private AppDatabase db;

    private TextInputEditText searchbar;
    private RecyclerView recyclerView;

    public List<Bookmark> bookmarks = new ArrayList<>();

    private static class UpdateListHandler extends Handler {
        private final WeakReference<BookmarkActivity> target;

        UpdateListHandler(Looper looper, BookmarkActivity target) {
            super(looper);
            this.target = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            BookmarkActivity activity = this.target.get();
            if (msg.what == MessageWhat.LoadBookMarks) {
                activity.bookmarks.clear();
                activity.bookmarks.addAll((List<Bookmark>) msg.obj);
                System.out.println("load enghadr bookmarks:");
                System.out.println(activity.bookmarks.size());
                activity.recyclerView.getAdapter().notifyDataSetChanged();

            }
        }
    }


    private void setFields() {
        db = AppDatabase.getDatabase(getApplicationContext());
        recyclerView = findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(new BookmarkAdapter(bookmarks, this));

        searchbar = findViewById(R.id.searchbar);

        searchbar.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                threadPoolExecutor.execute(new BookmarkLoader(handler, db, "%" + searchbar.getText() + "%"));

            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });

    }


    public void deleteBookmark(Bookmark bookmark) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Bookmark")
                .setMessage("Remove " + bookmark.name + " ?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        Toast.makeText(BookmarkActivity.this, "Bookmark removed", Toast.LENGTH_SHORT).show();
                        bookmarks.remove(bookmark);
                        recyclerView.getAdapter().notifyDataSetChanged();
                        threadPoolExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                db.bookmarkDao().delete(bookmark);
                            }
                        });
                    }
                }).setNegativeButton(android.R.string.no, null).show();


    }

    private void initThreadPool() {
        handler = new UpdateListHandler(Looper.getMainLooper(), this);
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        threadPoolExecutor = new ThreadPoolExecutor(
                0, 2, 15, TimeUnit.MINUTES, queue
        );
    }

    public void pre() {
        System.out.println("Pre called");
        if (ConfigStorage.darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        setContentView(R.layout.activity_bookmark);

        Activity from = BookmarkActivity.this;

        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        ((ImageView) findViewById(R.id.bookmark_nav)).setColorFilter(filter);

        findViewById(R.id.map_nav).setOnClickListener(v -> {
            finish();
            startActivity(new Intent(from, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        });

        findViewById(R.id.setting_nav).setOnClickListener(v -> {
            finish();
            startActivity(new Intent(from, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pre();
        setFields();
        initThreadPool();

//        /// this is how you add a bookmark
//        threadPoolExecutor.execute(() -> db.bookmarkDao().insert(new Bookmark("test", 1.2, 4.5)));
//
//        threadPoolExecutor.execute(() -> {
//            System.out.println("insering ke...");
//            db.bookmarkDao().insert(new Bookmark("test2", 1.2, 4.5));
//        });

        /// load bookmarks
        threadPoolExecutor.execute(new BookmarkLoader(handler, db, null));


    }
}