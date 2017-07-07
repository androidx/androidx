/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.flatfoot.apireviewdemo.db_01_basic;


import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.database.Cursor;

import timber.log.Timber;

public class Usage {
    BasicDatabase mBasicDatabase;

    public Usage(Context context) {
        mBasicDatabase = Room.inMemoryDatabaseBuilder(context.getApplicationContext(),
                BasicDatabase.class).build();
    }

    public void directSQL() {
        SupportSQLiteDatabase db = mBasicDatabase.getOpenHelper().getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from User", new String[0]);
        try {
            while (cursor.moveToNext()) {
                Timber.d("user name: %s", cursor.getString(cursor.getColumnIndex("name")));
            }
        } finally {
            cursor.close();
        }
    }
}
