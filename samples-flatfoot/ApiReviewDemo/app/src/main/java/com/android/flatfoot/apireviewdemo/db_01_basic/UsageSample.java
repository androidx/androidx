package com.android.flatfoot.apireviewdemo.db_01_basic;


import android.content.Context;
import android.database.Cursor;

import com.android.support.db.SupportSQLiteDatabase;
import com.android.support.room.Room;

import timber.log.Timber;

public class UsageSample {
    BasicDatabase mBasicDatabase;
    public UsageSample(Context context) {
        mBasicDatabase = Room.inMemoryDatabaseBuilder(context.getApplicationContext(),
                BasicDatabase.class).build();
    }

    public void directSQL() {
        SupportSQLiteDatabase db = mBasicDatabase.getOpenHelper().getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from User", new String[0]);
        try {
            while (cursor.moveToNext()) {
                Timber.d("user name: " + cursor.getString(cursor.getColumnIndex("name")));
            }
        } finally {
            cursor.close();
        }
    }
}
