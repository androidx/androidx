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
import android.os.CancellationSignal;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.SneakyThrow;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteCompat;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteStatement;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base class for all Room databases. All classes that are annotated with {@link Database} must
 * extend this class.
 * <p>
 * RoomDatabase provides direct access to the underlying database implementation but you should
 * prefer using {@link Dao} classes.
 *
 * @see Database
 */
public abstract class RoomDatabase {
    private static final String DB_IMPL_SUFFIX = "_Impl";
    /**
     * Unfortunately, we cannot read this value so we are only setting it to the SQLite default.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static final int MAX_BIND_PARAMETER_CNT = 999;
    /**
     * Set by the generated open helper.
     *
     * @deprecated Will be hidden in the next release.
     */
    @Deprecated
    protected volatile SupportSQLiteDatabase mDatabase;
    private Executor mQueryExecutor;
    private Executor mTransactionExecutor;
    private SupportSQLiteOpenHelper mOpenHelper;
    private final InvalidationTracker mInvalidationTracker;
    private boolean mAllowMainThreadQueries;
    boolean mWriteAheadLoggingEnabled;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Nullable
    @Deprecated
    protected List<Callback> mCallbacks;

    /**
     * A map of auto migration spec classes to their provided instance.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    protected Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> mAutoMigrationSpecs;

    private final ReentrantReadWriteLock mCloseLock = new ReentrantReadWriteLock();

    @Nullable
    private AutoCloser mAutoCloser;

    /**
     * {@link InvalidationTracker} uses this lock to prevent the database from closing while it is
     * querying database updates.
     * <p>
     * The returned lock is reentrant and will allow multiple threads to acquire the lock
     * simultaneously until {@link #close()} is invoked in which the lock becomes exclusive as
     * a way to let the InvalidationTracker finish its work before closing the database.
     *
     * @return The lock for {@link #close()}.
     */
    Lock getCloseLock() {
        return mCloseLock.readLock();
    }

    /**
     * This id is only set on threads that are used to dispatch coroutines within a suspending
     * database transaction.
     */
    private final ThreadLocal<Integer> mSuspendingTransactionId = new ThreadLocal<>();

    /**
     * Gets the suspending transaction id of the current thread.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    ThreadLocal<Integer> getSuspendingTransactionId() {
        return mSuspendingTransactionId;
    }

    private final Map<String, Object> mBackingFieldMap =
            Collections.synchronizedMap(new HashMap<>());

    /**
     * Gets the map for storing extension properties of Kotlin type.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    Map<String, Object> getBackingFieldMap() {
        return mBackingFieldMap;
    }

    // Updated later to an unmodifiable map when init is called.
    private final Map<Class<?>, Object> mTypeConverters;

    /**
     * Gets the instance of the given Type Converter.
     *
     * @param klass The Type Converter class.
     * @param <T> The type of the expected Type Converter subclass.
     * @return An instance of T if it is provided in the builder.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getTypeConverter(@NonNull Class<T> klass) {
        return (T) mTypeConverters.get(klass);
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
        mTypeConverters = new HashMap<>();
        mAutoMigrationSpecs = new HashMap<>();
    }

    /**
     * Called by {@link Room} when it is initialized.
     *
     * @param configuration The database configuration.
     */
    @CallSuper
    public void init(@NonNull DatabaseConfiguration configuration) {
        mOpenHelper = createOpenHelper(configuration);
        Set<Class<? extends AutoMigrationSpec>> requiredAutoMigrationSpecs =
                getRequiredAutoMigrationSpecs();
        BitSet usedSpecs = new BitSet();
        for (Class<? extends AutoMigrationSpec> spec : requiredAutoMigrationSpecs) {
            int foundIndex = -1;
            for (int providedIndex = configuration.autoMigrationSpecs.size() - 1;
                    providedIndex >= 0; providedIndex--
            ) {
                Object provided = configuration.autoMigrationSpecs.get(providedIndex);
                if (spec.isAssignableFrom(provided.getClass())) {
                    foundIndex = providedIndex;
                    usedSpecs.set(foundIndex);
                    break;
                }
            }
            if (foundIndex < 0) {
                throw new IllegalArgumentException(
                        "A required auto migration spec (" + spec.getCanonicalName()
                                + ") is missing in the database configuration.");
            }
            mAutoMigrationSpecs.put(spec, configuration.autoMigrationSpecs.get(foundIndex));
        }

        for (int providedIndex = configuration.autoMigrationSpecs.size() - 1;
                providedIndex >= 0; providedIndex--) {
            if (!usedSpecs.get(providedIndex)) {
                throw new IllegalArgumentException("Unexpected auto migration specs found. "
                        + "Annotate AutoMigrationSpec implementation with "
                        + "@ProvidedAutoMigrationSpec annotation or remove this spec from the "
                        + "builder.");
            }
        }

        List<Migration> autoMigrations = getAutoMigrations(mAutoMigrationSpecs);
        for (Migration autoMigration : autoMigrations) {
            boolean migrationExists = configuration.migrationContainer.getMigrations()
                            .containsKey(autoMigration.startVersion);
            if (!migrationExists) {
                configuration.migrationContainer.addMigrations(autoMigration);
            }
        }

        // Configure SqliteCopyOpenHelper if it is available:
        SQLiteCopyOpenHelper copyOpenHelper = unwrapOpenHelper(SQLiteCopyOpenHelper.class,
                mOpenHelper);
        if (copyOpenHelper != null) {
            copyOpenHelper.setDatabaseConfiguration(configuration);
        }

        AutoClosingRoomOpenHelper autoClosingRoomOpenHelper =
                unwrapOpenHelper(AutoClosingRoomOpenHelper.class, mOpenHelper);

        if (autoClosingRoomOpenHelper != null) {
            mAutoCloser = autoClosingRoomOpenHelper.getAutoCloser();
            mInvalidationTracker.setAutoCloser(mAutoCloser);
        }


        boolean wal = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            wal = configuration.journalMode == JournalMode.WRITE_AHEAD_LOGGING;
            mOpenHelper.setWriteAheadLoggingEnabled(wal);
        }
        mCallbacks = configuration.callbacks;
        mQueryExecutor = configuration.queryExecutor;
        mTransactionExecutor = new TransactionExecutor(configuration.transactionExecutor);
        mAllowMainThreadQueries = configuration.allowMainThreadQueries;
        mWriteAheadLoggingEnabled = wal;
        if (configuration.multiInstanceInvalidation) {
            mInvalidationTracker.startMultiInstanceInvalidation(configuration.context,
                    configuration.name);
        }

        Map<Class<?>, List<Class<?>>> requiredFactories = getRequiredTypeConverters();
        // indices for each converter on whether it is used or not so that we can throw an exception
        // if developer provides an unused converter. It is not necessarily an error but likely
        // to be because why would developer add a converter if it won't be used?
        BitSet used = new BitSet();
        for (Map.Entry<Class<?>, List<Class<?>>> entry : requiredFactories.entrySet()) {
            Class<?> daoName = entry.getKey();
            for (Class<?> converter : entry.getValue()) {
                int foundIndex = -1;
                // traverse provided converters in reverse so that newer one overrides
                for (int providedIndex = configuration.typeConverters.size() - 1;
                        providedIndex >= 0; providedIndex--) {
                    Object provided = configuration.typeConverters.get(providedIndex);
                    if (converter.isAssignableFrom(provided.getClass())) {
                        foundIndex = providedIndex;
                        used.set(foundIndex);
                        break;
                    }
                }
                if (foundIndex < 0) {
                    throw new IllegalArgumentException(
                            "A required type converter (" + converter + ") for"
                                    + " " + daoName.getCanonicalName()
                                    + " is missing in the database configuration.");
                }
                mTypeConverters.put(converter, configuration.typeConverters.get(foundIndex));
            }
        }
        // now, make sure all provided factories are used
        for (int providedIndex = configuration.typeConverters.size() - 1;
                providedIndex >= 0; providedIndex--) {
            if (!used.get(providedIndex)) {
                Object converter = configuration.typeConverters.get(providedIndex);
                throw new IllegalArgumentException("Unexpected type converter " + converter + ". "
                        + "Annotate TypeConverter class with @ProvidedTypeConverter annotation "
                        + "or remove this converter from the builder.");
            }
        }
    }

    /**
     * Returns a list of {@link Migration} of a database that have been automatically generated.
     *
     * @return A list of migration instances each of which is a generated autoMigration
     * @param autoMigrationSpecs
     *
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public List<Migration> getAutoMigrations(
            @NonNull Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs
    ) {
        return Collections.emptyList();
    }

    /**
     * Unwraps (delegating) open helpers until it finds clazz, otherwise returns null.
     *
     * @param clazz the open helper type to search for
     * @param openHelper the open helper to search through
     * @param <T> the type of clazz
     * @return the instance of clazz, otherwise null
     */
    @Nullable
    @SuppressWarnings("unchecked")
    private <T> T unwrapOpenHelper(Class<T> clazz, SupportSQLiteOpenHelper openHelper) {
        if (clazz.isInstance(openHelper)) {
            return (T) openHelper;
        }
        if (openHelper instanceof DelegatingOpenHelper) {
            return unwrapOpenHelper(clazz, ((DelegatingOpenHelper) openHelper).getDelegate());
        }
        return null;
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
     * Returns a Map of String -> List&lt;Class&gt; where each entry has the `key` as the DAO name
     * and `value` as the list of type converter classes that are necessary for the database to
     * function.
     * <p>
     * This is implemented by the generated code.
     *
     * @return Creates a map that will include all required type converters for this database.
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
        return Collections.emptyMap();
    }

    /**
     * Returns a Set of required AutoMigrationSpec classes.
     * <p>
     * This is implemented by the generated code.
     *
     * @return Creates a set that will include all required auto migration specs for this database.
     *
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
        return Collections.emptySet();
    }

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
        // We need to special case for the auto closing database because mDatabase is the
        // underlying database and not the wrapped database.
        if (mAutoCloser != null) {
            return mAutoCloser.isActive();
        }

        final SupportSQLiteDatabase db = mDatabase;
        return db != null && db.isOpen();
    }

    /**
     * Closes the database if it is already open.
     */
    public void close() {
        if (isOpen()) {
            final Lock closeLock = mCloseLock.writeLock();
            closeLock.lock();
            try {
                mInvalidationTracker.stopMultiInstanceInvalidation();
                mOpenHelper.close();
            } finally {
                closeLock.unlock();
            }
        }
    }

    /**
     * Asserts that we are not on the main thread.
     *
     * @hide
     */
    @SuppressWarnings("WeakerAccess")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    // used in generated code
    public void assertNotMainThread() {
        if (mAllowMainThreadQueries) {
            return;
        }
        if (isMainThread()) {
            throw new IllegalStateException("Cannot access database on the main thread since"
                    + " it may potentially lock the UI for a long period of time.");
        }
    }

    /**
     * Asserts that we are not on a suspending transaction.
     *
     * @hide
     */
    @SuppressWarnings("WeakerAccess")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    // used in generated code
    public void assertNotSuspendingTransaction() {
        if (!inTransaction() && mSuspendingTransactionId.get() != null) {
            throw new IllegalStateException("Cannot access database on a different coroutine"
                    + " context inherited from a suspending transaction.");
        }
    }

    // Below, there are wrapper methods for SupportSQLiteDatabase. This helps us track which
    // methods we are using and also helps unit tests to mock this class without mocking
    // all SQLite database methods.

    /**
     * Convenience method to query the database with arguments.
     *
     * @param query The sql query
     * @param args  The bind arguments for the placeholders in the query
     * @return A Cursor obtained by running the given query in the Room database.
     */
    @NonNull
    public Cursor query(@NonNull String query, @Nullable Object[] args) {
        return mOpenHelper.getWritableDatabase().query(new SimpleSQLiteQuery(query, args));
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#query(SupportSQLiteQuery)}.
     *
     * @param query The Query which includes the SQL and a bind callback for bind arguments.
     * @return Result of the query.
     */
    @NonNull
    public Cursor query(@NonNull SupportSQLiteQuery query) {
        return query(query, null);
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#query(SupportSQLiteQuery)}.
     *
     * @param query The Query which includes the SQL and a bind callback for bind arguments.
     * @param signal The cancellation signal to be attached to the query.
     * @return Result of the query.
     */
    @NonNull
    public Cursor query(@NonNull SupportSQLiteQuery query, @Nullable CancellationSignal signal) {
        assertNotMainThread();
        assertNotSuspendingTransaction();
        if (signal != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return mOpenHelper.getWritableDatabase().query(query, signal);
        } else {
            return mOpenHelper.getWritableDatabase().query(query);
        }
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#compileStatement(String)}.
     *
     * @param sql The query to compile.
     * @return The compiled query.
     */
    public SupportSQLiteStatement compileStatement(@NonNull String sql) {
        assertNotMainThread();
        assertNotSuspendingTransaction();
        return mOpenHelper.getWritableDatabase().compileStatement(sql);
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#beginTransaction()}.
     *
     * @deprecated Use {@link #runInTransaction(Runnable)}
     */
    @Deprecated
    public void beginTransaction() {
        assertNotMainThread();
        if (mAutoCloser == null) {
            internalBeginTransaction();
        } else {
            mAutoCloser.executeRefCountingFunction(db -> {
                internalBeginTransaction();
                return null;
            });
        }
    }

    private void internalBeginTransaction() {
        assertNotMainThread();
        SupportSQLiteDatabase database = mOpenHelper.getWritableDatabase();
        mInvalidationTracker.syncTriggers(database);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && database.isWriteAheadLoggingEnabled()) {
            database.beginTransactionNonExclusive();
        } else {
            database.beginTransaction();
        }
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#endTransaction()}.
     *
     * @deprecated Use {@link #runInTransaction(Runnable)}
     */
    @Deprecated
    public void endTransaction() {
        if (mAutoCloser == null) {
            internalEndTransaction();
        } else {
            mAutoCloser.executeRefCountingFunction(db -> {
                internalEndTransaction();
                return null;
            });
        }
    }

    private void internalEndTransaction() {
        mOpenHelper.getWritableDatabase().endTransaction();
        if (!inTransaction()) {
            // enqueue refresh only if we are NOT in a transaction. Otherwise, wait for the last
            // endTransaction call to do it.
            mInvalidationTracker.refreshVersionsAsync();
        }
    }

    /**
     * @return The Executor in use by this database for async queries.
     */
    @NonNull
    public Executor getQueryExecutor() {
        return mQueryExecutor;
    }

    /**
     * @return The Executor in use by this database for async transactions.
     */
    @NonNull
    public Executor getTransactionExecutor() {
        return mTransactionExecutor;
    }

    /**
     * Wrapper for {@link SupportSQLiteDatabase#setTransactionSuccessful()}.
     *
     * @deprecated Use {@link #runInTransaction(Runnable)}
     */
    @Deprecated
    public void setTransactionSuccessful() {
        mOpenHelper.getWritableDatabase().setTransactionSuccessful();
    }

    /**
     * Executes the specified {@link Runnable} in a database transaction. The transaction will be
     * marked as successful unless an exception is thrown in the {@link Runnable}.
     * <p>
     * Room will only perform at most one transaction at a time.
     *
     * @param body The piece of code to execute.
     */
    @SuppressWarnings("deprecation")
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
     * <p>
     * Room will only perform at most one transaction at a time.
     *
     * @param body The piece of code to execute.
     * @param <V>  The type of the return value.
     * @return The value returned from the {@link Callable}.
     */
    @SuppressWarnings("deprecation")
    public <V> V runInTransaction(@NonNull Callable<V> body) {
        beginTransaction();
        try {
            V result = body.call();
            setTransactionSuccessful();
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            SneakyThrow.reThrow(e);
            return null; // Unreachable code, but compiler doesn't know it.
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
     * @see Builder#setJournalMode(JournalMode)
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
                if (manager != null && !isLowRamDevice(manager)) {
                    return WRITE_AHEAD_LOGGING;
                }
            }
            return TRUNCATE;
        }

        private static boolean isLowRamDevice(@NonNull ActivityManager activityManager) {
            if (Build.VERSION.SDK_INT >= 19) {
                return SupportSQLiteCompat.Api19Impl.isLowRamDevice(activityManager);
            }
            return false;
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
        private PrepackagedDatabaseCallback mPrepackagedDatabaseCallback;
        private QueryCallback mQueryCallback;
        private Executor mQueryCallbackExecutor;
        private List<Object> mTypeConverters;
        private List<AutoMigrationSpec> mAutoMigrationSpecs;

        /** The Executor used to run database queries. This should be background-threaded. */
        private Executor mQueryExecutor;
        /** The Executor used to run database transactions. This should be background-threaded. */
        private Executor mTransactionExecutor;
        private SupportSQLiteOpenHelper.Factory mFactory;
        private boolean mAllowMainThreadQueries;
        private JournalMode mJournalMode;
        private boolean mMultiInstanceInvalidation;
        private boolean mRequireMigration;
        private boolean mAllowDestructiveMigrationOnDowngrade;

        private long mAutoCloseTimeout = -1L;
        private TimeUnit mAutoCloseTimeUnit;

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

        private String mCopyFromAssetPath;
        private File mCopyFromFile;
        private Callable<InputStream> mCopyFromInputStream;

        Builder(@NonNull Context context, @NonNull Class<T> klass, @Nullable String name) {
            mContext = context;
            mDatabaseClass = klass;
            mName = name;
            mJournalMode = JournalMode.AUTOMATIC;
            mRequireMigration = true;
            mMigrationContainer = new MigrationContainer();
        }

        /**
         * Configures Room to create and open the database using a pre-packaged database located in
         * the application 'assets/' folder.
         * <p>
         * Room does not open the pre-packaged database, instead it copies it into the internal
         * app database folder and then opens it. The pre-packaged database file must be located in
         * the "assets/" folder of your application. For example, the path for a file located in
         * "assets/databases/products.db" would be "databases/products.db".
         * <p>
         * The pre-packaged database schema will be validated. It might be best to create your
         * pre-packaged database schema utilizing the exported schema files generated when
         * {@link Database#exportSchema()} is enabled.
         * <p>
         * This method is not supported for an in memory database {@link Builder}.
         *
         * @param databaseFilePath The file path within the 'assets/' directory of where the
         *                         database file is located.
         *
         * @return This {@link Builder} instance.
         */
        @NonNull
        public Builder<T> createFromAsset(@NonNull String databaseFilePath) {
            mCopyFromAssetPath = databaseFilePath;
            return this;
        }

        /**
         * Configures Room to create and open the database using a pre-packaged database located in
         * the application 'assets/' folder.
         * <p>
         * Room does not open the pre-packaged database, instead it copies it into the internal
         * app database folder and then opens it. The pre-packaged database file must be located in
         * the "assets/" folder of your application. For example, the path for a file located in
         * "assets/databases/products.db" would be "databases/products.db".
         * <p>
         * The pre-packaged database schema will be validated. It might be best to create your
         * pre-packaged database schema utilizing the exported schema files generated when
         * {@link Database#exportSchema()} is enabled.
         * <p>
         * This method is not supported for an in memory database {@link Builder}.
         *
         * @param databaseFilePath The file path within the 'assets/' directory of where the
         *                         database file is located.
         * @param callback The pre-packaged callback.
         *
         * @return This {@link Builder} instance.
         */
        @NonNull
        @SuppressLint("BuilderSetStyle") // To keep naming consistency.
        public Builder<T> createFromAsset(
                @NonNull String databaseFilePath,
                @NonNull PrepackagedDatabaseCallback callback) {
            mPrepackagedDatabaseCallback = callback;
            mCopyFromAssetPath = databaseFilePath;
            return this;
        }

        /**
         * Configures Room to create and open the database using a pre-packaged database file.
         * <p>
         * Room does not open the pre-packaged database, instead it copies it into the internal
         * app database folder and then opens it. The given file must be accessible and the right
         * permissions must be granted for Room to copy the file.
         * <p>
         * The pre-packaged database schema will be validated. It might be best to create your
         * pre-packaged database schema utilizing the exported schema files generated when
         * {@link Database#exportSchema()} is enabled.
         * <p>
         * The {@link Callback#onOpen(SupportSQLiteDatabase)} method can be used as an indicator
         * that the pre-packaged database was successfully opened by Room and can be cleaned up.
         * <p>
         * This method is not supported for an in memory database {@link Builder}.
         *
         * @param databaseFile The database file.
         *
         * @return This {@link Builder} instance.
         */
        @NonNull
        public Builder<T> createFromFile(@NonNull File databaseFile) {
            mCopyFromFile = databaseFile;
            return this;
        }

        /**
         * Configures Room to create and open the database using a pre-packaged database file.
         * <p>
         * Room does not open the pre-packaged database, instead it copies it into the internal
         * app database folder and then opens it. The given file must be accessible and the right
         * permissions must be granted for Room to copy the file.
         * <p>
         * The pre-packaged database schema will be validated. It might be best to create your
         * pre-packaged database schema utilizing the exported schema files generated when
         * {@link Database#exportSchema()} is enabled.
         * <p>
         * The {@link Callback#onOpen(SupportSQLiteDatabase)} method can be used as an indicator
         * that the pre-packaged database was successfully opened by Room and can be cleaned up.
         * <p>
         * This method is not supported for an in memory database {@link Builder}.
         *
         * @param databaseFile The database file.
         * @param callback The pre-packaged callback.
         *
         * @return This {@link Builder} instance.
         */
        @NonNull
        @SuppressLint({"BuilderSetStyle", "StreamFiles"}) // To keep naming consistency.
        public Builder<T> createFromFile(
                @NonNull File databaseFile,
                @NonNull PrepackagedDatabaseCallback callback) {
            mPrepackagedDatabaseCallback = callback;
            mCopyFromFile = databaseFile;
            return this;
        }

        /**
         * Configures Room to create and open the database using a pre-packaged database via an
         * {@link InputStream}.
         * <p>
         * This is useful for processing compressed database files. Room does not open the
         * pre-packaged database, instead it copies it into the internal app database folder, and
         * then open it. The {@link InputStream} will be closed once Room is done consuming it.
         * <p>
         * The pre-packaged database schema will be validated. It might be best to create your
         * pre-packaged database schema utilizing the exported schema files generated when
         * {@link Database#exportSchema()} is enabled.
         * <p>
         * The {@link Callback#onOpen(SupportSQLiteDatabase)} method can be used as an indicator
         * that the pre-packaged database was successfully opened by Room and can be cleaned up.
         * <p>
         * This method is not supported for an in memory database {@link Builder}.
         *
         * @param inputStreamCallable A callable that returns an InputStream from which to copy
         *                            the database. The callable will be invoked in a thread from
         *                            the Executor set via {@link #setQueryExecutor(Executor)}. The
         *                            callable is only invoked if Room needs to create and open the
         *                            database from the pre-package database, usually the first time
         *                            it is created or during a destructive migration.
         *
         * @return This {@link Builder} instance.
         */
        @NonNull
        @SuppressLint("BuilderSetStyle") // To keep naming consistency.
        public Builder<T> createFromInputStream(
                @NonNull Callable<InputStream> inputStreamCallable) {
            mCopyFromInputStream = inputStreamCallable;
            return this;
        }

        /**
         * Configures Room to create and open the database using a pre-packaged database via an
         * {@link InputStream}.
         * <p>
         * This is useful for processing compressed database files. Room does not open the
         * pre-packaged database, instead it copies it into the internal app database folder, and
         * then open it. The {@link InputStream} will be closed once Room is done consuming it.
         * <p>
         * The pre-packaged database schema will be validated. It might be best to create your
         * pre-packaged database schema utilizing the exported schema files generated when
         * {@link Database#exportSchema()} is enabled.
         * <p>
         * The {@link Callback#onOpen(SupportSQLiteDatabase)} method can be used as an indicator
         * that the pre-packaged database was successfully opened by Room and can be cleaned up.
         * <p>
         * This method is not supported for an in memory database {@link Builder}.
         *
         * @param inputStreamCallable A callable that returns an InputStream from which to copy
         *                            the database. The callable will be invoked in a thread from
         *                            the Executor set via {@link #setQueryExecutor(Executor)}. The
         *                            callable is only invoked if Room needs to create and open the
         *                            database from the pre-package database, usually the first time
         *                            it is created or during a destructive migration.
         * @param callback The pre-packaged callback.
         *
         * @return This {@link Builder} instance.
         */
        @NonNull
        @SuppressLint({"BuilderSetStyle", "LambdaLast"}) // To keep naming consistency.
        public Builder<T> createFromInputStream(
                @NonNull Callable<InputStream> inputStreamCallable,
                @NonNull PrepackagedDatabaseCallback callback) {
            mPrepackagedDatabaseCallback = callback;
            mCopyFromInputStream = inputStreamCallable;
            return this;
        }

        /**
         * Sets the database factory. If not set, it defaults to
         * {@link FrameworkSQLiteOpenHelperFactory}.
         *
         * @param factory The factory to use to access the database.
         * @return This {@link Builder} instance.
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
         * @return This {@link Builder} instance.
         */
        @NonNull
        public Builder<T> addMigrations(@NonNull Migration... migrations) {
            if (mMigrationStartAndEndVersions == null) {
                mMigrationStartAndEndVersions = new HashSet<>();
            }
            for (Migration migration : migrations) {
                mMigrationStartAndEndVersions.add(migration.startVersion);
                mMigrationStartAndEndVersions.add(migration.endVersion);
            }

            mMigrationContainer.addMigrations(migrations);
            return this;
        }

        /**
         * Adds an auto migration spec to the builder.
         *
         * @param autoMigrationSpec The auto migration object that is annotated with
         * {@link AutoMigrationSpec} and is declared in an {@link AutoMigration} annotation.
         * @return This {@link Builder} instance.
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public Builder<T> addAutoMigrationSpec(@NonNull AutoMigrationSpec autoMigrationSpec) {
            if (mAutoMigrationSpecs == null) {
                mAutoMigrationSpecs = new ArrayList<>();
            }
            mAutoMigrationSpecs.add(autoMigrationSpec);
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
         * @return This {@link Builder} instance.
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
         * @return This {@link Builder} instance.
         */
        @NonNull
        public Builder<T> setJournalMode(@NonNull JournalMode journalMode) {
            mJournalMode = journalMode;
            return this;
        }

        /**
         * Sets the {@link Executor} that will be used to execute all non-blocking asynchronous
         * queries and tasks, including {@code LiveData} invalidation, {@code Flowable} scheduling
         * and {@code ListenableFuture} tasks.
         * <p>
         * When both the query executor and transaction executor are unset, then a default
         * {@code Executor} will be used. The default {@code Executor} allocates and shares threads
         * amongst Architecture Components libraries. If the query executor is unset but a
         * transaction executor was set, then the same {@code Executor} will be used for queries.
         * <p>
         * For best performance the given {@code Executor} should be bounded (max number of threads
         * is limited).
         * <p>
         * The input {@code Executor} cannot run tasks on the UI thread.
         **
         * @return This {@link Builder} instance.
         *
         * @see #setTransactionExecutor(Executor)
         */
        @NonNull
        public Builder<T> setQueryExecutor(@NonNull Executor executor) {
            mQueryExecutor = executor;
            return this;
        }

        /**
         * Sets the {@link Executor} that will be used to execute all non-blocking asynchronous
         * transaction queries and tasks, including {@code LiveData} invalidation, {@code Flowable}
         * scheduling and {@code ListenableFuture} tasks.
         * <p>
         * When both the transaction executor and query executor are unset, then a default
         * {@code Executor} will be used. The default {@code Executor} allocates and shares threads
         * amongst Architecture Components libraries. If the transaction executor is unset but a
         * query executor was set, then the same {@code Executor} will be used for transactions.
         * <p>
         * If the given {@code Executor} is shared then it should be unbounded to avoid the
         * possibility of a deadlock. Room will not use more than one thread at a time from this
         * executor since only one transaction at a time can be executed, other transactions will
         * be queued on a first come, first serve order.
         * <p>
         * The input {@code Executor} cannot run tasks on the UI thread.
         *
         * @return This {@link Builder} instance.
         *
         * @see #setQueryExecutor(Executor)
         */
        @NonNull
        public Builder<T> setTransactionExecutor(@NonNull Executor executor) {
            mTransactionExecutor = executor;
            return this;
        }

        /**
         * Sets whether table invalidation in this instance of {@link RoomDatabase} should be
         * broadcast and synchronized with other instances of the same {@link RoomDatabase},
         * including those in a separate process. In order to enable multi-instance invalidation,
         * this has to be turned on both ends.
         * <p>
         * This is not enabled by default.
         * <p>
         * This does not work for in-memory databases. This does not work between database instances
         * targeting different database files.
         *
         * @return This {@link Builder} instance.
         */
        @NonNull
        public Builder<T> enableMultiInstanceInvalidation() {
            mMultiInstanceInvalidation = mName != null;
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
         * If the database was create from an asset or a file then Room will try to use the same
         * file to re-create the database, otherwise this will delete all of the data in the
         * database tables managed by Room.
         * <p>
         * To let Room fallback to destructive migration only during a schema downgrade then use
         * {@link #fallbackToDestructiveMigrationOnDowngrade()}.
         *
         * @return This {@link Builder} instance.
         *
         * @see #fallbackToDestructiveMigrationOnDowngrade()
         */
        @NonNull
        public Builder<T> fallbackToDestructiveMigration() {
            mRequireMigration = false;
            mAllowDestructiveMigrationOnDowngrade = true;
            return this;
        }

        /**
         * Allows Room to destructively recreate database tables if {@link Migration}s are not
         * available when downgrading to old schema versions.
         *
         * @return This {@link Builder} instance.
         *
         * @see Builder#fallbackToDestructiveMigration()
         */
        @NonNull
        public Builder<T> fallbackToDestructiveMigrationOnDowngrade() {
            mRequireMigration = true;
            mAllowDestructiveMigrationOnDowngrade = true;
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
         * @return This {@link Builder} instance.
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
         * @return This {@link Builder} instance.
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
         * Sets a {@link QueryCallback} to be invoked when queries are executed.
         * <p>
         * The callback is invoked whenever a query is executed, note that adding this callback
         * has a small cost and should be avoided in production builds unless needed.
         * <p>
         * A use case for providing a callback is to allow logging executed queries. When the
         * callback implementation logs then it is recommended to use an immediate executor.
         *
         * @param queryCallback The query callback.
         * @param executor The executor on which the query callback will be invoked.
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        @NonNull
        public Builder<T> setQueryCallback(@NonNull QueryCallback queryCallback,
                @NonNull Executor executor) {
            mQueryCallback = queryCallback;
            mQueryCallbackExecutor = executor;
            return this;
        }

        /**
         * Adds a type converter instance to this database.
         *
         * @param typeConverter The converter. It must be an instance of a class annotated with
         * {@link ProvidedTypeConverter} otherwise Room will throw an exception.
         * @return This {@link Builder} instance.
         */
        @NonNull
        public Builder<T> addTypeConverter(@NonNull Object typeConverter) {
            if (mTypeConverters == null) {
                mTypeConverters = new ArrayList<>();
            }
            mTypeConverters.add(typeConverter);
            return this;
        }

        /**
         * Enables auto-closing for the database to free up unused resources. The underlying
         * database will be closed after it's last use after the specified {@code
         * autoCloseTimeout} has elapsed since its last usage. The database will be automatically
         * re-opened the next time it is accessed.
         * <p>
         * Auto-closing is not compatible with in-memory databases since the data will be lost
         * when the database is auto-closed.
         * <p>
         * Also, temp tables and temp triggers will be cleared each time the database is
         * auto-closed. If you need to use them, please include them in your
         * {@link RoomDatabase.Callback#onOpen callback}.
         * <p>
         * All configuration should happen in your {@link RoomDatabase.Callback#onOpen}
         * callback so it is re-applied every time the database is re-opened. Note that the
         * {@link RoomDatabase.Callback#onOpen} will be called every time the database is re-opened.
         * <p>
         * The auto-closing database operation runs on the query executor.
         * <p>
         * The database will not be reopened if the RoomDatabase or the
         * SupportSqliteOpenHelper is closed manually (by calling
         * {@link RoomDatabase#close()} or {@link SupportSQLiteOpenHelper#close()}. If the
         * database is closed manually, you must create a new database using
         * {@link RoomDatabase.Builder#build()}.
         *
         * @param autoCloseTimeout  the amount of time after the last usage before closing the
         *                          database. Must greater or equal to zero.
         * @param autoCloseTimeUnit the timeunit for autoCloseTimeout.
         * @return This {@link Builder} instance
         */
        @NonNull
        @SuppressWarnings("MissingGetterMatchingBuilder")
        @ExperimentalRoomApi // When experimental is removed, add these parameters to
        // DatabaseConfiguration
        public Builder<T> setAutoCloseTimeout(
                @IntRange(from = 0) long autoCloseTimeout, @NonNull TimeUnit autoCloseTimeUnit) {
            if (autoCloseTimeout < 0) {
                throw new IllegalArgumentException("autoCloseTimeout must be >= 0");
            }
            mAutoCloseTimeout = autoCloseTimeout;
            mAutoCloseTimeUnit = autoCloseTimeUnit;
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
        @SuppressLint("RestrictedApi")
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
            if (mQueryExecutor == null && mTransactionExecutor == null) {
                mQueryExecutor = mTransactionExecutor = ArchTaskExecutor.getIOThreadExecutor();
            } else if (mQueryExecutor != null && mTransactionExecutor == null) {
                mTransactionExecutor = mQueryExecutor;
            } else if (mQueryExecutor == null && mTransactionExecutor != null) {
                mQueryExecutor = mTransactionExecutor;
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

            SupportSQLiteOpenHelper.Factory factory;

            AutoCloser autoCloser = null;

            if (mFactory == null) {
                factory = new FrameworkSQLiteOpenHelperFactory();
            } else {
                factory = mFactory;
            }

            if (mAutoCloseTimeout > 0) {
                if (mName == null) {
                    throw new IllegalArgumentException("Cannot create auto-closing database for "
                            + "an in-memory database.");
                }

                autoCloser = new AutoCloser(mAutoCloseTimeout, mAutoCloseTimeUnit,
                        mTransactionExecutor);

                factory = new AutoClosingRoomOpenHelperFactory(factory, autoCloser);
            }

            if (mCopyFromAssetPath != null
                    || mCopyFromFile != null
                    || mCopyFromInputStream != null) {
                if (mName == null) {
                    throw new IllegalArgumentException("Cannot create from asset or file for an "
                            + "in-memory database.");
                }

                final int copyConfigurations = (mCopyFromAssetPath == null ? 0 : 1) +
                        (mCopyFromFile == null ? 0 : 1) +
                        (mCopyFromInputStream == null ? 0 : 1);
                if (copyConfigurations != 1) {
                    throw new IllegalArgumentException("More than one of createFromAsset(), "
                            + "createFromInputStream(), and createFromFile() were called on this "
                            + "Builder, but the database can only be created using one of the "
                            + "three configurations.");
                }
                factory = new SQLiteCopyOpenHelperFactory(mCopyFromAssetPath, mCopyFromFile,
                        mCopyFromInputStream, factory);
            }

            if (mQueryCallback != null) {
                factory = new QueryInterceptorOpenHelperFactory(factory, mQueryCallback,
                        mQueryCallbackExecutor);
            }

            DatabaseConfiguration configuration =
                    new DatabaseConfiguration(
                            mContext,
                            mName,
                            factory,
                            mMigrationContainer,
                            mCallbacks,
                            mAllowMainThreadQueries,
                            mJournalMode.resolve(mContext),
                            mQueryExecutor,
                            mTransactionExecutor,
                            mMultiInstanceInvalidation,
                            mRequireMigration,
                            mAllowDestructiveMigrationOnDowngrade,
                            mMigrationsNotRequiredFrom,
                            mCopyFromAssetPath,
                            mCopyFromFile,
                            mCopyFromInputStream,
                            mPrepackagedDatabaseCallback,
                            mTypeConverters,
                            mAutoMigrationSpecs);
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
        private HashMap<Integer, TreeMap<Integer, Migration>> mMigrations = new HashMap<>();

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

        /**
         * Adds the given migrations to the list of available migrations. If 2 migrations have the
         * same start-end versions, the latter migration overrides the previous one.
         *
         * @param migrations List of available migrations.
         */
        public void addMigrations(@NonNull List<Migration> migrations) {
            for (Migration migration : migrations) {
                addMigration(migration);
            }
        }

        private void addMigration(Migration migration) {
            final int start = migration.startVersion;
            final int end = migration.endVersion;
            TreeMap<Integer, Migration> targetMap = mMigrations.get(start);
            if (targetMap == null) {
                targetMap = new TreeMap<>();
                mMigrations.put(start, targetMap);
            }
            Migration existing = targetMap.get(end);
            if (existing != null) {
                Log.w(Room.LOG_TAG, "Overriding migration " + existing + " with " + migration);
            }
            targetMap.put(end, migration);
        }

        /**
         * Returns the map of available migrations where the key is the start version of the
         * migration, and the value is a map of (end version -> Migration).
         *
         * @return Map of migrations keyed by the start version
         */
        @NonNull
        public Map<Integer, Map<Integer, Migration>> getMigrations() {
            return Collections.unmodifiableMap(mMigrations);
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
            while (upgrade ? start < end : start > end) {
                TreeMap<Integer, Migration> targetNodes = mMigrations.get(start);
                if (targetNodes == null) {
                    return null;
                }
                // keys are ordered so we can start searching from one end of them.
                Set<Integer> keySet;
                if (upgrade) {
                    keySet = targetNodes.descendingKeySet();
                } else {
                    keySet = targetNodes.keySet();
                }
                boolean found = false;
                for (int targetVersion : keySet) {
                    final boolean shouldAddToPath;
                    if (upgrade) {
                        shouldAddToPath = targetVersion <= end && targetVersion > start;
                    } else {
                        shouldAddToPath = targetVersion >= end && targetVersion < start;
                    }
                    if (shouldAddToPath) {
                        result.add(targetNodes.get(targetVersion));
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

    /** Returns true if the calling thread is the main thread. */
    private static boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
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

        /**
         * Called after the database was destructively migrated
         *
         * @param db The database.
         */
        public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db){
        }
    }

    /**
     * Callback for {@link Builder#createFromAsset(String)}, {@link Builder#createFromFile(File)}
     * and {@link Builder#createFromInputStream(Callable)}
     * <p>
     * This callback will be invoked after the pre-package DB is copied but before Room had
     * a chance to open it and therefore before the {@link RoomDatabase.Callback} methods are
     * invoked. This callback can be useful for updating the pre-package DB schema to satisfy
     * Room's schema validation.
     */
    public abstract static class PrepackagedDatabaseCallback {

        /**
         * Called when the pre-packaged database has been copied.
         *
         * @param db The database.
         */
        public void onOpenPrepackagedDatabase(@NonNull SupportSQLiteDatabase db) {
        }
    }

    /**
     * Callback interface for when SQLite queries are executed.
     *
     * @see RoomDatabase.Builder#setQueryCallback
     */
    public interface QueryCallback {

        /**
         * Called when a SQL query is executed.
         *
         * @param sqlQuery The SQLite query statement.
         * @param bindArgs Arguments of the query if available, empty list otherwise.
         */
        void onQuery(@NonNull String sqlQuery, @NonNull List<Object>
                bindArgs);
    }
}
