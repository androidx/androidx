/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.SQLException;
import android.database.sqlite.SQLiteTransactionListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.arch.core.util.Function;
import androidx.room.util.SneakyThrow;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteStatement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A SupportSQLiteOpenHelper that has autoclose enabled for database connections.
 */
final class AutoClosingRoomOpenHelper implements SupportSQLiteOpenHelper, DelegatingOpenHelper {
    @NonNull
    private final SupportSQLiteOpenHelper mDelegateOpenHelper;

    @NonNull
    private final AutoClosingSupportSQLiteDatabase mAutoClosingDb;

    @NonNull
    private final AutoCloser mAutoCloser;

    AutoClosingRoomOpenHelper(@NonNull SupportSQLiteOpenHelper supportSQLiteOpenHelper,
            @NonNull AutoCloser autoCloser) {
        mDelegateOpenHelper = supportSQLiteOpenHelper;
        mAutoCloser = autoCloser;
        autoCloser.init(mDelegateOpenHelper);
        mAutoClosingDb = new AutoClosingSupportSQLiteDatabase(mAutoCloser);
    }

    @Nullable
    @Override
    public String getDatabaseName() {
        return mDelegateOpenHelper.getDatabaseName();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void setWriteAheadLoggingEnabled(boolean enabled) {
        mDelegateOpenHelper.setWriteAheadLoggingEnabled(enabled);
    }

    @NonNull
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public SupportSQLiteDatabase getWritableDatabase() {
        // Note we don't differentiate between writable db and readable db
        // We try to open the db so the open callbacks run
        mAutoClosingDb.pokeOpen();
        return mAutoClosingDb;
    }

    @NonNull
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public SupportSQLiteDatabase getReadableDatabase() {
        // Note we don't differentiate between writable db and readable db
        // We try to open the db so the open callbacks run
        mAutoClosingDb.pokeOpen();
        return mAutoClosingDb;
    }

    @Override
    public void close() {
        try {
            mAutoClosingDb.close();
        } catch (IOException e) {
            SneakyThrow.reThrow(e);
        }
    }

    /**
     * package protected to pass it to invalidation tracker...
     */
    @NonNull
    AutoCloser getAutoCloser() {
        return this.mAutoCloser;
    }

    @NonNull
    SupportSQLiteDatabase getAutoClosingDb() {
        return this.mAutoClosingDb;
    }

    @Override
    @NonNull
    public SupportSQLiteOpenHelper getDelegate() {
        return mDelegateOpenHelper;
    }

    /**
     * SupportSQLiteDatabase that also keeps refcounts and autocloses the database
     */
    static final class AutoClosingSupportSQLiteDatabase implements SupportSQLiteDatabase {
        @NonNull
        private final AutoCloser mAutoCloser;

        AutoClosingSupportSQLiteDatabase(@NonNull AutoCloser autoCloser) {
            mAutoCloser = autoCloser;
        }

        void pokeOpen() {
            mAutoCloser.executeRefCountingFunction(db -> null);
        }

        @Override
        public SupportSQLiteStatement compileStatement(String sql) {
            return new AutoClosingSupportSqliteStatement(sql, mAutoCloser);
        }

        @Override
        public void beginTransaction() {
            // We assume that after every successful beginTransaction() call there *must* be a
            // endTransaction() call.
            SupportSQLiteDatabase db = mAutoCloser.incrementCountAndEnsureDbIsOpen();
            try {
                db.beginTransaction();
            } catch (Throwable t) {
                // Note: we only want to decrement the ref count if the beginTransaction call
                // fails since there won't be a corresponding endTransaction call.
                mAutoCloser.decrementCountAndScheduleClose();
                throw t;
            }
        }

        @Override
        public void beginTransactionNonExclusive() {
            // We assume that after every successful beginTransaction() call there *must* be a
            // endTransaction() call.
            SupportSQLiteDatabase db = mAutoCloser.incrementCountAndEnsureDbIsOpen();
            try {
                db.beginTransactionNonExclusive();
            } catch (Throwable t) {
                // Note: we only want to decrement the ref count if the beginTransaction call
                // fails since there won't be a corresponding endTransaction call.
                mAutoCloser.decrementCountAndScheduleClose();
                throw t;
            }
        }

        @Override
        public void beginTransactionWithListener(SQLiteTransactionListener transactionListener) {
            // We assume that after every successful beginTransaction() call there *must* be a
            // endTransaction() call.
            SupportSQLiteDatabase db = mAutoCloser.incrementCountAndEnsureDbIsOpen();
            try {
                db.beginTransactionWithListener(transactionListener);
            } catch (Throwable t) {
                // Note: we only want to decrement the ref count if the beginTransaction call
                // fails since there won't be a corresponding endTransaction call.
                mAutoCloser.decrementCountAndScheduleClose();
                throw t;
            }
        }

        @Override
        public void beginTransactionWithListenerNonExclusive(
                SQLiteTransactionListener transactionListener) {
            // We assume that after every successful beginTransaction() call there *will* always
            // be a corresponding endTransaction() call. Without a corresponding
            // endTransactionCall we will never close the db.
            SupportSQLiteDatabase db = mAutoCloser.incrementCountAndEnsureDbIsOpen();
            try {
                db.beginTransactionWithListenerNonExclusive(transactionListener);
            } catch (Throwable t) {
                // Note: we only want to decrement the ref count if the beginTransaction call
                // fails since there won't be a corresponding endTransaction call.
                mAutoCloser.decrementCountAndScheduleClose();
                throw t;
            }
        }

        @Override
        public void endTransaction() {
            if (mAutoCloser.getDelegateDatabase() == null) {
                // This should never happen.
                throw new IllegalStateException("End transaction called but delegateDb is null");
            }

            try {
                mAutoCloser.getDelegateDatabase().endTransaction();
            } finally {
                mAutoCloser.decrementCountAndScheduleClose();
            }
        }

        @Override
        public void setTransactionSuccessful() {
            SupportSQLiteDatabase delegate = mAutoCloser.getDelegateDatabase();

            if (delegate == null) {
                // This should never happen.
                throw new IllegalStateException("setTransactionSuccessful called but delegateDb "
                        + "is null");
            }

            delegate.setTransactionSuccessful();
        }

        @Override
        public boolean inTransaction() {
            if (mAutoCloser.getDelegateDatabase() == null) {
                return false;
            }
            return mAutoCloser.executeRefCountingFunction(SupportSQLiteDatabase::inTransaction);
        }

        @Override
        public boolean isDbLockedByCurrentThread() {
            if (mAutoCloser.getDelegateDatabase() == null) {
                return false;
            }

            return mAutoCloser.executeRefCountingFunction(
                    SupportSQLiteDatabase::isDbLockedByCurrentThread);
        }

        @Override
        public boolean yieldIfContendedSafely() {
            return mAutoCloser.executeRefCountingFunction(
                    SupportSQLiteDatabase::yieldIfContendedSafely);
        }

        @Override
        public boolean yieldIfContendedSafely(long sleepAfterYieldDelay) {
            return mAutoCloser.executeRefCountingFunction(
                    SupportSQLiteDatabase::yieldIfContendedSafely);

        }

        @Override
        public int getVersion() {
            return mAutoCloser.executeRefCountingFunction(SupportSQLiteDatabase::getVersion);
        }

        @Override
        public void setVersion(int version) {
            mAutoCloser.executeRefCountingFunction(db -> {
                db.setVersion(version);
                return null;
            });
        }

        @Override
        public long getMaximumSize() {
            return mAutoCloser.executeRefCountingFunction(SupportSQLiteDatabase::getMaximumSize);
        }

        @Override
        public long setMaximumSize(long numBytes) {
            return mAutoCloser.executeRefCountingFunction(db -> db.setMaximumSize(numBytes));
        }

        @Override
        public long getPageSize() {
            return mAutoCloser.executeRefCountingFunction(SupportSQLiteDatabase::getPageSize);
        }

        @Override
        public void setPageSize(long numBytes) {
            mAutoCloser.executeRefCountingFunction(db -> {
                db.setPageSize(numBytes);
                return null;
            });
        }

        @Override
        public Cursor query(String query) {
            Cursor result;
            try {
                SupportSQLiteDatabase db = mAutoCloser.incrementCountAndEnsureDbIsOpen();
                result = db.query(query);
            } catch (Throwable throwable) {
                mAutoCloser.decrementCountAndScheduleClose();
                throw throwable;
            }

            return new KeepAliveCursor(result, mAutoCloser);
        }

        @Override
        public Cursor query(String query, Object[] bindArgs) {
            Cursor result;
            try {
                SupportSQLiteDatabase db = mAutoCloser.incrementCountAndEnsureDbIsOpen();
                result = db.query(query, bindArgs);
            } catch (Throwable throwable) {
                mAutoCloser.decrementCountAndScheduleClose();
                throw throwable;
            }

            return new KeepAliveCursor(result, mAutoCloser);
        }

        @Override
        public Cursor query(SupportSQLiteQuery query) {

            Cursor result;
            try {
                SupportSQLiteDatabase db = mAutoCloser.incrementCountAndEnsureDbIsOpen();
                result = db.query(query);
            } catch (Throwable throwable) {
                mAutoCloser.decrementCountAndScheduleClose();
                throw throwable;
            }

            return new KeepAliveCursor(result, mAutoCloser);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public Cursor query(SupportSQLiteQuery query, CancellationSignal cancellationSignal) {
            Cursor result;
            try {
                SupportSQLiteDatabase db = mAutoCloser.incrementCountAndEnsureDbIsOpen();
                result = db.query(query, cancellationSignal);
            } catch (Throwable throwable) {
                mAutoCloser.decrementCountAndScheduleClose();
                throw throwable;
            }

            return new KeepAliveCursor(result, mAutoCloser);
        }

        @Override
        public long insert(String table, int conflictAlgorithm, ContentValues values)
                throws SQLException {
            return mAutoCloser.executeRefCountingFunction(db -> db.insert(table, conflictAlgorithm,
                    values));
        }

        @Override
        public int delete(String table, String whereClause, Object[] whereArgs) {
            return mAutoCloser.executeRefCountingFunction(
                    db -> db.delete(table, whereClause, whereArgs));
        }

        @Override
        public int update(String table, int conflictAlgorithm, ContentValues values,
                String whereClause, Object[] whereArgs) {
            return mAutoCloser.executeRefCountingFunction(db -> db.update(table, conflictAlgorithm,
                    values, whereClause, whereArgs));
        }

        @Override
        public void execSQL(String sql) throws SQLException {
            mAutoCloser.executeRefCountingFunction(db -> {
                db.execSQL(sql);
                return null;
            });
        }

        @Override
        public void execSQL(String sql, Object[] bindArgs) throws SQLException {
            mAutoCloser.executeRefCountingFunction(db -> {
                db.execSQL(sql, bindArgs);
                return null;
            });
        }

        @Override
        public boolean isReadOnly() {
            return mAutoCloser.executeRefCountingFunction(SupportSQLiteDatabase::isReadOnly);
        }

        @Override
        public boolean isOpen() {
            // Get the db without incrementing the reference cause we don't want to open
            // the db for an isOpen call.
            SupportSQLiteDatabase localDelegate = mAutoCloser.getDelegateDatabase();

            if (localDelegate == null) {
                return false;
            }
            return localDelegate.isOpen();
        }

        @Override
        public boolean needUpgrade(int newVersion) {
            return mAutoCloser.executeRefCountingFunction(db -> db.needUpgrade(newVersion));
        }

        @Override
        public String getPath() {
            return mAutoCloser.executeRefCountingFunction(SupportSQLiteDatabase::getPath);
        }

        @Override
        public void setLocale(Locale locale) {
            mAutoCloser.executeRefCountingFunction(db -> {
                db.setLocale(locale);
                return null;
            });
        }

        @Override
        public void setMaxSqlCacheSize(int cacheSize) {
            mAutoCloser.executeRefCountingFunction(db -> {
                db.setMaxSqlCacheSize(cacheSize);
                return null;
            });
        }

        @SuppressLint("UnsafeNewApiCall")
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void setForeignKeyConstraintsEnabled(boolean enable) {
            mAutoCloser.executeRefCountingFunction(db -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    db.setForeignKeyConstraintsEnabled(enable);
                }
                return null;
            });
        }

        @Override
        public boolean enableWriteAheadLogging() {
            throw new UnsupportedOperationException("Enable/disable write ahead logging on the "
                    + "OpenHelper instead of on the database directly.");
        }

        @Override
        public void disableWriteAheadLogging() {
            throw new UnsupportedOperationException("Enable/disable write ahead logging on the "
                    + "OpenHelper instead of on the database directly.");
        }

        @SuppressLint("UnsafeNewApiCall")
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public boolean isWriteAheadLoggingEnabled() {
            return mAutoCloser.executeRefCountingFunction(db -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    return db.isWriteAheadLoggingEnabled();
                }
                return false;
            });
        }

        @Override
        public List<Pair<String, String>> getAttachedDbs() {
            return mAutoCloser.executeRefCountingFunction(SupportSQLiteDatabase::getAttachedDbs);
        }

        @Override
        public boolean isDatabaseIntegrityOk() {
            return mAutoCloser.executeRefCountingFunction(
                    SupportSQLiteDatabase::isDatabaseIntegrityOk);
        }

        @Override
        public void close() throws IOException {
            mAutoCloser.closeDatabaseIfOpen();
        }
    }

    /**
     * We need to keep the db alive until the cursor is closed, so we can't decrement our
     * reference count until the cursor is closed. The underlying database will not close until
     * this cursor is closed.
     */
    private static final class KeepAliveCursor implements Cursor {
        private final Cursor mDelegate;
        private final AutoCloser mAutoCloser;

        KeepAliveCursor(Cursor delegate, AutoCloser autoCloser) {
            mDelegate = delegate;
            mAutoCloser = autoCloser;
        }

        // close is the only important/changed method here:
        @Override
        public void close() {
            mDelegate.close();
            mAutoCloser.decrementCountAndScheduleClose();
        }

        @Override
        public boolean isClosed() {
            return mDelegate.isClosed();
        }


        @Override
        public int getCount() {
            return mDelegate.getCount();
        }

        @Override
        public int getPosition() {
            return mDelegate.getPosition();
        }

        @Override
        public boolean move(int offset) {
            return mDelegate.move(offset);
        }

        @Override
        public boolean moveToPosition(int position) {
            return mDelegate.moveToPosition(position);
        }

        @Override
        public boolean moveToFirst() {
            return mDelegate.moveToFirst();
        }

        @Override
        public boolean moveToLast() {
            return mDelegate.moveToLast();
        }

        @Override
        public boolean moveToNext() {
            return mDelegate.moveToNext();
        }

        @Override
        public boolean moveToPrevious() {
            return mDelegate.moveToPrevious();
        }

        @Override
        public boolean isFirst() {
            return mDelegate.isFirst();
        }

        @Override
        public boolean isLast() {
            return mDelegate.isLast();
        }

        @Override
        public boolean isBeforeFirst() {
            return mDelegate.isBeforeFirst();
        }

        @Override
        public boolean isAfterLast() {
            return mDelegate.isAfterLast();
        }

        @Override
        public int getColumnIndex(String columnName) {
            return mDelegate.getColumnIndex(columnName);
        }

        @Override
        public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
            return mDelegate.getColumnIndexOrThrow(columnName);
        }

        @Override
        public String getColumnName(int columnIndex) {
            return mDelegate.getColumnName(columnIndex);
        }

        @Override
        public String[] getColumnNames() {
            return mDelegate.getColumnNames();
        }

        @Override
        public int getColumnCount() {
            return mDelegate.getColumnCount();
        }

        @Override
        public byte[] getBlob(int columnIndex) {
            return mDelegate.getBlob(columnIndex);
        }

        @Override
        public String getString(int columnIndex) {
            return mDelegate.getString(columnIndex);
        }

        @Override
        public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
            mDelegate.copyStringToBuffer(columnIndex, buffer);
        }

        @Override
        public short getShort(int columnIndex) {
            return mDelegate.getShort(columnIndex);
        }

        @Override
        public int getInt(int columnIndex) {
            return mDelegate.getInt(columnIndex);
        }

        @Override
        public long getLong(int columnIndex) {
            return mDelegate.getLong(columnIndex);
        }

        @Override
        public float getFloat(int columnIndex) {
            return mDelegate.getFloat(columnIndex);
        }

        @Override
        public double getDouble(int columnIndex) {
            return mDelegate.getDouble(columnIndex);
        }

        @Override
        public int getType(int columnIndex) {
            return mDelegate.getType(columnIndex);
        }

        @Override
        public boolean isNull(int columnIndex) {
            return mDelegate.isNull(columnIndex);
        }

        /**
         * @deprecated see Cursor.deactivate
         */
        @Override
        @Deprecated
        public void deactivate() {
            mDelegate.deactivate();
        }

        /**
         * @deprecated see Cursor.requery
         */
        @Override
        @Deprecated
        public boolean requery() {
            return mDelegate.requery();
        }

        @Override
        public void registerContentObserver(ContentObserver observer) {
            mDelegate.registerContentObserver(observer);
        }

        @Override
        public void unregisterContentObserver(ContentObserver observer) {
            mDelegate.unregisterContentObserver(observer);
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            mDelegate.registerDataSetObserver(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            mDelegate.unregisterDataSetObserver(observer);
        }

        @Override
        public void setNotificationUri(ContentResolver cr, Uri uri) {
            mDelegate.setNotificationUri(cr, uri);
        }

        @SuppressLint("UnsafeNewApiCall")
        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public void setNotificationUris(@NonNull ContentResolver cr,
                @NonNull List<Uri> uris) {
            mDelegate.setNotificationUris(cr, uris);
        }

        @SuppressLint("UnsafeNewApiCall")
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public Uri getNotificationUri() {
            return mDelegate.getNotificationUri();
        }

        @SuppressLint("UnsafeNewApiCall")
        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Nullable
        @Override
        public List<Uri> getNotificationUris() {
            return mDelegate.getNotificationUris();
        }

        @Override
        public boolean getWantsAllOnMoveCalls() {
            return mDelegate.getWantsAllOnMoveCalls();
        }

        @SuppressLint("UnsafeNewApiCall")
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void setExtras(Bundle extras) {
            mDelegate.setExtras(extras);
        }

        @Override
        public Bundle getExtras() {
            return mDelegate.getExtras();
        }

        @Override
        public Bundle respond(Bundle extras) {
            return mDelegate.respond(extras);
        }
    }

    /**
     * We can't close our db if the SupportSqliteStatement is open.
     *
     * Each of these that are created need to be registered with RefCounter.
     *
     * On auto-close, RefCounter needs to close each of these before closing the db that these
     * were constructed from.
     *
     * Each of the methods here need to get
     */
    //TODO(rohitsat) cache the prepared statement... I'm not sure what the performance implications
    // are for the way it's done here, but caching the prepared statement would definitely be more
    // complicated since we need to invalidate any of the PreparedStatements that were created
    // with this db
    private static class AutoClosingSupportSqliteStatement implements SupportSQLiteStatement {
        private final String mSql;
        private final ArrayList<Object> mBinds = new ArrayList<>();
        private final AutoCloser mAutoCloser;

        AutoClosingSupportSqliteStatement(
                String sql, AutoCloser autoCloser) {
            mSql = sql;
            mAutoCloser = autoCloser;
        }

        private <T> T executeSqliteStatementWithRefCount(Function<SupportSQLiteStatement, T> func) {
            return mAutoCloser.executeRefCountingFunction(
                    db -> {
                        SupportSQLiteStatement statement = db.compileStatement(mSql);
                        doBinds(statement);
                        return func.apply(statement);
                    }
            );
        }

        private void doBinds(SupportSQLiteStatement supportSQLiteStatement) {
            // Replay the binds
            for (int i = 0; i < mBinds.size(); i++) {
                int bindIndex = i + 1; // Bind indices are 1 based so we start at 1 not 0
                Object bind = mBinds.get(i);
                if (bind == null) {
                    supportSQLiteStatement.bindNull(bindIndex);
                } else if (bind instanceof Long) {
                    supportSQLiteStatement.bindLong(bindIndex, (Long) bind);
                } else if (bind instanceof Double) {
                    supportSQLiteStatement.bindDouble(bindIndex, (Double) bind);
                } else if (bind instanceof String) {
                    supportSQLiteStatement.bindString(bindIndex, (String) bind);
                } else if (bind instanceof byte[]) {
                    supportSQLiteStatement.bindBlob(bindIndex, (byte[]) bind);
                }
            }
        }

        private void saveBinds(int bindIndex, Object value) {
            int index = bindIndex - 1;
            if (index >= mBinds.size()) {
                // Add null entries to the list until we have the desired # of indices
                for (int i = mBinds.size(); i <= index; i++) {
                    mBinds.add(null);
                }
            }
            mBinds.set(index, value);
        }

        @Override
        public void close() throws IOException {
            // Nothing to do here since we re-compile the statement each time.
        }

        @Override
        public void execute() {
            executeSqliteStatementWithRefCount(statement -> {
                statement.execute();
                return null;
            });
        }

        @Override
        public int executeUpdateDelete() {
            return executeSqliteStatementWithRefCount(SupportSQLiteStatement::executeUpdateDelete);
        }

        @Override
        public long executeInsert() {
            return executeSqliteStatementWithRefCount(SupportSQLiteStatement::executeInsert);
        }

        @Override
        public long simpleQueryForLong() {
            return executeSqliteStatementWithRefCount(SupportSQLiteStatement::simpleQueryForLong);
        }

        @Override
        public String simpleQueryForString() {
            return executeSqliteStatementWithRefCount(SupportSQLiteStatement::simpleQueryForString);
        }

        @Override
        public void bindNull(int index) {
            saveBinds(index, null);
        }

        @Override
        public void bindLong(int index, long value) {
            saveBinds(index, value);
        }

        @Override
        public void bindDouble(int index, double value) {
            saveBinds(index, value);
        }

        @Override
        public void bindString(int index, String value) {
            saveBinds(index, value);
        }

        @Override
        public void bindBlob(int index, byte[] value) {
            saveBinds(index, value);
        }

        @Override
        public void clearBindings() {
            mBinds.clear();
        }
    }
}
