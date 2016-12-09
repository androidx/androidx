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
import com.android.support.db.SupportSQLiteOpenHelper;
import com.android.support.db.SupportSQLiteStatement;

/**
 * Base class for all Room databases.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class RoomDatabase {
    private volatile SupportSQLiteDatabase mDatabase;
    private final SupportSQLiteOpenHelper mOpenHelper;

    /**
     * Creates a RoomDatabase with the given configuration.
     *
     * @param configuration The configuration to setup the database.
     */
    public RoomDatabase(DatabaseConfiguration configuration) {
        mOpenHelper = createOpenHelper(configuration);
    }

    /**
     * Returns the SQLite open helper used by this database.
     *
     * @return The SQLite open helper used by this database.
     */
    public SupportSQLiteOpenHelper getOpenHelper() {
        return mOpenHelper;
    }

    /**
     * Creates the open helper to access the database. Generated class already implements this
     * method.
     * Note that this method is called when the RoomDatabase is initialized.
     *
     * @param config The configuration of the Room database.
     *
     * @return A new SupportSQLiteOpenHelper to be used while connecting to the database.
     */
    protected abstract SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration config);

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
        return mOpenHelper.getWritableDatabase().rawQuery(sql, selectionArgs);
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#compileStatement(String)}.
     *
     * @param sql The query to compile.
     *
     * @return The compiled query.
     */
    public SupportSQLiteStatement compileStatement(String sql) {
        return mOpenHelper.getWritableDatabase().compileStatement(sql);
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#beginTransaction()}.
     */
    public void beginTransaction() {
        mOpenHelper.getWritableDatabase().beginTransaction();
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#endTransaction()}.
     */
    public void endTransaction() {
        mOpenHelper.getWritableDatabase().endTransaction();
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#setTransactionSuccessful()}.
     */
    public void setTransactionSuccessful() {
        mOpenHelper.getWritableDatabase().setTransactionSuccessful();
    }
}
