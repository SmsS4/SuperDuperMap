package com.example.superdupermap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.example.superdupermap.database.AppDatabase;
import com.example.superdupermap.database.Bookmark;

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

    private ProgressBar progressBar;
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
                activity.hideProgressBar();
                activity.bookmarks.addAll((List<Bookmark>)msg.obj);
            }
//            MainActivity target = this.target.get();
//            if (target == null)
//                return;
//            if (msg.what == MESSAGE_NETWORK_ERROR) {
//                target.requestDone(false);
//            } else {
//                UpdateCoinsListObj obj = (UpdateCoinsListObj) msg.obj;
//                target.updateList(obj.getStart() - 1, obj.getCoins());
//                if (obj.isFresh())
//                    target.requestDone(true);
//            }
        }
    }


    private void setFields() {
        db = AppDatabase.getDatabase(getApplicationContext());
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager coinsListLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(coinsListLayoutManager);
        recyclerView.setAdapter(new BookmarkAdapter(bookmarks));

    }

    public void hideProgressBar() {
        this.progressBar.setVisibility(View.INVISIBLE);
    }

    private void initThreaqdPool() {
        handler = new UpdateListHandler(Looper.getMainLooper(), this);
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        threadPoolExecutor = new ThreadPoolExecutor(
                0, 2, 15, TimeUnit.MINUTES, queue
        );
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmark);
        setFields();
        initThreaqdPool();

        /// load bookmarks
        threadPoolExecutor.execute(new BookmarkLoader(handler, db));
    }
}