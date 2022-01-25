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

package androidx.room;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteStatement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Implements an instance of {@link SupportSQLiteStatement} for SQLite queries.
 */
final class QueryInterceptorStatement implements SupportSQLiteStatement {

    private final SupportSQLiteStatement mDelegate;
    private final RoomDatabase.QueryCallback mQueryCallback;
    private final String mSqlStatement;
    private final List<Object> mBindArgsCache = new ArrayList<>();
    private final Executor mQueryCallbackExecutor;

    QueryInterceptorStatement(@NonNull SupportSQLiteStatement compileStatement,
            @NonNull RoomDatabase.QueryCallback queryCallback, String sqlStatement,
            @NonNull Executor queryCallbackExecutor) {
        mDelegate = compileStatement;
        mQueryCallback = queryCallback;
        mSqlStatement = sqlStatement;
        mQueryCallbackExecutor = queryCallbackExecutor;
    }

    @Override
    public void execute() {
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery(mSqlStatement, mBindArgsCache));
        mDelegate.execute();
    }

    @Override
    public int executeUpdateDelete() {
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery(mSqlStatement, mBindArgsCache));
        return mDelegate.executeUpdateDelete();
    }

    @Override
    public long executeInsert() {
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery(mSqlStatement, mBindArgsCache));
        return mDelegate.executeInsert();
    }

    @Override
    public long simpleQueryForLong() {
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery(mSqlStatement, mBindArgsCache));
        return mDelegate.simpleQueryForLong();
    }

    @Override
    public String simpleQueryForString() {
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery(mSqlStatement, mBindArgsCache));
        return mDelegate.simpleQueryForString();
    }

    @Override
    public void bindNull(int index) {
        saveArgsToCache(index, mBindArgsCache.toArray());
        mDelegate.bindNull(index);
    }

    @Override
    public void bindLong(int index, long value) {
        saveArgsToCache(index, value);
        mDelegate.bindLong(index, value);
    }

    @Override
    public void bindDouble(int index, double value) {
        saveArgsToCache(index, value);
        mDelegate.bindDouble(index, value);
    }

    @Override
    public void bindString(int index, String value) {
        saveArgsToCache(index, value);
        mDelegate.bindString(index, value);
    }

    @Override
    public void bindBlob(int index, byte[] value) {
        saveArgsToCache(index, value);
        mDelegate.bindBlob(index, value);
    }

    @Override
    public void clearBindings() {
        mBindArgsCache.clear();
        mDelegate.clearBindings();
    }

    @Override
    public void close() throws IOException {
        mDelegate.close();
    }

    private void saveArgsToCache(int bindIndex, Object value) {
        int index = bindIndex - 1;
        if (index >= mBindArgsCache.size()) {
            // Add null entries to the list until we have the desired # of indices
            for (int i = mBindArgsCache.size(); i <= index; i++) {
                mBindArgsCache.add(null);
            }
        }
        mBindArgsCache.set(index, value);
    }
}
