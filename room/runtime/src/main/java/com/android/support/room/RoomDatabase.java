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
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class RoomDatabase {
    private final SupportSQLiteDatabase mDb;

    public RoomDatabase(SupportSQLiteDatabase supportDb) {
        mDb = supportDb;
    }
    // Below, there are wrapper methods for SupportSQLiteDatabase. This helps us track which
    // methods we are using and also helps unit tests to mock this class without mocking
    // all sqlite database methods.
    /**
     * Wrapper for {@link SupportSQLiteDatabase#rawQuery(String, String[])}.
     *
     * @param sql Sql query to run.
     * @param selectionArgs Selection arguments.
     *
     * @return Result of the query.
     */
    public Cursor query(String sql, String[] selectionArgs) {
        return mDb.rawQuery(sql, selectionArgs);
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#compileStatement(String)}.
     *
     * @param sql The query to compile.
     *
     * @return The compiled query.
     */
    public SupportSQLiteStatement compileStatement(String sql) {
        return mDb.compileStatement(sql);
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#beginTransaction()}.
     */
    public void beginTransaction() {
        mDb.beginTransaction();
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#endTransaction()}.
     */
    public void endTransaction() {
        mDb.endTransaction();
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#setTransactionSuccessful()}.
     */
    public void setTransactionSuccessful() {
        mDb.setTransactionSuccessful();
    }
}
