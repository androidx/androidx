package com.android.flatfoot.apireviewdemo.db_05_converters;

import com.android.support.room.Database;
import com.android.support.room.RoomDatabase;

@Database(entities = Game.class)
public abstract class AppDatabase_05 extends RoomDatabase {
    public abstract GameDao gameDao();
}
