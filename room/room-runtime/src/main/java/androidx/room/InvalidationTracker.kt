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
package androidx.room

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.Build
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.arch.core.internal.SafeIterableMap
import androidx.lifecycle.LiveData
import androidx.room.Room.LOG_TAG
import androidx.room.util.useCursor
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import java.lang.ref.WeakReference
import java.util.Arrays
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean

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
open class InvalidationTracker @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) constructor(
    internal val database: RoomDatabase,
    private val shadowTablesMap: Map<String, String>,
    private val viewTables: Map<String, @JvmSuppressWildcards Set<String>>,
    vararg tableNames: String
) {
    internal val tableIdLookup: Map<String, Int>
    internal val tablesNames: Array<out String>

    private var autoCloser: AutoCloser? = null

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @field:RestrictTo(RestrictTo.Scope.LIBRARY)
    val pendingRefresh = AtomicBoolean(false)

    @Volatile
    private var initialized = false

    @Volatile
    internal var cleanupStatement: SupportSQLiteStatement? = null

    private val observedTableTracker: ObservedTableTracker = ObservedTableTracker(tableNames.size)

    private val invalidationLiveDataContainer: InvalidationLiveDataContainer =
        InvalidationLiveDataContainer(database)

    @GuardedBy("observerMap")
    internal val observerMap = SafeIterableMap<Observer, ObserverWrapper>()

    private var multiInstanceInvalidationClient: MultiInstanceInvalidationClient? = null

    private val syncTriggersLock = Any()

    private val trackerLock = Any()

    init {
        tableIdLookup = mutableMapOf()
        tablesNames = Array(tableNames.size) { id ->
            val tableName = tableNames[id].lowercase(Locale.US)
            tableIdLookup[tableName] = id
            val shadowTableName = shadowTablesMap[tableNames[id]]?.lowercase(Locale.US)
            shadowTableName ?: tableName
        }

        // Adjust table id lookup for those tables whose shadow table is another already mapped
        // table (e.g. external content fts tables).
        shadowTablesMap.forEach { entry ->
            val shadowTableName = entry.value.lowercase(Locale.US)
            if (tableIdLookup.containsKey(shadowTableName)) {
                val tableName = entry.key.lowercase(Locale.US)
                tableIdLookup[tableName] = tableIdLookup.getValue(shadowTableName)
            }
        }
    }

    /**
     * Used by the generated code.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    constructor(database: RoomDatabase, vararg tableNames: String) :
        this(
            database = database,
            shadowTablesMap = emptyMap(),
            viewTables = emptyMap(),
            tableNames = tableNames
        )

    /**
     * Sets the auto closer for this invalidation tracker so that the invalidation tracker can
     * ensure that the database is not closed if there are pending invalidations that haven't yet
     * been flushed.
     *
     * This also adds a callback to the autocloser to ensure that the InvalidationTracker is in
     * an ok state once the table is invalidated.
     *
     * This must be called before the database is used.
     *
     * @param autoCloser the autocloser associated with the db
     */
    internal fun setAutoCloser(autoCloser: AutoCloser) {
        this.autoCloser = autoCloser
        autoCloser.setAutoCloseCallback(::onAutoCloseCallback)
    }

    /**
     * Internal method to initialize table tracking.
     *
     * You should never call this method, it is called by the generated code.
     */
    internal fun internalInit(database: SupportSQLiteDatabase) {
        synchronized(trackerLock) {
            if (initialized) {
                Log.e(LOG_TAG, "Invalidation tracker is initialized twice :/.")
                return
            }

            // These actions are not in a transaction because temp_store is not allowed to be
            // performed on a transaction, and recursive_triggers is not affected by transactions.
            database.execSQL("PRAGMA temp_store = MEMORY;")
            database.execSQL("PRAGMA recursive_triggers='ON';")
            database.execSQL(CREATE_TRACKING_TABLE_SQL)
            syncTriggers(database)
            cleanupStatement = database.compileStatement(RESET_UPDATED_TABLES_SQL)
            initialized = true
        }
    }

    private fun onAutoCloseCallback() {
        synchronized(trackerLock) {
            initialized = false
            observedTableTracker.resetTriggerState()
            cleanupStatement?.close()
        }
    }

    internal fun startMultiInstanceInvalidation(
        context: Context,
        name: String,
        serviceIntent: Intent
    ) {
        multiInstanceInvalidationClient = MultiInstanceInvalidationClient(
            context = context,
            name = name,
            serviceIntent = serviceIntent,
            invalidationTracker = this,
            executor = database.queryExecutor
        )
    }

    internal fun stopMultiInstanceInvalidation() {
        multiInstanceInvalidationClient?.stop()
        multiInstanceInvalidationClient = null
    }

    private fun stopTrackingTable(db: SupportSQLiteDatabase, tableId: Int) {
        val tableName = tablesNames[tableId]
        for (trigger in TRIGGERS) {
            val sql = buildString {
                append("DROP TRIGGER IF EXISTS ")
                append(getTriggerName(tableName, trigger))
            }
            db.execSQL(sql)
        }
    }

    private fun startTrackingTable(db: SupportSQLiteDatabase, tableId: Int) {
        db.execSQL(
            "INSERT OR IGNORE INTO $UPDATE_TABLE_NAME VALUES($tableId, 0)"
        )
        val tableName = tablesNames[tableId]
        for (trigger in TRIGGERS) {
            val sql = buildString {
                append("CREATE TEMP TRIGGER IF NOT EXISTS ")
                append(getTriggerName(tableName, trigger))
                append(" AFTER ")
                append(trigger)
                append(" ON `")
                append(tableName)
                append("` BEGIN UPDATE ")
                append(UPDATE_TABLE_NAME)
                append(" SET ").append(INVALIDATED_COLUMN_NAME)
                append(" = 1")
                append(" WHERE ").append(TABLE_ID_COLUMN_NAME)
                append(" = ").append(tableId)
                append(" AND ").append(INVALIDATED_COLUMN_NAME)
                append(" = 0")
                append("; END")
            }
            db.execSQL(sql)
        }
    }

    /**
     * Adds the given observer to the observers list and it will be notified if any table it
     * observes changes.
     *
     * Database changes are pulled on another thread so in some race conditions, the observer might
     * be invoked for changes that were done before it is added.
     *
     * If the observer already exists, this is a no-op call.
     *
     * If one of the tables in the Observer does not exist in the database, this method throws an
     * [IllegalArgumentException].
     *
     * This method should be called on a background/worker thread as it performs database
     * operations.
     *
     * @param observer The observer which listens the database for changes.
     */
    @SuppressLint("RestrictedApi")
    @WorkerThread
    open fun addObserver(observer: Observer) {
        val tableNames = resolveViews(observer.tables)
        val tableIds = tableNames.map { tableName ->
            tableIdLookup[tableName.lowercase(Locale.US)]
                ?: throw IllegalArgumentException("There is no table with name $tableName")
        }.toIntArray()

        val wrapper = ObserverWrapper(
            observer = observer,
            tableIds = tableIds,
            tableNames = tableNames
        )

        val currentObserver = synchronized(observerMap) {
            observerMap.putIfAbsent(observer, wrapper)
        }
        if (currentObserver == null && observedTableTracker.onAdded(*tableIds)) {
            syncTriggers()
        }
    }

    private fun validateAndResolveTableNames(tableNames: Array<out String>): Array<out String> {
        val resolved = resolveViews(tableNames)
        resolved.forEach { tableName ->
            require(tableIdLookup.containsKey(tableName.lowercase(Locale.US))) {
                "There is no table with name $tableName"
            }
        }
        return resolved
    }

    /**
     * Resolves the list of tables and views into a list of unique tables that are underlying them.
     *
     * @param names The names of tables or views.
     * @return The names of the underlying tables.
     */
    private fun resolveViews(names: Array<out String>): Array<out String> {
        return buildSet {
            names.forEach { name ->
                if (viewTables.containsKey(name.lowercase(Locale.US))) {
                    addAll(viewTables[name.lowercase(Locale.US)]!!)
                } else {
                    add(name)
                }
            }
        }.toTypedArray()
    }

    /**
     * Adds an observer but keeps a weak reference back to it.
     *
     * Note that you cannot remove this observer once added. It will be automatically removed
     * when the observer is GC'ed.
     *
     * @param observer The observer to which InvalidationTracker will keep a weak reference.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    open fun addWeakObserver(observer: Observer) {
        addObserver(WeakObserver(this, observer))
    }

    /**
     * Removes the observer from the observers list.
     *
     * This method should be called on a background/worker thread as it performs database
     * operations.
     *
     * @param observer The observer to remove.
     */
    @SuppressLint("RestrictedApi")
    @WorkerThread
    open fun removeObserver(observer: Observer) {
        val wrapper = synchronized(observerMap) {
            observerMap.remove(observer)
        }
        if (wrapper != null && observedTableTracker.onRemoved(tableIds = wrapper.tableIds)) {
            syncTriggers()
        }
    }

    internal fun ensureInitialization(): Boolean {
        if (!database.isOpenInternal) {
            return false
        }
        if (!initialized) {
            // trigger initialization
            database.openHelper.writableDatabase
        }
        if (!initialized) {
            Log.e(LOG_TAG, "database is not initialized even though it is open")
            return false
        }
        return true
    }

    @VisibleForTesting
    @JvmField
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    val refreshRunnable: Runnable = object : Runnable {
        override fun run() {
            val closeLock = database.getCloseLock()
            closeLock.lock()
            val invalidatedTableIds: Set<Int> =
                try {
                    if (!ensureInitialization()) {
                        return
                    }
                    if (!pendingRefresh.compareAndSet(true, false)) {
                        // no pending refresh
                        return
                    }
                    if (database.inTransaction()) {
                        // current thread is in a transaction. when it ends, it will invoke
                        // refreshRunnable again. pendingRefresh is left as false on purpose
                        // so that the last transaction can flip it on again.
                        return
                    }

                    // This transaction has to be on the underlying DB rather than the RoomDatabase
                    // in order to avoid a recursive loop after endTransaction.
                    val db = database.openHelper.writableDatabase
                    db.beginTransactionNonExclusive()
                    val invalidatedTableIds: Set<Int>
                    try {
                        invalidatedTableIds = checkUpdatedTable()
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                    invalidatedTableIds
                } catch (ex: IllegalStateException) {
                    // may happen if db is closed. just log.
                    Log.e(
                        LOG_TAG, "Cannot run invalidation tracker. Is the db closed?",
                        ex
                    )
                    emptySet()
                } catch (ex: SQLiteException) {
                    Log.e(
                        LOG_TAG, "Cannot run invalidation tracker. Is the db closed?",
                        ex
                    )
                    emptySet()
                } finally {
                    closeLock.unlock()
                    autoCloser?.decrementCountAndScheduleClose()
                }

            if (invalidatedTableIds.isNotEmpty()) {
                synchronized(observerMap) {
                    observerMap.forEach {
                        it.value.notifyByTableInvalidStatus(invalidatedTableIds)
                    }
                }
            }
        }

        private fun checkUpdatedTable(): Set<Int> {
            val invalidatedTableIds = buildSet {
                database.query(SimpleSQLiteQuery(SELECT_UPDATED_TABLES_SQL)).useCursor { cursor ->
                    while (cursor.moveToNext()) {
                        add(cursor.getInt(0))
                    }
                }
            }
            if (invalidatedTableIds.isNotEmpty()) {
                checkNotNull(cleanupStatement)
                val statement = cleanupStatement
                requireNotNull(statement)
                statement.executeUpdateDelete()
            }
            return invalidatedTableIds
        }
    }

    /**
     * Enqueues a task to refresh the list of updated tables.
     *
     * This method is automatically called when [RoomDatabase.endTransaction] is called but
     * if you have another connection to the database or directly use [ ], you may need to call this
     * manually.
     */
    open fun refreshVersionsAsync() {
        // TODO we should consider doing this sync instead of async.
        if (pendingRefresh.compareAndSet(false, true)) {
            // refreshVersionsAsync is called with the ref count incremented from
            // RoomDatabase, so the db can't be closed here, but we need to be sure that our
            // db isn't closed until refresh is completed. This increment call must be
            // matched with a corresponding call in refreshRunnable.
            autoCloser?.incrementCountAndEnsureDbIsOpen()
            database.queryExecutor.execute(refreshRunnable)
        }
    }

    /**
     * Check versions for tables, and run observers synchronously if tables have been updated.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @WorkerThread
    open fun refreshVersionsSync() {
        // This increment call must be matched with a corresponding call in refreshRunnable.
        autoCloser?.incrementCountAndEnsureDbIsOpen()
        syncTriggers()
        refreshRunnable.run()
    }

    /**
     * Notifies all the registered [Observer]s of table changes.
     *
     * This can be used for notifying invalidation that cannot be detected by this
     * [InvalidationTracker], for example, invalidation from another process.
     *
     * @param tables The invalidated tables.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun notifyObserversByTableNames(vararg tables: String) {
        synchronized(observerMap) {
            observerMap.forEach { (observer, wrapper) ->
                if (!observer.isRemote) {
                    wrapper.notifyByTableNames(tables)
                }
            }
        }
    }

    internal fun syncTriggers(database: SupportSQLiteDatabase) {
        if (database.inTransaction()) {
            // we won't run this inside another transaction.
            return
        }
        try {
            val closeLock = this.database.getCloseLock()
            closeLock.lock()
            try {
                // Serialize adding and removing table trackers, this is specifically important
                // to avoid missing invalidation before a transaction starts but there are
                // pending (possibly concurrent) observer changes.
                synchronized(syncTriggersLock) {
                    val tablesToSync = observedTableTracker.getTablesToSync() ?: return
                    beginTransactionInternal(database)
                    try {
                        tablesToSync.forEachIndexed { tableId, syncState ->
                            when (syncState) {
                                ObservedTableTracker.ADD ->
                                    startTrackingTable(database, tableId)
                                ObservedTableTracker.REMOVE ->
                                    stopTrackingTable(database, tableId)
                            }
                        }
                        database.setTransactionSuccessful()
                    } finally {
                        database.endTransaction()
                    }
                }
            } finally {
                closeLock.unlock()
            }
        } catch (ex: IllegalStateException) {
            // may happen if db is closed. just log.
            Log.e(LOG_TAG, "Cannot run invalidation tracker. Is the db closed?", ex)
        } catch (ex: SQLiteException) {
            Log.e(LOG_TAG, "Cannot run invalidation tracker. Is the db closed?", ex)
        }
    }

    /**
     * Called by RoomDatabase before each beginTransaction call.
     *
     * It is important that pending trigger changes are applied to the database before any query
     * runs. Otherwise, we may miss some changes.
     *
     * This api should eventually be public.
     */
    internal fun syncTriggers() {
        if (!database.isOpenInternal) {
            return
        }
        syncTriggers(database.openHelper.writableDatabase)
    }

    /**
     * Creates a LiveData that computes the given function once and for every other invalidation
     * of the database.
     *
     * Holds a strong reference to the created LiveData as long as it is active.
     *
     * @param computeFunction The function that calculates the value
     * @param tableNames      The list of tables to observe
     * @param T             The return type
     * @return A new LiveData that computes the given function when the given list of tables
     * invalidates.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Deprecated("Use [createLiveData(String[], boolean, Callable)]")
    open fun <T> createLiveData(
        tableNames: Array<out String>,
        computeFunction: Callable<T?>
    ): LiveData<T> {
        return createLiveData(tableNames, false, computeFunction)
    }

    /**
     * Creates a LiveData that computes the given function once and for every other invalidation
     * of the database.
     *
     * Holds a strong reference to the created LiveData as long as it is active.
     *
     * @param tableNames      The list of tables to observe
     * @param inTransaction   True if the computeFunction will be done in a transaction, false
     * otherwise.
     * @param computeFunction The function that calculates the value
     * @param T             The return type
     * @return A new LiveData that computes the given function when the given list of tables
     * invalidates.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    open fun <T> createLiveData(
        tableNames: Array<out String>,
        inTransaction: Boolean,
        computeFunction: Callable<T?>
    ): LiveData<T> {
        return invalidationLiveDataContainer.create(
            validateAndResolveTableNames(tableNames), inTransaction, computeFunction
        )
    }

    /**
     * Wraps an observer and keeps the table information.
     *
     * Internally table ids are used which may change from database to database so the table
     * related information is kept here rather than in the Observer.
     */
    internal class ObserverWrapper(
        internal val observer: Observer,
        internal val tableIds: IntArray,
        private val tableNames: Array<out String>
    ) {
        private val singleTableSet = if (tableNames.isNotEmpty()) {
            setOf(tableNames[0])
        } else {
            emptySet()
        }

        init {
            check(tableIds.size == tableNames.size)
        }

        /**
         * Notifies the underlying [.mObserver] if any of the observed tables are invalidated
         * based on the given invalid status set.
         *
         * @param invalidatedTablesIds The table ids of the tables that are invalidated.
         */
        internal fun notifyByTableInvalidStatus(invalidatedTablesIds: Set<Int?>) {
            val invalidatedTables = when (tableIds.size) {
                0 -> emptySet()
                1 -> if (invalidatedTablesIds.contains(tableIds[0])) {
                    singleTableSet // Optimization for a single-table observer
                } else {
                    emptySet()
                }
                else -> buildSet {
                    tableIds.forEachIndexed { idx, tableId ->
                        if (invalidatedTablesIds.contains(tableId)) {
                            add(tableNames[idx])
                        }
                    }
                }
            }

            if (invalidatedTables.isNotEmpty()) {
                observer.onInvalidated(invalidatedTables)
            }
        }

        /**
         * Notifies the underlying [.mObserver] if it observes any of the specified
         * `tables`.
         *
         * @param tables The invalidated table names.
         */
        internal fun notifyByTableNames(tables: Array<out String>) {
            val invalidatedTables = when (tableNames.size) {
                0 -> emptySet()
                1 -> if (tables.any { it.equals(tableNames[0], ignoreCase = true) }) {
                    singleTableSet // Optimization for a single-table observer
                } else {
                    emptySet()
                }
                else -> buildSet {
                    tables.forEach { table ->
                        tableNames.forEach ourTablesLoop@{ ourTable ->
                            if (ourTable.equals(table, ignoreCase = true)) {
                                add(ourTable)
                                return@ourTablesLoop
                            }
                        }
                    }
                }
            }
            if (invalidatedTables.isNotEmpty()) {
                observer.onInvalidated(invalidatedTables)
            }
        }
    }

    /**
     * An observer that can listen for changes in the database.
     */
    abstract class Observer(internal val tables: Array<out String>) {
        /**
         * Observes the given list of tables and views.
         *
         * @param firstTable The name of the table or view.
         * @param rest       More names of tables or views.
         */
        protected constructor(firstTable: String, vararg rest: String) : this(
            buildList {
                addAll(rest)
                add(firstTable)
            }.toTypedArray()
        )

        /**
         * Called when one of the observed tables is invalidated in the database.
         *
         * @param tables A set of invalidated tables. This is useful when the observer targets
         * multiple tables and you want to know which table is invalidated. This will
         * be names of underlying tables when you are observing views.
         */
        abstract fun onInvalidated(tables: Set<String>)

        internal open val isRemote: Boolean
            get() = false
    }

    /**
     * Keeps a list of tables we should observe. Invalidation tracker lazily syncs this list w/
     * triggers in the database.
     *
     * This class is thread safe
     */
    internal class ObservedTableTracker(tableCount: Int) {
        // number of observers per table
        val tableObservers = LongArray(tableCount)

        // trigger state for each table at last sync
        // this field is updated when syncAndGet is called.
        private val triggerStates = BooleanArray(tableCount)

        // when sync is called, this field is returned. It includes actions as ADD, REMOVE, NO_OP
        private val triggerStateChanges = IntArray(tableCount)

        var needsSync = false

        /**
         * @return true if # of triggers is affected.
         */
        fun onAdded(vararg tableIds: Int): Boolean {
            var needTriggerSync = false
            synchronized(this) {
                tableIds.forEach { tableId ->
                    val prevObserverCount = tableObservers[tableId]
                    tableObservers[tableId] = prevObserverCount + 1
                    if (prevObserverCount == 0L) {
                        needsSync = true
                        needTriggerSync = true
                    }
                }
            }
            return needTriggerSync
        }

        /**
         * @return true if # of triggers is affected.
         */
        fun onRemoved(vararg tableIds: Int): Boolean {
            var needTriggerSync = false
            synchronized(this) {
                tableIds.forEach { tableId ->
                    val prevObserverCount = tableObservers[tableId]
                    tableObservers[tableId] = prevObserverCount - 1
                    if (prevObserverCount == 1L) {
                        needsSync = true
                        needTriggerSync = true
                    }
                }
            }
            return needTriggerSync
        }

        /**
         * If we are re-opening the db we'll need to add all the triggers that we need so change
         * the current state to false for all.
         */
        fun resetTriggerState() {
            synchronized(this) {
                Arrays.fill(triggerStates, false)
                needsSync = true
            }
        }

        /**
         * If this returns non-null, you must call onSyncCompleted.
         *
         * @return int[] An int array where the index for each tableId has the action for that
         * table.
         */
        @VisibleForTesting
        @JvmName("getTablesToSync")
        fun getTablesToSync(): IntArray? {
            synchronized(this) {
                if (!needsSync) {
                    return null
                }
                tableObservers.forEachIndexed { i, observerCount ->
                    val newState = observerCount > 0
                    if (newState != triggerStates[i]) {
                        triggerStateChanges[i] = if (newState) ADD else REMOVE
                    } else {
                        triggerStateChanges[i] = NO_OP
                    }
                    triggerStates[i] = newState
                }
                needsSync = false
                return triggerStateChanges.clone()
            }
        }

        internal companion object {
            const val NO_OP = 0 // don't change trigger state for this table
            const val ADD = 1 // add triggers for this table
            const val REMOVE = 2 // remove triggers for this table
        }
    }

    /**
     * An Observer wrapper that keeps a weak reference to the given object.
     *
     * This class will automatically unsubscribe when the wrapped observer goes out of memory.
     */
    internal class WeakObserver(
        val tracker: InvalidationTracker,
        delegate: Observer
    ) : Observer(delegate.tables) {
        val delegateRef: WeakReference<Observer> = WeakReference(delegate)
        override fun onInvalidated(tables: Set<String>) {
            val observer = delegateRef.get()
            if (observer == null) {
                tracker.removeObserver(this)
            } else {
                observer.onInvalidated(tables)
            }
        }
    }

    companion object {
        private val TRIGGERS = arrayOf("UPDATE", "DELETE", "INSERT")
        private const val UPDATE_TABLE_NAME = "room_table_modification_log"
        private const val TABLE_ID_COLUMN_NAME = "table_id"
        private const val INVALIDATED_COLUMN_NAME = "invalidated"
        private const val CREATE_TRACKING_TABLE_SQL =
            "CREATE TEMP TABLE $UPDATE_TABLE_NAME ($TABLE_ID_COLUMN_NAME INTEGER PRIMARY KEY, " +
                "$INVALIDATED_COLUMN_NAME INTEGER NOT NULL DEFAULT 0)"

        @VisibleForTesting
        internal const val RESET_UPDATED_TABLES_SQL =
            "UPDATE $UPDATE_TABLE_NAME SET $INVALIDATED_COLUMN_NAME = 0 " +
                "WHERE $INVALIDATED_COLUMN_NAME = 1"

        @VisibleForTesting
        internal const val SELECT_UPDATED_TABLES_SQL =
            "SELECT * FROM $UPDATE_TABLE_NAME WHERE $INVALIDATED_COLUMN_NAME = 1;"

        internal fun getTriggerName(
            tableName: String,
            triggerType: String
        ) = "`room_table_modification_trigger_${tableName}_$triggerType`"

        internal fun beginTransactionInternal(database: SupportSQLiteDatabase) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                database.isWriteAheadLoggingEnabled
            ) {
                database.beginTransactionNonExclusive()
            } else {
                database.beginTransaction()
            }
        }
    }
}