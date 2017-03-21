package com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.db;

import android.content.Context;

import com.android.support.room.Database;
import com.android.support.room.Room;
import com.android.support.room.RoomDatabase;
import com.android.support.room.TypeConverters;

import com.example.android.flatfoot.codelab.flatfootcodelab.orm_db.DateConverter;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.dao.CommentDao;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.dao.ProductDao;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.entity.MyComment;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.entity.MyProduct;

@Database(entities = {MyProduct.class, MyComment.class}, version = 1)
@TypeConverters(DateConverter.class)
public abstract class MyDatabase extends RoomDatabase {
    private static MyDatabase sInstance;
    private static final Object LOCK = new Object();
    public static MyDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (LOCK) {
                if (sInstance == null) {
                    sInstance = Room.databaseBuilder(context.getApplicationContext(),
                            MyDatabase.class, "step10-solution-db").build();
                }
            }
        }
        return sInstance;
    }
    public abstract ProductDao productDao();
    public abstract CommentDao commentDao();
}
