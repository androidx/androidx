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

package com.android.support.db.framework;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteTransactionListener;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Pair;

import com.android.support.db.SupportSQLiteDatabase;
import com.android.support.db.SupportSQLiteStatement;

import java.util.List;
import java.util.Locale;

/**
 * Delegates all calls to an implementation of {@link SQLiteDatabase}.
 */
@SuppressWarnings("unused")
public class FrameworkSQLiteDatabase implements SupportSQLiteDatabase {
    private final SQLiteDatabase mDelegate;

    /**
     * Creates a wrapper around {@link SQLiteDatabase}.
     * @param delegate The delegate to receive all calls.
     */
    @SuppressWarnings("WeakerAccess")
    public FrameworkSQLiteDatabase(SQLiteDatabase delegate) {
        mDelegate = delegate;
    }

    @Override
    public SupportSQLiteStatement compileStatement(String sql) {
        return new FrameworkSQLiteStatement(mDelegate.compileStatement(sql));
    }

    @Override
    public void beginTransaction() {
        mDelegate.beginTransaction();
    }

    @Override
    public void beginTransactionNonExclusive() {
        mDelegate.beginTransactionNonExclusive();
    }

    @Override
    public void beginTransactionWithListener(SQLiteTransactionListener transactionListener) {
        mDelegate.beginTransactionWithListener(transactionListener);
    }

    @Override
    public void beginTransactionWithListenerNonExclusive(
            SQLiteTransactionListener transactionListener) {
        mDelegate.beginTransactionWithListenerNonExclusive(transactionListener);
    }

    @Override
    public void endTransaction() {
        mDelegate.endTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
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

    @Override
    public Cursor query(boolean distinct, String table, String[] columns, String selection,
            String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        return mDelegate.query(distinct, table, columns, selection, selectionArgs, groupBy,
                having, orderBy, limit);
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    public Cursor query(boolean distinct, String table, String[] columns, String selection,
            String[] selectionArgs, String groupBy, String having, String orderBy, String limit,
            CancellationSignal cancellationSignal) {
        return mDelegate.query(distinct, table, columns, selection, selectionArgs, groupBy,
                having, orderBy, limit, cancellationSignal);
    }

    @Override
    public Cursor queryWithFactory(SQLiteDatabase.CursorFactory cursorFactory, boolean distinct,
            String table, String[] columns, String selection, String[] selectionArgs,
            String groupBy, String having, String orderBy, String limit) {
        return mDelegate.queryWithFactory(cursorFactory, distinct, table, columns, selection,
                selectionArgs, groupBy, having, orderBy, limit);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public Cursor queryWithFactory(SQLiteDatabase.CursorFactory cursorFactory, boolean distinct,
            String table, String[] columns, String selection, String[] selectionArgs,
            String groupBy, String having, String orderBy, String limit,
            CancellationSignal cancellationSignal) {
        return mDelegate.queryWithFactory(cursorFactory, distinct, table, columns, selection,
                selectionArgs, groupBy, having, orderBy, limit, cancellationSignal);
    }

    @Override
    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
            String groupBy, String having, String orderBy) {
        return mDelegate.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
    }

    @Override
    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
            String groupBy, String having, String orderBy, String limit) {
        return mDelegate.query(table, columns, selection, selectionArgs, groupBy, having,
                orderBy, limit);
    }

    @Override
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        return mDelegate.rawQuery(sql, selectionArgs);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public Cursor rawQuery(String sql, String[] selectionArgs,
            CancellationSignal cancellationSignal) {
        return mDelegate.rawQuery(sql, selectionArgs, cancellationSignal);
    }

    @Override
    public Cursor rawQueryWithFactory(SQLiteDatabase.CursorFactory cursorFactory, String sql,
            String[] selectionArgs, String editTable) {
        return mDelegate.rawQueryWithFactory(cursorFactory, sql, selectionArgs, editTable);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public Cursor rawQueryWithFactory(SQLiteDatabase.CursorFactory cursorFactory, String sql,
            String[] selectionArgs, String editTable, CancellationSignal cancellationSignal) {
        return mDelegate.rawQueryWithFactory(cursorFactory, sql, selectionArgs, editTable,
                cancellationSignal);
    }

    @Override
    public long insert(String table, String nullColumnHack, ContentValues values) {
        return mDelegate.insert(table, nullColumnHack, values);
    }

    @Override
    public long insertOrThrow(String table, String nullColumnHack, ContentValues values)
            throws SQLException {
        return mDelegate.insertOrThrow(table, nullColumnHack, values);
    }

    @Override
    public long replace(String table, String nullColumnHack, ContentValues initialValues) {
        return mDelegate.replace(table, nullColumnHack, initialValues);
    }

    @Override
    public long replaceOrThrow(String table, String nullColumnHack, ContentValues initialValues)
            throws SQLException {
        return mDelegate.replaceOrThrow(table, nullColumnHack, initialValues);
    }

    @Override
    public long insertWithOnConflict(String table, String nullColumnHack,
            ContentValues initialValues, int conflictAlgorithm) {
        return mDelegate.insertWithOnConflict(table, nullColumnHack, initialValues,
                conflictAlgorithm);
    }

    @Override
    public int delete(String table, String whereClause, String[] whereArgs) {
        return mDelegate.delete(table, whereClause, whereArgs);
    }

    @Override
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return mDelegate.update(table, values, whereClause, whereArgs);
    }

    @Override
    public int updateWithOnConflict(String table, ContentValues values, String whereClause,
            String[] whereArgs, int conflictAlgorithm) {
        return mDelegate.updateWithOnConflict(table, values, whereClause, whereArgs,
                conflictAlgorithm);
    }

    @Override
    public void execSQL(String sql) throws SQLException {
        mDelegate.execSQL(sql);
    }

    @Override
    public void execSQL(String sql, Object[] bindArgs) throws SQLException {
        mDelegate.execSQL(sql, bindArgs);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void validateSql(@NonNull String sql, @Nullable CancellationSignal cancellationSignal) {
        mDelegate.validateSql(sql, cancellationSignal);
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

    @Override
    public String getPath() {
        return mDelegate.getPath();
    }

    @Override
    public void setLocale(Locale locale) {
        mDelegate.setLocale(locale);
    }

    @Override
    public void setMaxSqlCacheSize(int cacheSize) {
        mDelegate.setMaxSqlCacheSize(cacheSize);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void setForeignKeyConstraintsEnabled(boolean enable) {
        mDelegate.setForeignKeyConstraintsEnabled(enable);
    }

    @Override
    public boolean enableWriteAheadLogging() {
        return mDelegate.enableWriteAheadLogging();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void disableWriteAheadLogging() {
        mDelegate.disableWriteAheadLogging();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public boolean isWriteAheadLoggingEnabled() {
        return mDelegate.isWriteAheadLoggingEnabled();
    }

    @Override
    public List<Pair<String, String>> getAttachedDbs() {
        return mDelegate.getAttachedDbs();
    }

    @Override
    public boolean isDatabaseIntegrityOk() {
        return mDelegate.isDatabaseIntegrityOk();
    }
}
