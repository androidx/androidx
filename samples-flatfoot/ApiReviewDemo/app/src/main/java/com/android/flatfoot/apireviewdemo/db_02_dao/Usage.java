package com.android.flatfoot.apireviewdemo.db_02_dao;

import android.content.Context;

import com.android.flatfoot.apireviewdemo.db_01_basic.User;
import com.android.support.room.Room;

import java.util.List;

import timber.log.Timber;

public class Usage {
    AppDatabase_02 mBasicDatabase;
    public Usage(Context context) {
        mBasicDatabase = Room.inMemoryDatabaseBuilder(context.getApplicationContext(),
                AppDatabase_02.class).build();
    }

    public void loadAllUsers() {
        List<User> allUsers = mBasicDatabase.userDao().loadAllUsers();
        for (User user : allUsers) {
            Timber.d("user name %s", user.name);
        }
    }
}
