package com.android.flatfoot.apireviewdemo.db_03_entity;

import com.android.flatfoot.apireviewdemo.db_01_basic.User;
import com.android.flatfoot.apireviewdemo.db_02_dao.UserCrudDao;
import com.android.support.room.Database;
import com.android.support.room.RoomDatabase;

@Database(entities = {User.class, Pet.class})
public abstract class AppDatabase_03 extends RoomDatabase {
    public abstract UserCrudDao userDao();
    public abstract PetDao petDao();
}
