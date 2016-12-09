/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.room;

import android.database.Cursor;

import com.android.support.db.SupportSQLiteDatabase;
import com.android.support.db.SupportSQLiteStatement;

/**
 * Base class for all Room databases.
 */
@SuppressWarnings("unused")
public abstract class RoomDatabase implements SupportSQLiteDatabase {
    private final SupportSQLiteDatabase mDb;

    public RoomDatabase(SupportSQLiteDatabase supportDb) {
        mDb = supportDb;
    }

    @Override
    public Cursor query(String sql, String[] args) {
        return mDb.query(sql, args);
    }

    @Override
    public SupportSQLiteStatement compileStatement(String sql) {
        return mDb.compileStatement(sql);
    }

    @Override
    public void beginTransaction() {
        mDb.beginTransaction();
    }

    @Override
    public void endTransaction() {
        mDb.endTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        mDb.setTransactionSuccessful();
    }
}
