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

package com.android.support.room.integration.testapp.migration;

import android.content.ContentValues;

import com.android.support.db.SupportSQLiteDatabase;
import com.android.support.room.Dao;
import com.android.support.room.Database;
import com.android.support.room.Entity;
import com.android.support.room.Insert;
import com.android.support.room.PrimaryKey;
import com.android.support.room.Query;
import com.android.support.room.RoomDatabase;

import java.util.List;

@SuppressWarnings("WeakerAccess")
@Database(version = MigrationDb.LATEST_VERSION,
        entities = {MigrationDb.Vo1.class, MigrationDb.Vo2.class})
public abstract class MigrationDb extends RoomDatabase {
    static final int LATEST_VERSION = 2;
    abstract MigrationDao dao();
    @Entity
    static class Vo1 {
        @PrimaryKey
        public int id;
        public String name;
    }

    @Entity
    static class Vo2 {
        @PrimaryKey
        public int id;
        public String name;
    }

    @Dao
    interface MigrationDao {
        @Query("SELECT * from Vo1 ORDER BY id ASC")
        List<Vo1> loadAllVo1s();
        @Query("SELECT * from Vo2 ORDER BY id ASC")
        List<Vo1> loadAllVo2s();
        @Insert
        void insert(Vo2... vo2);
    }

    /**
     * not a real dao because database will change.
     */
    static class Dao_V1 {
        final SupportSQLiteDatabase mDb;

        Dao_V1(SupportSQLiteDatabase db) {
            mDb = db;
        }

        public void insertIntoVo1(int id, String name) {
            ContentValues values = new ContentValues();
            values.put("id", id);
            values.put("name", name);
            long insertionId = mDb.insert("Vo1", null, values);
            if (insertionId == -1) {
                throw new RuntimeException("test sanity failure");
            }
        }
    }
}
