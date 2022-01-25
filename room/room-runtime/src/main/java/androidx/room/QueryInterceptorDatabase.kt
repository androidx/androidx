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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteTransactionListener;
import android.os.Build;
import android.os.CancellationSignal;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteStatement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;


/**
 * Implements {@link SupportSQLiteDatabase} for SQLite queries.
 */
final class QueryInterceptorDatabase implements SupportSQLiteDatabase {

    private final SupportSQLiteDatabase mDelegate;
    private final RoomDatabase.QueryCallback mQueryCallback;
    private final Executor mQueryCallbackExecutor;

    QueryInterceptorDatabase(@NonNull SupportSQLiteDatabase supportSQLiteDatabase,
            @NonNull RoomDatabase.QueryCallback queryCallback, @NonNull Executor
            queryCallbackExecutor) {
        mDelegate = supportSQLiteDatabase;
        mQueryCallback = queryCallback;
        mQueryCallbackExecutor = queryCallbackExecutor;
    }

    @NonNull
    @Override
    public SupportSQLiteStatement compileStatement(@NonNull String sql) {
        return new QueryInterceptorStatement(mDelegate.compileStatement(sql),
                mQueryCallback, sql, mQueryCallbackExecutor);
    }

    @Override
    public void beginTransaction() {
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery("BEGIN EXCLUSIVE TRANSACTION",
                Collections.emptyList()));
        mDelegate.beginTransaction();
    }

    @Override
    public void beginTransactionNonExclusive() {
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery("BEGIN DEFERRED TRANSACTION",
                Collections.emptyList()));
        mDelegate.beginTransactionNonExclusive();
    }

    @Override
    public void beginTransactionWithListener(@NonNull SQLiteTransactionListener
            transactionListener) {
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery("BEGIN EXCLUSIVE TRANSACTION",
                Collections.emptyList()));
        mDelegate.beginTransactionWithListener(transactionListener);
    }

    @Override
    public void beginTransactionWithListenerNonExclusive(
            @NonNull SQLiteTransactionListener transactionListener) {
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery("BEGIN DEFERRED TRANSACTION",
                Collections.emptyList()));
        mDelegate.beginTransactionWithListenerNonExclusive(transactionListener);
    }

    @Override
    public void endTransaction() {
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery("END TRANSACTION",
                Collections.emptyList()));
        mDelegate.endTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery("TRANSACTION SUCCESSFUL",
                Collections.emptyList()));
        mDelegate.setTransactionSuccessful();
    }

    @Override
    public boolean inTransaction() {
        return mDelegate.inTransaction();
    }

    @Override
    public boolean isDbLockedByCurrentThread() {
        return mDelegate.isDbLockedByCurrentThread();
    }

    @Override
    public boolean yieldIfContendedSafely() {
        return mDelegate.yieldIfContendedSafely();
    }

    @Override
    public boolean yieldIfContendedSafely(long sleepAfterYieldDelay) {
        return mDelegate.yieldIfContendedSafely(sleepAfterYieldDelay);
    }

    @Override
    public int getVersion() {
        return mDelegate.getVersion();
    }

    @Override
    public void setVersion(int version) {
        mDelegate.setVersion(version);
    }

    @Override
    public long getMaximumSize() {
        return mDelegate.getMaximumSize();
    }

    @Override
    public long setMaximumSize(long numBytes) {
        return mDelegate.setMaximumSize(numBytes);
    }

    @Override
    public long getPageSize() {
        return mDelegate.getPageSize();
    }

    @Override
    public void setPageSize(long numBytes) {
        mDelegate.setPageSize(numBytes);
    }

    @NonNull
    @Override
    public Cursor query(@NonNull String query) {
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery(query,
                Collections.emptyList()));
        return mDelegate.query(query);
    }

    @NonNull
    @Override
    public Cursor query(@NonNull String query, @NonNull Object[] bindArgs) {
        List<Object> inputArguments = new ArrayList<>();
        inputArguments.addAll(Arrays.asList(bindArgs));
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery(query,
                inputArguments));
        return mDelegate.query(query, bindArgs);
    }

    @NonNull
    @Override
    public Cursor query(@NonNull SupportSQLiteQuery query) {
        QueryInterceptorProgram queryInterceptorProgram = new QueryInterceptorProgram();
        query.bindTo(queryInterceptorProgram);
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery(query.getSql(),
                queryInterceptorProgram.getBindArgs()));
        return mDelegate.query(query);
    }

    @NonNull
    @Override
    public Cursor query(@NonNull SupportSQLiteQuery query,
            @NonNull CancellationSignal cancellationSignal) {
        QueryInterceptorProgram queryInterceptorProgram = new QueryInterceptorProgram();
        query.bindTo(queryInterceptorProgram);
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery(query.getSql(),
                queryInterceptorProgram.getBindArgs()));
        return mDelegate.query(query);
    }

    @Override
    public long insert(@NonNull String table, int conflictAlgorithm, @NonNull ContentValues values)
            throws SQLException {
        return mDelegate.insert(table, conflictAlgorithm, values);
    }

    @Override
    public int delete(@NonNull String table, @NonNull String whereClause,
            @NonNull Object[] whereArgs) {
        return mDelegate.delete(table, whereClause, whereArgs);
    }

    @Override
    public int update(@NonNull String table, int conflictAlgorithm, @NonNull ContentValues values,
            @NonNull String whereClause,
            @NonNull Object[] whereArgs) {
        return mDelegate.update(table, conflictAlgorithm, values, whereClause,
                whereArgs);
    }

    @Override
    public void execSQL(@NonNull String sql) throws SQLException {
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery(sql, new ArrayList<>(0)));
        mDelegate.execSQL(sql);
    }

    @Override
    public void execSQL(@NonNull String sql, @NonNull Object[] bindArgs) throws SQLException {
        List<Object> inputArguments = new ArrayList<>();
        inputArguments.addAll(Arrays.asList(bindArgs));
        mQueryCallbackExecutor.execute(() -> mQueryCallback.onQuery(sql, inputArguments));
        mDelegate.execSQL(sql, inputArguments.toArray());
    }

    @Override
    public boolean isReadOnly() {
        return mDelegate.isReadOnly();
    }

    @Override
    public boolean isOpen() {
        return mDelegate.isOpen();
    }

    @Override
    public boolean needUpgrade(int newVersion) {
        return mDelegate.needUpgrade(newVersion);
    }

    @NonNull
    @Override
    public String getPath() {
        return mDelegate.getPath();
    }

    @Override
    public void setLocale(@NonNull Locale locale) {
        mDelegate.setLocale(locale);
    }

    @Override
    public void setMaxSqlCacheSize(int cacheSize) {
        mDelegate.setMaxSqlCacheSize(cacheSize);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void setForeignKeyConstraintsEnabled(boolean enable) {
        mDelegate.setForeignKeyConstraintsEnabled(enable);
    }

    @Override
    public boolean enableWriteAheadLogging() {
        return mDelegate.enableWriteAheadLogging();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void disableWriteAheadLogging() {
        mDelegate.disableWriteAheadLogging();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean isWriteAheadLoggingEnabled() {
        return mDelegate.isWriteAheadLoggingEnabled();
    }

    @NonNull
    @Override
    public List<Pair<String, String>> getAttachedDbs() {
        return mDelegate.getAttachedDbs();
    }

    @Override
    public boolean isDatabaseIntegrityOk() {
        return mDelegate.isDatabaseIntegrityOk();
    }

    @Override
    public void close() throws IOException {
        mDelegate.close();
    }
}
