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

package androidx.room;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.collection.SparseArrayCompat;
import androidx.core.app.ActivityManagerCompat;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteStatement;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base class for all Room databases. All classes that are annotated with {@link Database} must
 * extend this class.
 * <p>
 * RoomDatabase provides direct access to the underlying database implementation but you should
 * prefer using {@link Dao} classes.
 *
 * @see Database
 */
//@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class RoomDatabase {
    private static final String DB_IMPL_SUFFIX = "_Impl";
    /**
     * Unfortunately, we cannot read this value so we are only setting it to the SQLite default.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final int MAX_BIND_PARAMETER_CNT = 999;
    // set by the generated open helper.
    protected volatile SupportSQLiteDatabase mDatabase;
    private SupportSQLiteOpenHelper mOpenHelper;
    private final InvalidationTracker mInvalidationTracker;
    private boolean mAllowMainThreadQueries;
    boolean mWriteAheadLoggingEnabled;

    @Nullable
    protected List<Callback> mCallbacks;

    private final ReentrantLock mCloseLock = new ReentrantLock();

    /**
     * {@link InvalidationTracker} uses this lock to prevent the database from closing while it is
     * querying database updates.
     *
     * @return The lock for {@link #close()}.
     */
    Lock getCloseLock() {
        return mCloseLock;
    }

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
    public void init(@NonNull DatabaseConfiguration configuration) {
        mOpenHelper = createOpenHelper(configuration);
        boolean wal = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            wal = configuration.journalMode == JournalMode.WRITE_AHEAD_LOGGING;
            mOpenHelper.setWriteAheadLoggingEnabled(wal);
        }
        mCallbacks = configuration.callbacks;
        mAllowMainThreadQueries = configuration.allowMainThreadQueries;
        mWriteAheadLoggingEnabled = wal;
    }

    /**
     * Returns the SQLite open helper used by this database.
     *
     * @return The SQLite open helper used by this database.
     */
    @NonNull
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
    @NonNull
    protected abstract SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration config);

    /**
     * Called when the RoomDatabase is created.
     * <p>
     * This is already implemented by the generated code.
     *
     * @return Creates a new InvalidationTracker.
     */
    @NonNull
    protected abstract InvalidationTracker createInvalidationTracker();

    /**
     * Deletes all rows from all the tables that are registered to this database as
     * {@link Database#entities()}.
     * <p>
     * This does NOT reset the auto-increment value generated by {@link PrimaryKey#autoGenerate()}.
     * <p>
     * After deleting the rows, Room will set a WAL checkpoint and run VACUUM. This means that the
     * data is completely erased. The space will be reclaimed by the system if the amount surpasses
     * the threshold of database file size.
     *
     * @see <a href="https://www.sqlite.org/fileformat.html">Database File Format</a>
     */
    @WorkerThread
    public abstract void clearAllTables();

    /**
     * Returns true if database connection is open and initialized.
     *
     * @return true if the database connection is open, false otherwise.
     */
    public boolean isOpen() {
        final SupportSQLiteDatabase db = mDatabase;
        return db != null && db.isOpen();
    }

    /**
     * Closes the database if it is already open.
     */
    public void close() {
        if (isOpen()) {
            try {
                mCloseLock.lock();
                mOpenHelper.close();
            } finally {
                mCloseLock.unlock();
            }
        }
    }

    /**
     * Asserts that we are not on the main thread.
     *
     * @hide
     */
    @SuppressWarnings("WeakerAccess")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    // used in generated code
    public void assertNotMainThread() {
        if (mAllowMainThreadQueries) {
            return;
        }
        if (ArchTaskExecutor.getInstance().isMainThread()) {
            throw new IllegalStateException("Cannot access database on the main thread since"
                    + " it may potentially lock the UI for a long period of time.");
        }
    }

    // Below, there are wrapper methods for SupportSQLiteDatabase. This helps us track which
    // methods we are using and also helps unit tests to mock this class without mocking
    // all SQLite database methods.

    /**
     * Convenience method to query the database with arguments.
     *
     * @param query The sql query
     * @param args The bind arguments for the placeholders in the query
     *
     * @return A Cursor obtained by running the given query in the Room database.
     */
    public Cursor query(String query, @Nullable Object[] args) {
        return mOpenHelper.getWritableDatabase().query(new SimpleSQLiteQuery(query, args));
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#query(SupportSQLiteQuery)}.
     *
     * @param query The Query which includes the SQL and a bind callback for bind arguments.
     * @return Result of the query.
     */
    public Cursor query(SupportSQLiteQuery query) {
        assertNotMainThread();
        return mOpenHelper.getWritableDatabase().query(query);
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#compileStatement(String)}.
     *
     * @param sql The query to compile.
     * @return The compiled query.
     */
    public SupportSQLiteStatement compileStatement(@NonNull String sql) {
        assertNotMainThread();
        return mOpenHelper.getWritableDatabase().compileStatement(sql);
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#beginTransaction()}.
     */
    public void beginTransaction() {
        assertNotMainThread();
        SupportSQLiteDatabase database = mOpenHelper.getWritableDatabase();
        mInvalidationTracker.syncTriggers(database);
        database.beginTransaction();
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#endTransaction()}.
     */
    public void endTransaction() {
        mOpenHelper.getWritableDatabase().endTransaction();
        if (!inTransaction()) {
            // enqueue refresh only if we are NOT in a transaction. Otherwise, wait for the last
            // endTransaction call to do it.
            mInvalidationTracker.refreshVersionsAsync();
        }
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#setTransactionSuccessful()}.
     */
    public void setTransactionSuccessful() {
        mOpenHelper.getWritableDatabase().setTransactionSuccessful();
    }

    /**
     * Executes the specified {@link Runnable} in a database transaction. The transaction will be
     * marked as successful unless an exception is thrown in the {@link Runnable}.
     *
     * @param body The piece of code to execute.
     */
    public void runInTransaction(@NonNull Runnable body) {
        beginTransaction();
        try {
            body.run();
            setTransactionSuccessful();
        } finally {
            endTransaction();
        }
    }

    /**
     * Executes the specified {@link Callable} in a database transaction. The transaction will be
     * marked as successful unless an exception is thrown in the {@link Callable}.
     *
     * @param body The piece of code to execute.
     * @param <V>  The type of the return value.
     * @return The value returned from the {@link Callable}.
     */
    public <V> V runInTransaction(@NonNull Callable<V> body) {
        beginTransaction();
        try {
            V result = body.call();
            setTransactionSuccessful();
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Exception in transaction", e);
        } finally {
            endTransaction();
        }
    }

    /**
     * Called by the generated code when database is open.
     * <p>
     * You should never call this method manually.
     *
     * @param db The database instance.
     */
    protected void internalInitInvalidationTracker(@NonNull SupportSQLiteDatabase db) {
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
    @NonNull
    public InvalidationTracker getInvalidationTracker() {
        return mInvalidationTracker;
    }

    /**
     * Returns true if current thread is in a transaction.
     *
     * @return True if there is an active transaction in current thread, false otherwise.
     * @see SupportSQLiteDatabase#inTransaction()
     */
    @SuppressWarnings("WeakerAccess")
    public boolean inTransaction() {
        return mOpenHelper.getWritableDatabase().inTransaction();
    }

    /**
     * Journal modes for SQLite database.
     *
     * @see RoomDatabase.Builder#setJournalMode(JournalMode)
     */
    public enum JournalMode {

        /**
         * Let Room choose the journal mode. This is the default value when no explicit value is
         * specified.
         * <p>
         * The actual value will be {@link #TRUNCATE} when the device runs API Level lower than 16
         * or it is a low-RAM device. Otherwise, {@link #WRITE_AHEAD_LOGGING} will be used.
         */
        AUTOMATIC,

        /**
         * Truncate journal mode.
         */
        TRUNCATE,

        /**
         * Write-Ahead Logging mode.
         */
        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
        WRITE_AHEAD_LOGGING;

        /**
         * Resolves {@link #AUTOMATIC} to either {@link #TRUNCATE} or
         * {@link #WRITE_AHEAD_LOGGING}.
         */
        @SuppressLint("NewApi")
        JournalMode resolve(Context context) {
            if (this != AUTOMATIC) {
                return this;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ActivityManager manager = (ActivityManager)
                        context.getSystemService(Context.ACTIVITY_SERVICE);
                if (manager != null && !ActivityManagerCompat.isLowRamDevice(manager)) {
                    return WRITE_AHEAD_LOGGING;
                }
            }
            return TRUNCATE;
        }
    }

    /**
     * Builder for RoomDatabase.
     *
     * @param <T> The type of the abstract database class.
     */
    public static class Builder<T extends RoomDatabase> {
        private final Class<T> mDatabaseClass;
        private final String mName;
        private final Context mContext;
        private ArrayList<Callback> mCallbacks;

        private SupportSQLiteOpenHelper.Factory mFactory;
        private boolean mAllowMainThreadQueries;
        private JournalMode mJournalMode;
        private boolean mRequireMigration;
        /**
         * Migrations, mapped by from-to pairs.
         */
        private final MigrationContainer mMigrationContainer;
        private Set<Integer> mMigrationsNotRequiredFrom;
        /**
         * Keeps track of {@link Migration#startVersion}s and {@link Migration#endVersion}s added in
         * {@link #addMigrations(Migration...)} for later validation that makes those versions don't
         * match any versions passed to {@link #fallbackToDestructiveMigrationFrom(int...)}.
         */
        private Set<Integer> mMigrationStartAndEndVersions;

        Builder(@NonNull Context context, @NonNull Class<T> klass, @Nullable String name) {
            mContext = context;
            mDatabaseClass = klass;
            mName = name;
            mJournalMode = JournalMode.AUTOMATIC;
            mRequireMigration = true;
            mMigrationContainer = new MigrationContainer();
        }

        /**
         * Sets the database factory. If not set, it defaults to
         * {@link FrameworkSQLiteOpenHelperFactory}.
         *
         * @param factory The factory to use to access the database.
         * @return this
         */
        @NonNull
        public Builder<T> openHelperFactory(@Nullable SupportSQLiteOpenHelper.Factory factory) {
            mFactory = factory;
            return this;
        }

        /**
         * Adds a migration to the builder.
         * <p>
         * Each Migration has a start and end versions and Room runs these migrations to bring the
         * database to the latest version.
         * <p>
         * If a migration item is missing between current version and the latest version, Room
         * will clear the database and recreate so even if you have no changes between 2 versions,
         * you should still provide a Migration object to the builder.
         * <p>
         * A migration can handle more than 1 version (e.g. if you have a faster path to choose when
         * going version 3 to 5 without going to version 4). If Room opens a database at version
         * 3 and latest version is &gt;= 5, Room will use the migration object that can migrate from
         * 3 to 5 instead of 3 to 4 and 4 to 5.
         *
         * @param migrations The migration object that can modify the database and to the necessary
         *                   changes.
         * @return this
         */
        @NonNull
        public Builder<T> addMigrations(@NonNull  Migration... migrations) {
            if (mMigrationStartAndEndVersions == null) {
                mMigrationStartAndEndVersions = new HashSet<>();
            }
            for (Migration migration: migrations) {
                mMigrationStartAndEndVersions.add(migration.startVersion);
                mMigrationStartAndEndVersions.add(migration.endVersion);
            }

            mMigrationContainer.addMigrations(migrations);
            return this;
        }

        /**
         * Disables the main thread query check for Room.
         * <p>
         * Room ensures that Database is never accessed on the main thread because it may lock the
         * main thread and trigger an ANR. If you need to access the database from the main thread,
         * you should always use async alternatives or manually move the call to a background
         * thread.
         * <p>
         * You may want to turn this check off for testing.
         *
         * @return this
         */
        @NonNull
        public Builder<T> allowMainThreadQueries() {
            mAllowMainThreadQueries = true;
            return this;
        }

        /**
         * Sets the journal mode for this database.
         *
         * <p>
         * This value is ignored if the builder is initialized with
         * {@link Room#inMemoryDatabaseBuilder(Context, Class)}.
         * <p>
         * The journal mode should be consistent across multiple instances of
         * {@link RoomDatabase} for a single SQLite database file.
         * <p>
         * The default value is {@link JournalMode#AUTOMATIC}.
         *
         * @param journalMode The journal mode.
         * @return this
         */
        @NonNull
        public Builder<T> setJournalMode(@NonNull JournalMode journalMode) {
            mJournalMode = journalMode;
            return this;
        }

        /**
         * Allows Room to destructively recreate database tables if {@link Migration}s that would
         * migrate old database schemas to the latest schema version are not found.
         * <p>
         * When the database version on the device does not match the latest schema version, Room
         * runs necessary {@link Migration}s on the database.
         * <p>
         * If it cannot find the set of {@link Migration}s that will bring the database to the
         * current version, it will throw an {@link IllegalStateException}.
         * <p>
         * You can call this method to change this behavior to re-create the database instead of
         * crashing.
         * <p>
         * Note that this will delete all of the data in the database tables managed by Room.
         *
         * @return this
         */
        @NonNull
        public Builder<T> fallbackToDestructiveMigration() {
            mRequireMigration = false;
            return this;
        }

        /**
         * Informs Room that it is allowed to destructively recreate database tables from specific
         * starting schema versions.
         * <p>
         * This functionality is the same as that provided by
         * {@link #fallbackToDestructiveMigration()}, except that this method allows the
         * specification of a set of schema versions for which destructive recreation is allowed.
         * <p>
         * Using this method is preferable to {@link #fallbackToDestructiveMigration()} if you want
         * to allow destructive migrations from some schema versions while still taking advantage
         * of exceptions being thrown due to unintentionally missing migrations.
         * <p>
         * Note: No versions passed to this method may also exist as either starting or ending
         * versions in the {@link Migration}s provided to {@link #addMigrations(Migration...)}. If a
         * version passed to this method is found as a starting or ending version in a Migration, an
         * exception will be thrown.
         *
         * @param startVersions The set of schema versions from which Room should use a destructive
         *                      migration.
         * @return this
         */
        @NonNull
        public Builder<T> fallbackToDestructiveMigrationFrom(int... startVersions) {
            if (mMigrationsNotRequiredFrom == null) {
                mMigrationsNotRequiredFrom = new HashSet<>(startVersions.length);
            }
            for (int startVersion : startVersions) {
                mMigrationsNotRequiredFrom.add(startVersion);
            }
            return this;
        }

        /**
         * Adds a {@link Callback} to this database.
         *
         * @param callback The callback.
         * @return this
         */
        @NonNull
        public Builder<T> addCallback(@NonNull Callback callback) {
            if (mCallbacks == null) {
                mCallbacks = new ArrayList<>();
            }
            mCallbacks.add(callback);
            return this;
        }

        /**
         * Creates the databases and initializes it.
         * <p>
         * By default, all RoomDatabases use in memory storage for TEMP tables and enables recursive
         * triggers.
         *
         * @return A new database instance.
         */
        @NonNull
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

            if (mMigrationStartAndEndVersions != null && mMigrationsNotRequiredFrom != null) {
                for (Integer version : mMigrationStartAndEndVersions) {
                    if (mMigrationsNotRequiredFrom.contains(version)) {
                        throw new IllegalArgumentException(
                                "Inconsistency detected. A Migration was supplied to "
                                        + "addMigration(Migration... migrations) that has a start "
                                        + "or end version equal to a start version supplied to "
                                        + "fallbackToDestructiveMigrationFrom(int... "
                                        + "startVersions). Start version: "
                                        + version);
                    }
                }
            }

            if (mFactory == null) {
                mFactory = new FrameworkSQLiteOpenHelperFactory();
            }
            DatabaseConfiguration configuration =
                    new DatabaseConfiguration(mContext, mName, mFactory, mMigrationContainer,
                            mCallbacks, mAllowMainThreadQueries,
                            mJournalMode.resolve(mContext),
                            mRequireMigration, mMigrationsNotRequiredFrom);
            T db = Room.getGeneratedImplementation(mDatabaseClass, DB_IMPL_SUFFIX);
            db.init(configuration);
            return db;
        }
    }

    /**
     * A container to hold migrations. It also allows querying its contents to find migrations
     * between two versions.
     */
    public static class MigrationContainer {
        private SparseArrayCompat<SparseArrayCompat<Migration>> mMigrations =
                new SparseArrayCompat<>();

        /**
         * Adds the given migrations to the list of available migrations. If 2 migrations have the
         * same start-end versions, the latter migration overrides the previous one.
         *
         * @param migrations List of available migrations.
         */
        public void addMigrations(@NonNull Migration... migrations) {
            for (Migration migration : migrations) {
                addMigration(migration);
            }
        }

        private void addMigration(Migration migration) {
            final int start = migration.startVersion;
            final int end = migration.endVersion;
            SparseArrayCompat<Migration> targetMap = mMigrations.get(start);
            if (targetMap == null) {
                targetMap = new SparseArrayCompat<>();
                mMigrations.put(start, targetMap);
            }
            Migration existing = targetMap.get(end);
            if (existing != null) {
                Log.w(Room.LOG_TAG, "Overriding migration " + existing + " with " + migration);
            }
            targetMap.append(end, migration);
        }

        /**
         * Finds the list of migrations that should be run to move from {@code start} version to
         * {@code end} version.
         *
         * @param start The current database version
         * @param end   The target database version
         * @return An ordered list of {@link Migration} objects that should be run to migrate
         * between the given versions. If a migration path cannot be found, returns {@code null}.
         */
        @SuppressWarnings("WeakerAccess")
        @Nullable
        public List<Migration> findMigrationPath(int start, int end) {
            if (start == end) {
                return Collections.emptyList();
            }
            boolean migrateUp = end > start;
            List<Migration> result = new ArrayList<>();
            return findUpMigrationPath(result, migrateUp, start, end);
        }

        private List<Migration> findUpMigrationPath(List<Migration> result, boolean upgrade,
                int start, int end) {
            final int searchDirection = upgrade ? -1 : 1;
            while (upgrade ? start < end : start > end) {
                SparseArrayCompat<Migration> targetNodes = mMigrations.get(start);
                if (targetNodes == null) {
                    return null;
                }
                // keys are ordered so we can start searching from one end of them.
                final int size = targetNodes.size();
                final int firstIndex;
                final int lastIndex;

                if (upgrade) {
                    firstIndex = size - 1;
                    lastIndex = -1;
                } else {
                    firstIndex = 0;
                    lastIndex = size;
                }
                boolean found = false;
                for (int i = firstIndex; i != lastIndex; i += searchDirection) {
                    final int targetVersion = targetNodes.keyAt(i);
                    final boolean shouldAddToPath;
                    if (upgrade) {
                        shouldAddToPath = targetVersion <= end && targetVersion > start;
                    } else {
                        shouldAddToPath = targetVersion >= end && targetVersion < start;
                    }
                    if (shouldAddToPath) {
                        result.add(targetNodes.valueAt(i));
                        start = targetVersion;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return null;
                }
            }
            return result;
        }
    }

    /**
     * Callback for {@link RoomDatabase}.
     */
    public abstract static class Callback {

        /**
         * Called when the database is created for the first time. This is called after all the
         * tables are created.
         *
         * @param db The database.
         */
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
        }

        /**
         * Called when the database has been opened.
         *
         * @param db The database.
         */
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
        }
    }
}
