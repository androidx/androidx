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

import com.android.support.db.SupportDb;
import com.android.support.db.SupportSqliteStatement;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all Room databases.
 */
@SuppressWarnings("unused")
public abstract class RoomDatabase implements SupportDb {
    private ThreadLocal<Map<String, SupportSqliteStatement>> mStatementCache;
    private final SupportDb mDb;

    public RoomDatabase(SupportDb supportDb) {
        mDb = supportDb;
        mStatementCache = new ThreadLocal<>();
    }

    @Override
    public Cursor query(String sql, String[] args) {
        return mDb.query(sql, args);
    }

    @Override
    public SupportSqliteStatement compileStatement(String sql) {
        Map<String, SupportSqliteStatement> cache = mStatementCache.get();
        if (cache == null) {
            cache = new HashMap<>();
            mStatementCache.set(cache);
        }
        SupportSqliteStatement cached = cache.get(sql);
        if (cached == null) {
            cached = mDb.compileStatement(sql);
            cache.put(sql, cached);
        }
        return cached;
    }
}
