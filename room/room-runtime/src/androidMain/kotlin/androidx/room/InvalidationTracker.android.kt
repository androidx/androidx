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

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.arch.core.internal.SafeIterableMap
import androidx.lifecycle.LiveData
import androidx.room.InvalidationTracker.Observer
import androidx.room.Room.LOG_TAG
import androidx.room.concurrent.ifNotClosed
import androidx.room.driver.SupportSQLiteConnection
import androidx.room.support.AutoCloser
import androidx.room.util.useCursor
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import java.lang.ref.WeakReference
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The invalidation tracker keeps track of modified tables by queries and notifies its registered
 * [Observer]s about such modifications.
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
actual open class InvalidationTracker
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
actual constructor(
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

    private val observedTableTracker = ObservedTableStates(tableNames.size)

    private val invalidationLiveDataContainer: InvalidationLiveDataContainer =
        InvalidationLiveDataContainer(database)

    @GuardedBy("observerMap")
    internal val observerMap = SafeIterableMap<Observer, ObserverWrapper>()

    private var multiInstanceInvalidationClient: MultiInstanceInvalidationClient? = null

    private val syncTriggersLock = Any()

    private val trackerLock = Any()

    /** The initialization state for restarting invalidation after auto-close. */
    private var multiInstanceClientInitState: MultiInstanceClientInitState? = null

    init {
        tableIdLookup = mutableMapOf()
        tablesNames = Array(tableNames.size) { id ->
            val tableName = tableNames[id].lowercase()
            tableIdLookup[tableName] = id
            val shadowTableName = shadowTablesMap[tableNames[id]]?.lowercase()
            shadowTableName ?: tableName
        }

        // Adjust table id lookup for those tables whose shadow table is another already mapped
        // table (e.g. external content fts tables).
        shadowTablesMap.forEach { entry ->
            val shadowTableName = entry.value.lowercase()
            if (tableIdLookup.containsKey(shadowTableName)) {
                val tableName = entry.key.lowercase()
                tableIdLookup[tableName] = tableIdLookup.getValue(shadowTableName)
            }
        }
    }

    /**
     * Used by the generated code.
     *
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
     */
    internal actual fun internalInit(connection: SQLiteConnection) {
        if (connection is SupportSQLiteConnection) {
            @Suppress("DEPRECATION")
            internalInit(connection.db)
        } else {
            Log.e(LOG_TAG, "Invalidation tracker is disabled due to lack of driver " +
                "support. - b/309990302")
        }
    }

    /**
     * Internal method to initialize table tracking.
     */
    @Deprecated("No longer called by generated code")
    internal fun internalInit(database: SupportSQLiteDatabase) {
        synchronized(trackerLock) {
            if (initialized) {
                Log.e(LOG_TAG, "Invalidation tracker is initialized twice :/.")
                return
            }

            multiInstanceClientInitState?.let {
                // Start multi-instance invalidation, based in info from the saved initState.
                startMultiInstanceInvalidation()
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
            val isObserverMapEmpty = observerMap.filterNot { it.key.isRemote }.isEmpty()
            if (multiInstanceInvalidationClient != null && isObserverMapEmpty) {
                stopMultiInstanceInvalidation()
            }
            initialized = false
            observedTableTracker.resetTriggerState()
            cleanupStatement?.close()
        }
    }

    private fun startMultiInstanceInvalidation() {
        val state = checkNotNull(multiInstanceClientInitState)
        multiInstanceInvalidationClient = MultiInstanceInvalidationClient(
            context = state.context,
            name = state.name,
            serviceIntent = state.serviceIntent,
            invalidationTracker = this,
            executor = database.queryExecutor
        )
    }

    private fun stopMultiInstanceInvalidation() {
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
    @WorkerThread
    open fun addObserver(observer: Observer) {
        val tableNames = resolveViews(observer.tables)
        val tableIds = tableNames.map { tableName ->
            tableIdLookup[tableName.lowercase()]
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
        if (currentObserver == null && observedTableTracker.onObserverAdded(tableIds)) {
            syncTriggers()
        }
    }

    private fun validateAndResolveTableNames(tableNames: Array<out String>): Array<out String> {
        val resolved = resolveViews(tableNames)
        resolved.forEach { tableName ->
            require(tableIdLookup.containsKey(tableName.lowercase())) {
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
                if (viewTables.containsKey(name.lowercase())) {
                    addAll(viewTables[name.lowercase()]!!)
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
    @WorkerThread
    open fun removeObserver(observer: Observer) {
        val wrapper = synchronized(observerMap) {
            observerMap.remove(observer)
        }
        if (wrapper != null && observedTableTracker.onObserverRemoved(wrapper.tableIds)) {
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
        override fun run() = database.closeBarrier.ifNotClosed {
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
                    autoCloser?.decrementCountAndScheduleClose()
                }

            if (invalidatedTableIds.isNotEmpty()) {
                synchronized(observerMap) {
                    observerMap.forEach {
                        it.value.notifyByTableIds(invalidatedTableIds)
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
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun notifyObserversByTableNames(vararg tables: String) {
        val tableNames = setOf(*tables)
        synchronized(observerMap) {
            observerMap.forEach { (observer, wrapper) ->
                if (!observer.isRemote) {
                    wrapper.notifyByTableNames(tableNames)
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
            this.database.closeBarrier.ifNotClosed {
                // Serialize adding and removing table trackers, this is specifically important
                // to avoid missing invalidation before a transaction starts but there are
                // pending (possibly concurrent) observer changes.
                synchronized(syncTriggersLock) {
                    val tablesToSync = observedTableTracker.getTablesToSync() ?: return
                    beginTransactionInternal(database)
                    try {
                        tablesToSync.forEachIndexed { tableId, syncState ->
                            when (syncState) {
                                ObservedTableStates.ObserveOp.NO_OP -> {}
                                ObservedTableStates.ObserveOp.ADD ->
                                    startTrackingTable(database, tableId)
                                ObservedTableStates.ObserveOp.REMOVE ->
                                    stopTrackingTable(database, tableId)
                            }
                        }
                        database.setTransactionSuccessful()
                    } finally {
                        database.endTransaction()
                    }
                }
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

    internal fun initMultiInstanceInvalidation(
        context: Context,
        name: String,
        serviceIntent: Intent
    ) {
        multiInstanceClientInitState = MultiInstanceClientInitState(
            context = context,
            name = name,
            serviceIntent = serviceIntent
        )
    }

    internal fun stop() {
        stopMultiInstanceInvalidation()
    }

    /**
     * An observer that can listen for changes in the database by subscribing to an
     * [InvalidationTracker].
     *
     * @param tables The names of the tables this observer is interested in getting notified if
     * they are modified.
     */
    actual abstract class Observer actual constructor(
        internal actual val tables: Array<out String>
    ) {
        /**
         * Creates an observer for the given tables and views.
         *
         * @param firstTable The name of the table or view.
         * @param rest       More names of tables or views.
         */
        protected actual constructor(
            firstTable: String,
            vararg rest: String
        ) : this(arrayOf(firstTable, *rest))

        /**
         * Invoked when one of the observed tables is invalidated (changed).
         *
         * @param tables A set of invalidated tables. When the observer is interested in multiple
         * tables, this set can be used to distinguish which of the observed tables were
         * invalidated. When observing a database view the names of underlying tables will be in
         * the set instead of the view name.
         */
        actual abstract fun onInvalidated(tables: Set<String>)

        internal open val isRemote: Boolean
            get() = false
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
            if (database.isWriteAheadLoggingEnabled) {
                database.beginTransactionNonExclusive()
            } else {
                database.beginTransaction()
            }
        }
    }
}

/**
 * Stores needed info to restart the invalidation after it was auto-closed.
 */
internal data class MultiInstanceClientInitState(
    val context: Context,
    val name: String,
    val serviceIntent: Intent
)
