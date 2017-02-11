package com.android.flatfoot.apireviewdemo.db_04_pojo;

import com.android.flatfoot.apireviewdemo.db_01_basic.User;
import com.android.flatfoot.apireviewdemo.db_02_dao.UserCrudDao;
import com.android.flatfoot.apireviewdemo.db_03_entity.Pet;
import com.android.flatfoot.apireviewdemo.db_03_entity.PetDao;
import com.android.support.room.Database;
import com.android.support.room.RoomDatabase;

@Database(entities = {User.class, Pet.class})
public abstract class AppDatabase_04 extends RoomDatabase {
    public abstract UserPetDao userPetDao();
}
