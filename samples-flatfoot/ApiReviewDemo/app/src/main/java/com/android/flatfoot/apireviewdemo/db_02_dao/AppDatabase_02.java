package com.android.flatfoot.apireviewdemo.db_02_dao;

import com.android.flatfoot.apireviewdemo.db_01_basic.User;
import com.android.support.room.Database;
import com.android.support.room.RoomDatabase;

@Database(entities = User.class)
public abstract class AppDatabase_02 extends RoomDatabase {
    public abstract UserCrudDao userDao();
}
