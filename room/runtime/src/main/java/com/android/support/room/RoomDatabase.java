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

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.support.db.SupportSQLiteDatabase;
import com.android.support.db.SupportSQLiteOpenHelper;
import com.android.support.db.SupportSQLiteQuery;
import com.android.support.db.SupportSQLiteStatement;
import com.android.support.db.framework.FrameworkSQLiteOpenHelperFactory;

/**
 * Base class for all Room databases. All classes that are annotated with {@link Database} must
 * extend this class.
 * <p>
 * RoomDatabase provides direct access to the underlying database implementation but you should
 * prefer using {@link Dao} classes.
 *
 * @see Database
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class RoomDatabase {
    private static final String DB_IMPL_SUFFIX = "_Impl";
    // set by the generated open helper.
    protected volatile SupportSQLiteDatabase mDatabase;
    private SupportSQLiteOpenHelper mOpenHelper;
    private final InvalidationTracker mInvalidationTracker;

    /**
     * Creates a RoomDatabase.
     * <p>
     * You cannot create an instance of a database, instead, you should acquire it via
     * {@link Room#databaseBuilder(Context, Class, String)} or
     * {@link Room#inMemoryDatabaseBuilder(Context, Class)}.
     */
    public RoomDatabase() {
        mInvalidationTracker = createInvalidationTracker();
    }

    /**
     * Called by {@link Room} when it is initialized.
     *
     * @param configuration The database configuration.
     */
    @CallSuper
    public void init(DatabaseConfiguration configuration) {
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
     * @return A new SupportSQLiteOpenHelper to be used while connecting to the database.
     */
    protected abstract SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration config);

    /**
     * Called when the RoomDatabase is created.
     * <p>
     * This is already implemented by the generated code.
     *
     * @return Creates a new InvalidationTracker.
     */
    protected abstract InvalidationTracker createInvalidationTracker();

    /**
     * Returns the database connection. Note that, if the database is not opened yet, this method
     * will return {code null}. You can use the {@link #getOpenHelper()} method to open the
     * database.
     *
     * @return The database connection or {@code null} if it is not opened yet.
     */
    @Nullable
    public SupportSQLiteDatabase getDatabase() {
        return mDatabase;
    }

    // Below, there are wrapper methods for SupportSQLiteDatabase. This helps us track which
    // methods we are using and also helps unit tests to mock this class without mocking
    // all sqlite database methods.

    /**
     * Wrapper for {@link SupportSQLiteDatabase#rawQuery(String, String[])}.
     *
     * @param sql Sql query to run.
     * @param selectionArgs Selection arguments.
     * @return Result of the query.
     */
    public Cursor query(String sql, String[] selectionArgs) {
        return mOpenHelper.getWritableDatabase().rawQuery(sql, selectionArgs);
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#rawQuery(SupportSQLiteQuery)}.
     *
     * @param query The Query which includes the SQL and a bind callback for bind argumetns.
     *
     * @return Result of the query.
     */
    public Cursor query(SupportSQLiteQuery query) {
        return mOpenHelper.getWritableDatabase().rawQuery(query);
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#compileStatement(String)}.
     *
     * @param sql The query to compile.
     * @return The compiled query.
     */
    public SupportSQLiteStatement compileStatement(String sql) {
        return mOpenHelper.getWritableDatabase().compileStatement(sql);
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#beginTransaction()}.
     */
    public void beginTransaction() {
        mInvalidationTracker.syncTriggers();
        mOpenHelper.getWritableDatabase().beginTransaction();
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#endTransaction()}.
     */
    public void endTransaction() {
        mOpenHelper.getWritableDatabase().endTransaction();
        mInvalidationTracker.refreshVersionsAsync();
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#setTransactionSuccessful()}.
     */
    public void setTransactionSuccessful() {
        mOpenHelper.getWritableDatabase().setTransactionSuccessful();
    }

    /**
     * Called by the generated code when database is open.
     * <p>
     * You should never call this method manually.
     *
     * @param db The database instance.
     */
    protected void internalInitInvalidationTracker(SupportSQLiteDatabase db) {
        mInvalidationTracker.internalInit(db);
    }

    /**
     * Returns the invalidation tracker for this database.
     * <p>
     * You can use the invalidation tracker to get notified when certain tables in the database
     * are modified.
     *
     * @return The invalidation tracker for the database.
     */
    public InvalidationTracker getInvalidationTracker() {
        return mInvalidationTracker;
    }

    /**
     * Returns true if current thread is in a transaction.
     *
     * @return True if there is an active transaction in current thread, false otherwise.
     *
     * @see SupportSQLiteDatabase#inTransaction()
     */
    public boolean inTransaction() {
        return mOpenHelper.getWritableDatabase().inTransaction();
    }

    /**
     * Builder for RoomDatabase.
     *
     * @param <T> The type of the abstract database class.
     */
    @SuppressWarnings("unused")
    public static class Builder<T extends RoomDatabase> {
        private final Class<T> mDatabaseClass;
        private final String mName;
        private final Context mContext;

        private SupportSQLiteOpenHelper.Factory mFactory;
        private int mVersion = 1;
        private boolean mInMemory;

        Builder(@NonNull Context context, @NonNull Class<T> klass, @Nullable String name) {
            mContext = context;
            mDatabaseClass = klass;
            mName = name;
        }

        /**
         * Sets the database factory. If not set, it defaults to
         * {@link FrameworkSQLiteOpenHelperFactory}.
         *
         * @param factory The factory to use to access the database.
         * @return this
         */
        public Builder<T> openHelperFactory(SupportSQLiteOpenHelper.Factory factory) {
            mFactory = factory;
            return this;
        }

        /**
         * Version of the database, defaults to 1.
         *
         * @param version The database version to use
         * @return this
         */
        public Builder<T> version(int version) {
            mVersion = version;
            return this;
        }

        /**
         * Creates the databases and initializes it.
         * <p>
         * By default, all RoomDatabases use in memory storage for TEMP tables and enables recursive
         * triggers.
         * @return A new database instance.
         */
        public T build() {
            //noinspection ConstantConditions
            if (mContext == null) {
                throw new IllegalArgumentException("Cannot provide null context for the database.");
            }
            //noinspection ConstantConditions
            if (mDatabaseClass == null) {
                throw new IllegalArgumentException("Must provide an abstract class that"
                        + " extends RoomDatabase");
            }
            if (mFactory == null) {
                mFactory = new FrameworkSQLiteOpenHelperFactory();
            }
            DatabaseConfiguration configuration =
                    new DatabaseConfiguration(mContext, mName, mVersion, mFactory);
            T db = Room.getGeneratedImplementation(mDatabaseClass, DB_IMPL_SUFFIX);
            db.init(configuration);
            return db;
        }
    }
}
