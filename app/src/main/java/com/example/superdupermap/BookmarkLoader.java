package com.example.superdupermap;

import android.os.Handler;
import android.os.MemoryFile;
import android.os.Message;

import com.example.superdupermap.database.AppDatabase;
import com.example.superdupermap.database.Bookmark;

public class BookmarkLoader implements Runnable {
    /*
    * this class loads all bookmark from db and pass them to handler
    * */
    private final Handler handler;
    private final AppDatabase db;
    public BookmarkLoader(Handler handler, AppDatabase db) {
        this.handler = handler;
        this.db = db;
    }

    @Override
    public void run() {
        Message message = new Message();
        message.what = MessageWhat.LoadBookMarks;
        message.obj = db.bookmarkDao().getAll();
        handler.sendMessage(message);


//        Bookmark bookmark = new Bookmark("test2", 14214.2, 24234.5);
//        db.bookmarkDao().insert(bookmark);
//
//        Bookmark bookmark2 = new Bookmark("test3", 14252345214.2, 24234322.55);
//        db.bookmarkDao().insert(bookmark2);
//        Bookmark bookmark3 = new Bookmark("test4", 134214.2, 24234.532545);
//        db.bookmarkDao().insert(bookmark3);
//        System.out.println("Selecting");
//        for(Bookmark bm: db.bookmarkDao().getAll()){
//            System.out.println(bm.name);
//            System.out.println(bm.x);
//            System.out.println(bm.y);
//        }
//        System.out.println(db.bookmarkDao().getAll().size());
    }
}
