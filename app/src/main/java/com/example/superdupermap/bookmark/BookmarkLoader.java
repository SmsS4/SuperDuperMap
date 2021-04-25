package com.example.superdupermap.bookmark;

import android.os.Handler;
import android.os.MemoryFile;
import android.os.Message;

import com.example.superdupermap.MessageWhat;
import com.example.superdupermap.database.AppDatabase;
import com.example.superdupermap.database.Bookmark;

public class BookmarkLoader implements Runnable {
    /*
     * this class loads all bookmark from db and pass them to handler
     * */
    private final Handler handler;
    private final AppDatabase db;
    private final String searchPattern;

    public BookmarkLoader(Handler handler, AppDatabase db, String searchPattern) {
        this.handler = handler;
        this.db = db;
        this.searchPattern = searchPattern;
    }

    @Override
    public void run() {
        Message message = new Message();
        message.what = MessageWhat.LoadBookMarks;
        if (searchPattern == null)
            message.obj = db.bookmarkDao().getAll();
        else {
            System.out.println("looking for");
            System.out.println(searchPattern);
            message.obj = db.bookmarkDao().search(searchPattern);
        }
        handler.sendMessage(message);
    }
}
