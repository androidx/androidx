package com.android.flatfoot.apireviewdemo.exercise.db;

import android.content.Context;

import com.android.flatfoot.apireviewdemo.DemoApplication;
import com.android.support.room.Room;
import com.android.support.room.RoomDatabase;

//@Database(entities = Note.class)
abstract public class NoteDatabase extends RoomDatabase {

    private static NoteDatabase sInstance;

    public abstract NoteDao getNoteDao();

    /**
     * Gets a database instance.
     */
    public static synchronized NoteDatabase getDatabase() {
        if (sInstance == null) {
            Context context = DemoApplication.context();
            sInstance = Room.databaseBuilder(context, NoteDatabase.class, "notes.db").build();
        }
        return sInstance;
    }
}
