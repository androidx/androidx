package com.android.flatfoot.apireviewdemo.db_01_basic;


import com.android.support.room.Database;
import com.android.support.room.RoomDatabase;

@Database(entities = User.class)
public abstract class BasicDatabase extends RoomDatabase {

}
