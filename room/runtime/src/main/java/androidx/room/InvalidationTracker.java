/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.arch.core.internal.SafeIterableMap;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.collection.SparseArrayCompat;
import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteStatement;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

/**
 * InvalidationTracker keeps a list of tables modified by queries and notifies its callbacks about
 * these tables.
 */
// Some details on how the InvalidationTracker works:
// * An in memory table is created with (table_id, invalidated) table_id is a hardcoded int from
// initialization, while invalidated is a boolean bit to indicate if the table has been invalidated.
// * ObservedTableTracker tracks list of tables we should be watching (e.g. adding triggers for).
// * Before each beginTransaction, RoomDatabase invokes InvalidationTracker to sync trigger states.
// * After each endTransaction, RoomDatabase invokes InvalidationTracker to refresh invalidated
// tables.
// * Each update (write operation) on one of the observed tables triggers an update into the
// memory table table, flipping the invalidated flag ON.
// * When multi-instance invalidation is turned on, MultiInstanceInvalidationClient will be created.
// It works as an Observer, and notifies other instances of table invalidation.
public class InvalidationTracker {

    private static final String[] TRIGGERS = new String[]{"UPDATE", "DELETE", "INSERT"};

    private static final String UPDATE_TABLE_NAME = "room_table_modification_log";

    private static final String TABLE_ID_COLUMN_NAME = "table_id";

    private static final String INVALIDATED_COLUMN_NAME = "invalidated";

    private static final String CREATE_TRACKING_TABLE_SQL = "CREATE TEMP TABLE " + UPDATE_TABLE_NAME
            + "(" + TABLE_ID_COLUMN_NAME + " INTEGER PRIMARY KEY, "
            + INVALIDATED_COLUMN_NAME + " INTEGER NOT NULL DEFAULT 0)";

    @VisibleForTesting
    static final String RESET_UPDATED_TABLES_SQL = "UPDATE " + UPDATE_TABLE_NAME
            + " SET " + INVALIDATED_COLUMN_NAME + " = 0 WHERE " + INVALIDATED_COLUMN_NAME + " = 1 ";

    @VisibleForTesting
    static final String SELECT_UPDATED_TABLES_SQL = "SELECT * FROM " + UPDATE_TABLE_NAME
            + " WHERE " + INVALIDATED_COLUMN_NAME + " = 1;";

    @NonNull
    @VisibleForTesting
    final ArrayMap<String, Integer> mTableIdLookup;
    final String[] mTableNames;
    @NonNull
    @VisibleForTesting
    final SparseArrayCompat<String> mShadowTableLookup;

    @NonNull
    private Map<String, Set<String>> mViewTables;

    @NonNull
    @VisibleForTesting
    final BitSet mTableInvalidStatus;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final RoomDatabase mDatabase;

    AtomicBoolean mPendingRefresh = new AtomicBoolean(false);

    private volatile boolean mInitialized = false;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    volatile SupportSQLiteStatement mCleanupStatement;

    private ObservedTableTracker mObservedTableTracker;

    private final InvalidationLiveDataContainer mInvalidationLiveDataContainer;

    // should be accessed with synchronization only.
    @VisibleForTesting
    final SafeIterableMap<Observer, ObserverWrapper> mObserverMap = new SafeIterableMap<>();

    private MultiInstanceInvalidationClient mMultiInstanceInvalidationClient;

    /**
     * Used by the generated code.
     *
     * @hide
     */
    @SuppressWarnings("WeakerAccess")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public InvalidationTracker(RoomDatabase database, String... tableNames) {
        this(database, new HashMap<String, String>(), Collections.<String, Set<String>>emptyMap(),
                tableNames);
    }

    /**
     * Used by the generated code.
     *
     * @hide
     */
    @SuppressWarnings("WeakerAccess")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public InvalidationTracker(RoomDatabase database, Map<String, String> shadowTablesMap,
            Map<String, Set<String>> viewTables, String... tableNames) {
        mDatabase = database;
        mObservedTableTracker = new ObservedTableTracker(tableNames.length);
        mTableIdLookup = new ArrayMap<>();
        mShadowTableLookup = new SparseArrayCompat<>(shadowTablesMap.size());
        mViewTables = viewTables;
        mInvalidationLiveDataContainer = new InvalidationLiveDataContainer(mDatabase);
        final int size = tableNames.length;
        mTableNames = new String[size];
        for (int id = 0; id < size; id++) {
            final String tableName = tableNames[id].toLowerCase(Locale.US);
            mTableIdLookup.put(tableName, id);
            mTableNames[id] = tableName;
            String shadowTableName = shadowTablesMap.get(tableNames[id]);
            if (shadowTableName != null) {
                mShadowTableLookup.append(id, shadowTableName.toLowerCase(Locale.US));
            }
        }
        mTableInvalidStatus = new BitSet(tableNames.length);
    }

    /**
     * Internal method to initialize table tracking.
     * <p>
     * You should never call this method, it is called by the generated code.
     */
    void internalInit(SupportSQLiteDatabase database) {
        synchronized (this) {
            if (mInitialized) {
                Log.e(Room.LOG_TAG, "Invalidation tracker is initialized twice :/.");
                return;
            }

            database.beginTransaction();
            try {
                database.execSQL("PRAGMA temp_store = MEMORY;");
                database.execSQL("PRAGMA recursive_triggers='ON';");
                database.execSQL(CREATE_TRACKING_TABLE_SQL);
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
            syncTriggers(database);
            mCleanupStatement = database.compileStatement(RESET_UPDATED_TABLES_SQL);
            mInitialized = true;
        }
    }

    void startMultiInstanceInvalidation(Context context, String name) {
        mMultiInstanceInvalidationClient = new MultiInstanceInvalidationClient(context, name, this,
                mDatabase.getQueryExecutor());
    }

    void stopMultiInstanceInvalidation() {
        if (mMultiInstanceInvalidationClient != null) {
            mMultiInstanceInvalidationClient.stop();
            mMultiInstanceInvalidationClient = null;
        }
    }

    private static void appendTriggerName(StringBuilder builder, String tableName,
            String triggerType) {
        builder.append("`")
                .append("room_table_modification_trigger_")
                .append(tableName)
                .append("_")
                .append(triggerType)
                .append("`");
    }

    private void stopTrackingTable(SupportSQLiteDatabase writableDb, int tableId) {
        final String tableName = mShadowTableLookup.get(tableId, mTableNames[tableId]);
        StringBuilder stringBuilder = new StringBuilder();
        for (String trigger : TRIGGERS) {
            stringBuilder.setLength(0);
            stringBuilder.append("DROP TRIGGER IF EXISTS ");
            appendTriggerName(stringBuilder, tableName, trigger);
            writableDb.execSQL(stringBuilder.toString());
        }
    }

    private void startTrackingTable(SupportSQLiteDatabase writableDb, int tableId) {
        writableDb.execSQL(
                "INSERT OR IGNORE INTO " + UPDATE_TABLE_NAME + " VALUES(" + tableId + ", 0)");
        final String tableName = mShadowTableLookup.get(tableId, mTableNames[tableId]);
        StringBuilder stringBuilder = new StringBuilder();
        for (String trigger : TRIGGERS) {
            stringBuilder.setLength(0);
            stringBuilder.append("CREATE TEMP TRIGGER IF NOT EXISTS ");
            appendTriggerName(stringBuilder, tableName, trigger);
            stringBuilder.append(" AFTER ")
                    .append(trigger)
                    .append(" ON `")
                    .append(tableName)
                    .append("` BEGIN UPDATE ")
                    .append(UPDATE_TABLE_NAME)
                    .append(" SET ").append(INVALIDATED_COLUMN_NAME).append(" = 1")
                    .append(" WHERE ").append(TABLE_ID_COLUMN_NAME).append(" = ").append(tableId)
                    .append(" AND ").append(INVALIDATED_COLUMN_NAME).append(" = 0")
                    .append("; END");
            writableDb.execSQL(stringBuilder.toString());
        }
    }

    /**
     * Adds the given observer to the observers list and it will be notified if any table it
     * observes changes.
     * <p>
     * Database changes are pulled on another thread so in some race conditions, the observer might
     * be invoked for changes that were done before it is added.
     * <p>
     * If the observer already exists, this is a no-op call.
     * <p>
     * If one of the tables in the Observer does not exist in the database, this method throws an
     * {@link IllegalArgumentException}.
     *
     * @param observer The observer which listens the database for changes.
     */
    @WorkerThread
    public void addObserver(@NonNull Observer observer) {
        final String[] tableNames = resolveViews(observer.mTables);
        int[] tableIds = new int[tableNames.length];
        final int size = tableNames.length;

        for (int i = 0; i < size; i++) {
            Integer tableId = mTableIdLookup.get(tableNames[i].toLowerCase(Locale.US));
            if (tableId == null) {
                throw new IllegalArgumentException("There is no table with name " + tableNames[i]);
            }
            tableIds[i] = tableId;
        }
        ObserverWrapper wrapper = new ObserverWrapper(observer, tableIds, tableNames);
        ObserverWrapper currentObserver;
        synchronized (mObserverMap) {
            currentObserver = mObserverMap.putIfAbsent(observer, wrapper);
        }
        if (currentObserver == null && mObservedTableTracker.onAdded(tableIds)) {
            syncTriggers();
        }
    }

    private String[] validateAndResolveTableNames(String[] tableNames) {
        String[] resolved = resolveViews(tableNames);
        for (String tableName : resolved) {
            if (!mTableIdLookup.containsKey(tableName.toLowerCase(Locale.US))) {
                throw new IllegalArgumentException("There is no table with name " + tableName);
            }
        }
        return resolved;
    }

    /**
     * Resolves the list of tables and views into a list of unique tables that are underlying them.
     *
     * @param names The names of tables or views.
     * @return The names of the underlying tables.
     */
    private String[] resolveViews(String[] names) {
        Set<String> tables = new ArraySet<>();
        for (String name : names) {
            final String lowercase = name.toLowerCase(Locale.US);
            if (mViewTables.containsKey(lowercase)) {
                tables.addAll(mViewTables.get(lowercase));
            } else {
                tables.add(name);
            }
        }
        return tables.toArray(new String[tables.size()]);
    }

    /**
     * Adds an observer but keeps a weak reference back to it.
     * <p>
     * Note that you cannot remove this observer once added. It will be automatically removed
     * when the observer is GC'ed.
     *
     * @param observer The observer to which InvalidationTracker will keep a weak reference.
     * @hide
     */
    @SuppressWarnings("unused")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void addWeakObserver(Observer observer) {
        addObserver(new WeakObserver(this, observer));
    }

    /**
     * Removes the observer from the observers list.
     *
     * @param observer The observer to remove.
     */
    @SuppressWarnings("WeakerAccess")
    @WorkerThread
    public void removeObserver(@NonNull final Observer observer) {
        ObserverWrapper wrapper;
        synchronized (mObserverMap) {
            wrapper = mObserverMap.remove(observer);
        }
        if (wrapper != null && mObservedTableTracker.onRemoved(wrapper.mTableIds)) {
            syncTriggers();
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean ensureInitialization() {
        if (!mDatabase.isOpen()) {
            return false;
        }
        if (!mInitialized) {
            // trigger initialization
            mDatabase.getOpenHelper().getWritableDatabase();
        }
        if (!mInitialized) {
            Log.e(Room.LOG_TAG, "database is not initialized even though it is open");
            return false;
        }
        return true;
    }

    @VisibleForTesting
    Runnable mRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            final Lock closeLock = mDatabase.getCloseLock();
            boolean hasUpdatedTable = false;
            try {
                closeLock.lock();

                if (!ensureInitialization()) {
                    return;
                }

                if (!mPendingRefresh.compareAndSet(true, false)) {
                    // no pending refresh
                    return;
                }

                if (mDatabase.inTransaction()) {
                    // current thread is in a transaction. when it ends, it will invoke
                    // refreshRunnable again. mPendingRefresh is left as false on purpose
                    // so that the last transaction can flip it on again.
                    return;
                }

                if (mDatabase.mWriteAheadLoggingEnabled) {
                    // This transaction has to be on the underlying DB rather than the RoomDatabase
                    // in order to avoid a recursive loop after endTransaction.
                    SupportSQLiteDatabase db = mDatabase.getOpenHelper().getWritableDatabase();
                    db.beginTransaction();
                    try {
                        hasUpdatedTable = checkUpdatedTable();
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                } else {
                    hasUpdatedTable = checkUpdatedTable();
                }
            } catch (IllegalStateException | SQLiteException exception) {
                // may happen if db is closed. just log.
                Log.e(Room.LOG_TAG, "Cannot run invalidation tracker. Is the db closed?",
                        exception);
            } finally {
                closeLock.unlock();
            }
            if (hasUpdatedTable) {
                synchronized (mObserverMap) {
                    for (Map.Entry<Observer, ObserverWrapper> entry : mObserverMap) {
                        entry.getValue().notifyByTableVersions(mTableInvalidStatus);
                    }
                }
                // Reset invalidated status flags.
                mTableInvalidStatus.clear();
            }
        }

        private boolean checkUpdatedTable() {
            boolean hasUpdatedTable = false;
            Cursor cursor = mDatabase.query(new SimpleSQLiteQuery(SELECT_UPDATED_TABLES_SQL));
            //noinspection TryFinallyCanBeTryWithResources
            try {
                while (cursor.moveToNext()) {
                    final int tableId = cursor.getInt(0);
                    mTableInvalidStatus.set(tableId);
                    hasUpdatedTable = true;
                }
            } finally {
                cursor.close();
            }
            if (hasUpdatedTable) {
                mCleanupStatement.executeUpdateDelete();
            }
            return hasUpdatedTable;
        }
    };

    /**
     * Enqueues a task to refresh the list of updated tables.
     * <p>
     * This method is automatically called when {@link RoomDatabase#endTransaction()} is called but
     * if you have another connection to the database or directly use {@link
     * SupportSQLiteDatabase}, you may need to call this manually.
     */
    @SuppressWarnings("WeakerAccess")
    public void refreshVersionsAsync() {
        // TODO we should consider doing this sync instead of async.
        if (mPendingRefresh.compareAndSet(false, true)) {
            mDatabase.getQueryExecutor().execute(mRefreshRunnable);
        }
    }

    /**
     * Check versions for tables, and run observers synchronously if tables have been updated.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @WorkerThread
    public void refreshVersionsSync() {
        syncTriggers();
        mRefreshRunnable.run();
    }

    /**
     * Notifies all the registered {@link Observer}s of table changes.
     * <p>
     * This can be used for notifying invalidation that cannot be detected by this
     * {@link InvalidationTracker}, for example, invalidation from another process.
     *
     * @param tables The invalidated tables.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void notifyObserversByTableNames(String... tables) {
        synchronized (mObserverMap) {
            for (Map.Entry<Observer, ObserverWrapper> entry : mObserverMap) {
                if (!entry.getKey().isRemote()) {
                    entry.getValue().notifyByTableNames(tables);
                }
            }
        }
    }

    void syncTriggers(SupportSQLiteDatabase database) {
        if (database.inTransaction()) {
            // we won't run this inside another transaction.
            return;
        }
        try {
            // This method runs in a while loop because while changes are synced to db, another
            // runnable may be skipped. If we cause it to skip, we need to do its work.
            while (true) {
                Lock closeLock = mDatabase.getCloseLock();
                closeLock.lock();
                try {
                    // there is a potential race condition where another mSyncTriggers runnable
                    // can start running right after we get the tables list to sync.
                    final int[] tablesToSync = mObservedTableTracker.getTablesToSync();
                    if (tablesToSync == null) {
                        return;
                    }
                    final int limit = tablesToSync.length;
                    database.beginTransaction();
                    try {
                        for (int tableId = 0; tableId < limit; tableId++) {
                            switch (tablesToSync[tableId]) {
                                case ObservedTableTracker.ADD:
                                    startTrackingTable(database, tableId);
                                    break;
                                case ObservedTableTracker.REMOVE:
                                    stopTrackingTable(database, tableId);
                                    break;
                            }
                        }
                        database.setTransactionSuccessful();
                    } finally {
                        database.endTransaction();
                    }
                    mObservedTableTracker.onSyncCompleted();
                } finally {
                    closeLock.unlock();
                }
            }
        } catch (IllegalStateException | SQLiteException exception) {
            // may happen if db is closed. just log.
            Log.e(Room.LOG_TAG, "Cannot run invalidation tracker. Is the db closed?",
                    exception);
        }
    }

    /**
     * Called by RoomDatabase before each beginTransaction call.
     * <p>
     * It is important that pending trigger changes are applied to the database before any query
     * runs. Otherwise, we may miss some changes.
     * <p>
     * This api should eventually be public.
     */
    void syncTriggers() {
        if (!mDatabase.isOpen()) {
            return;
        }
        syncTriggers(mDatabase.getOpenHelper().getWritableDatabase());
    }

    /**
     * Creates a LiveData that computes the given function once and for every other invalidation
     * of the database.
     * <p>
     * Holds a strong reference to the created LiveData as long as it is active.
     *
     * @param computeFunction The function that calculates the value
     * @param tableNames      The list of tables to observe
     * @param <T>             The return type
     * @return A new LiveData that computes the given function when the given list of tables
     * invalidates.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public <T> LiveData<T> createLiveData(String[] tableNames, Callable<T> computeFunction) {
        return mInvalidationLiveDataContainer.create(
                validateAndResolveTableNames(tableNames), computeFunction);
    }

    /**
     * Wraps an observer and keeps the table information.
     * <p>
     * Internally table ids are used which may change from database to database so the table
     * related information is kept here rather than in the Observer.
     */
    @SuppressWarnings("WeakerAccess")
    static class ObserverWrapper {
        final int[] mTableIds;
        private final String[] mTableNames;
        final Observer mObserver;
        private final Set<String> mSingleTableSet;

        ObserverWrapper(Observer observer, int[] tableIds, String[] tableNames) {
            mObserver = observer;
            mTableIds = tableIds;
            mTableNames = tableNames;
            if (tableIds.length == 1) {
                ArraySet<String> set = new ArraySet<>();
                set.add(mTableNames[0]);
                mSingleTableSet = Collections.unmodifiableSet(set);
            } else {
                mSingleTableSet = null;
            }
        }

        /**
         * Updates the table versions and notifies the underlying {@link #mObserver} if any of the
         * observed tables are invalidated.
         *
         * @param tableInvalidStatus The table invalid statuses.
         */
        void notifyByTableVersions(BitSet tableInvalidStatus) {
            Set<String> invalidatedTables = null;
            final int size = mTableIds.length;
            for (int index = 0; index < size; index++) {
                final int tableId = mTableIds[index];
                if (tableInvalidStatus.get(tableId)) {
                    if (size == 1) {
                        // Optimization for a single-table observer
                        invalidatedTables = mSingleTableSet;
                    } else {
                        if (invalidatedTables == null) {
                            invalidatedTables = new ArraySet<>(size);
                        }
                        invalidatedTables.add(mTableNames[index]);
                    }
                }
            }
            if (invalidatedTables != null) {
                mObserver.onInvalidated(invalidatedTables);
            }
        }

        /**
         * Notifies the underlying {@link #mObserver} if it observes any of the specified
         * {@code tables}.
         *
         * @param tables The invalidated table names.
         */
        void notifyByTableNames(String[] tables) {
            Set<String> invalidatedTables = null;
            if (mTableNames.length == 1) {
                for (String table : tables) {
                    if (table.equalsIgnoreCase(mTableNames[0])) {
                        // Optimization for a single-table observer
                        invalidatedTables = mSingleTableSet;
                        break;
                    }
                }
            } else {
                ArraySet<String> set = new ArraySet<>();
                for (String table : tables) {
                    for (String ourTable : mTableNames) {
                        if (ourTable.equalsIgnoreCase(table)) {
                            set.add(ourTable);
                            break;
                        }
                    }
                }
                if (set.size() > 0) {
                    invalidatedTables = set;
                }
            }
            if (invalidatedTables != null) {
                mObserver.onInvalidated(invalidatedTables);
            }
        }
    }

    /**
     * An observer that can listen for changes in the database.
     */
    public abstract static class Observer {
        final String[] mTables;

        /**
         * Observes the given list of tables and views.
         *
         * @param firstTable The name of the table or view.
         * @param rest       More names of tables or views.
         */
        @SuppressWarnings("unused")
        protected Observer(@NonNull String firstTable, String... rest) {
            mTables = Arrays.copyOf(rest, rest.length + 1);
            mTables[rest.length] = firstTable;
        }

        /**
         * Observes the given list of tables and views.
         *
         * @param tables The list of tables or views to observe for changes.
         */
        public Observer(@NonNull String[] tables) {
            // copy tables in case user modifies them afterwards
            mTables = Arrays.copyOf(tables, tables.length);
        }

        /**
         * Called when one of the observed tables is invalidated in the database.
         *
         * @param tables A set of invalidated tables. This is useful when the observer targets
         *               multiple tables and you want to know which table is invalidated. This will
         *               be names of underlying tables when you are observing views.
         */
        public abstract void onInvalidated(@NonNull Set<String> tables);

        boolean isRemote() {
            return false;
        }
    }

    /**
     * Keeps a list of tables we should observe. Invalidation tracker lazily syncs this list w/
     * triggers in the database.
     * <p>
     * This class is thread safe
     */
    static class ObservedTableTracker {
        static final int NO_OP = 0; // don't change trigger state for this table
        static final int ADD = 1; // add triggers for this table
        static final int REMOVE = 2; // remove triggers for this table

        // number of observers per table
        final long[] mTableObservers;
        // trigger state for each table at last sync
        // this field is updated when syncAndGet is called.
        final boolean[] mTriggerStates;
        // when sync is called, this field is returned. It includes actions as ADD, REMOVE, NO_OP
        final int[] mTriggerStateChanges;

        boolean mNeedsSync;

        /**
         * After we return non-null value from getTablesToSync, we expect a onSyncCompleted before
         * returning any non-null value from getTablesToSync.
         * This allows us to workaround any multi-threaded state syncing issues.
         */
        boolean mPendingSync;

        ObservedTableTracker(int tableCount) {
            mTableObservers = new long[tableCount];
            mTriggerStates = new boolean[tableCount];
            mTriggerStateChanges = new int[tableCount];
            Arrays.fill(mTableObservers, 0);
            Arrays.fill(mTriggerStates, false);
        }

        /**
         * @return true if # of triggers is affected.
         */
        boolean onAdded(int... tableIds) {
            boolean needTriggerSync = false;
            synchronized (this) {
                for (int tableId : tableIds) {
                    final long prevObserverCount = mTableObservers[tableId];
                    mTableObservers[tableId] = prevObserverCount + 1;
                    if (prevObserverCount == 0) {
                        mNeedsSync = true;
                        needTriggerSync = true;
                    }
                }
            }
            return needTriggerSync;
        }

        /**
         * @return true if # of triggers is affected.
         */
        boolean onRemoved(int... tableIds) {
            boolean needTriggerSync = false;
            synchronized (this) {
                for (int tableId : tableIds) {
                    final long prevObserverCount = mTableObservers[tableId];
                    mTableObservers[tableId] = prevObserverCount - 1;
                    if (prevObserverCount == 1) {
                        mNeedsSync = true;
                        needTriggerSync = true;
                    }
                }
            }
            return needTriggerSync;
        }

        /**
         * If this returns non-null, you must call onSyncCompleted.
         *
         * @return int[] An int array where the index for each tableId has the action for that
         * table.
         */
        @Nullable
        int[] getTablesToSync() {
            synchronized (this) {
                if (!mNeedsSync || mPendingSync) {
                    return null;
                }
                final int tableCount = mTableObservers.length;
                for (int i = 0; i < tableCount; i++) {
                    final boolean newState = mTableObservers[i] > 0;
                    if (newState != mTriggerStates[i]) {
                        mTriggerStateChanges[i] = newState ? ADD : REMOVE;
                    } else {
                        mTriggerStateChanges[i] = NO_OP;
                    }
                    mTriggerStates[i] = newState;
                }
                mPendingSync = true;
                mNeedsSync = false;
                return mTriggerStateChanges;
            }
        }

        /**
         * if getTablesToSync returned non-null, the called should call onSyncCompleted once it
         * is done.
         */
        void onSyncCompleted() {
            synchronized (this) {
                mPendingSync = false;
            }
        }
    }

    /**
     * An Observer wrapper that keeps a weak reference to the given object.
     * <p>
     * This class will automatically unsubscribe when the wrapped observer goes out of memory.
     */
    static class WeakObserver extends Observer {
        final InvalidationTracker mTracker;
        final WeakReference<Observer> mDelegateRef;

        WeakObserver(InvalidationTracker tracker, Observer delegate) {
            super(delegate.mTables);
            mTracker = tracker;
            mDelegateRef = new WeakReference<>(delegate);
        }

        @Override
        public void onInvalidated(@NonNull Set<String> tables) {
            final Observer observer = mDelegateRef.get();
            if (observer == null) {
                mTracker.removeObserver(this);
            } else {
                observer.onInvalidated(tables);
            }
        }
    }
}
