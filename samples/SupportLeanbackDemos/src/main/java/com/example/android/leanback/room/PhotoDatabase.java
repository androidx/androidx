/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.leanback.room;
import android.content.ContentValues;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.OnConflictStrategy;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.android.leanback.PhotoItem;
import com.example.android.leanback.R;
/**
 * PhotoDatabase
 */
@Database(entities = {Photo.class}, version = 1, exportSchema = false)
public abstract class PhotoDatabase extends RoomDatabase {
    private static PhotoDatabase sInstance;
    /**
     * Gets an instance of PhotoDatabase
     * @param context
     * @return
     */
    public static synchronized PhotoDatabase getInstance(
            Context context) {
        if (sInstance == null) {
            sInstance = Room.inMemoryDatabaseBuilder(context, PhotoDatabase.class)
                    .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            ContentValues cv = new ContentValues();
                            for (int i = 0; i < sData.length; ++i) {
                                PhotoItem dataItem = sData[i];
                                cv.clear();
                                cv.put("id", i);
                                cv.put("title", dataItem.getTitle());
                                cv.put("imgResourceId", dataItem.getImageResourceId());
                                db.insert("Photo", OnConflictStrategy.IGNORE, cv);
                            }
                        }
                    })
                    .fallbackToDestructiveMigration(false)
                    .build();
        }
        return sInstance;
    }
    private static PhotoItem[] sData = new PhotoItem[]{
            new PhotoItem("Hello world", R.drawable.gallery_photo_1),
            new PhotoItem("This is a test", R.drawable.gallery_photo_2),
            new PhotoItem("Android TV", R.drawable.gallery_photo_3),
            new PhotoItem("Leanback", R.drawable.gallery_photo_4),
            new PhotoItem("Hello world", R.drawable.gallery_photo_5),
            new PhotoItem("This is a test", R.drawable.gallery_photo_6),
            new PhotoItem("Android TV", R.drawable.gallery_photo_7),
            new PhotoItem("Leanback", R.drawable.gallery_photo_8),
            new PhotoItem("Hello world", R.drawable.gallery_photo_1),
            new PhotoItem("This is a test", R.drawable.gallery_photo_2),
            new PhotoItem("Android TV", R.drawable.gallery_photo_3),
            new PhotoItem("Leanback", R.drawable.gallery_photo_4),
            new PhotoItem("Hello world", R.drawable.gallery_photo_5),
            new PhotoItem("This is a test", R.drawable.gallery_photo_6),
            new PhotoItem("Android TV", R.drawable.gallery_photo_7),
            new PhotoItem("Leanback", R.drawable.gallery_photo_8)
    };
    /**
     * PhotoDao
     * @return
     */
    public abstract PhotoDao photoDao();
}
