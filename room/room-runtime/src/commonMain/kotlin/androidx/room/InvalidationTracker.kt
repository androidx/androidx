/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import androidx.room.InvalidationTracker.Observer
import androidx.room.Transactor.SQLiteTransactionType
import androidx.room.concurrent.ifNotClosed
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteException
import androidx.sqlite.execSQL
import androidx.sqlite.use
import kotlin.jvm.JvmSuppressWildcards
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.launch

/**
 * The invalidation tracker keeps track of tables modified by queries and notifies its subscribed
 * [Observer]s about such modifications.
 *
 * [Observer]s contain one or more tables and are added to the tracker via [subscribe]. Once an
 * observer is subscribed, if a database operation changes one of the tables the observer is
 * subscribed to, then such table is considered 'invalidated' and [Observer.onInvalidated] will be
 * invoked on the observer. If an observer is no longer interested in tracking modifications it can
 * be removed via [unsubscribe].
 */
expect class InvalidationTracker
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
constructor(
    database: RoomDatabase,
    shadowTablesMap: Map<String, String>,
    viewTables: Map<String, @JvmSuppressWildcards Set<String>>,
    vararg tableNames: String
) {
    /** Internal method to initialize tracker for a given connection. Invoked by generated code. */
    internal fun internalInit(connection: SQLiteConnection)

    /**
     * Subscribes the given [observer] with the tracker such that it is notified if any table it is
     * interested on changes.
     *
     * If the observer is already subscribed, then this function does nothing.
     *
     * @param observer The observer that will listen for database changes.
     * @throws IllegalArgumentException if one of the tables in the observer does not exist.
     */
    // TODO(b/329315924): Replace with Flow based API
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) suspend fun subscribe(observer: Observer)

    /**
     * Unsubscribes the given [observer] from the tracker.
     *
     * If the observer was never subscribed in the first place, then this function does nothing.
     *
     * @param observer The observer to remove.
     */
    // TODO(b/329315924): Replace with Flow based API
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) suspend fun unsubscribe(observer: Observer)

    /**
     * Synchronize subscribed observers with their tables.
     *
     * This function should be called before any write operation is performed on the database so
     * that a tracking link is created between observers and its interest tables.
     *
     * @see refreshAsync
     */
    internal suspend fun sync()

    /**
     * Refresh subscribed observers asynchronously, invoking [Observer.onInvalidated] on those whose
     * tables have been invalidated.
     *
     * This function should be called after any write operation is performed on the database, such
     * that tracked tables and its associated observers are notified if invalidated.
     */
    fun refreshAsync()

    /** Stops invalidation tracker operations. */
    internal fun stop()

    /**
     * An observer that can listen for changes in the database by subscribing to an
     * [InvalidationTracker].
     *
     * @param tables The names of the tables this observer is interested in getting notified if they
     *   are modified.
     */
    abstract class Observer(tables: Array<out String>) {

        internal val tables: Array<out String>

        /**
         * Creates an observer for the given tables and views.
         *
         * @param firstTable The name of the table or view.
         * @param rest More names of tables or views.
         */
        protected constructor(firstTable: String, vararg rest: String)

        /**
         * Invoked when one of the observed tables is invalidated (changed).
         *
         * @param tables A set of invalidated tables. When the observer is interested in multiple
         *   tables, this set can be used to distinguish which of the observed tables were
         *   invalidated. When observing a database view the names of underlying tables will be in
         *   the set instead of the view name.
         */
        abstract fun onInvalidated(tables: Set<String>)
    }
}

/**
 * A TRIGGER based implementation of an invalidation tracker.
 *
 * Some details on how this tracker works:
 * * An in-memory table is created with two columns, 'table_id' and 'invalidated' to known which
 *   table has been modified.
 * * [ObservedTableStates] keeps the 'observed' state of each table helping the tracker know which
 *   tables should be watched (via an installed trigger) based on the number of observers
 *   interested.
 * * Before a write transaction, Room will sync triggers by invoking [InvalidationTracker.sync].
 * * If in the write transaction a table was modified, the installed trigger will flip the table's
 *   invalidated column in the in-memory table to ON.
 * * After a write transaction, Room will check the invalidated rows by invoking
 *   [InvalidationTracker.refreshAsync], notifying observers if necessary.
 */
internal class TriggerBasedInvalidationTracker(
    private val database: RoomDatabase,
    // Table to shadow / content table names
    private val shadowTablesMap: Map<String, String>,
    // View to underlying table names
    private val viewTables: Map<String, Set<String>>,
    tableNames: Array<out String>
) {
    /** Table name (lowercase) to index (id) in [tablesNames], used as a quick lookup map. */
    private val tableIdLookup: Map<String, Int>
    /** Table names (lowercase), the index is at which a table is in the array is its 'id'. */
    private val tablesNames: Array<String>

    private val observerMap: MutableMap<Observer, ObserverWrapper>
    private val observerMapLock = reentrantLock()
    private val observedTableStates: ObservedTableStates

    /**
     * Whether there is a pending [refreshInvalidation] to be done or not. Since a refresh can be
     * queue to be done asynchronously, this flag is used to control excessive scheduling of
     * refreshes.
     */
    private val pendingRefresh = atomic(false)

    /** Callback to allow or disallow [refreshInvalidation] from proceeding. */
    internal var onAllowRefresh: () -> Boolean = { true }

    init {
        tableIdLookup = mutableMapOf()
        tablesNames =
            Array(tableNames.size) { id ->
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

        observerMap = mutableMapOf()
        observedTableStates = ObservedTableStates(tablesNames.size)
    }

    /**
     * Configure a connection. All connections open by Room should be configured by the tracker even
     * though the one we really care about is the single write connection.
     */
    fun configureConnection(connection: SQLiteConnection) {
        val isReadConnection =
            connection.prepare("PRAGMA query_only").use {
                it.step()
                it.getBoolean(0)
            }
        if (!isReadConnection) {
            connection.execSQL("PRAGMA temp_store = MEMORY")
            connection.execSQL("PRAGMA recursive_triggers = 1")
            connection.execSQL(CREATE_TRACKING_TABLE_SQL)
            // When a connection is configured the temporary triggers need to be synced since it is
            // possible that a new write connection is being configured because a previous one
            // was lost along with its installed triggers.
            observedTableStates.forceNeedSync()
        }
    }

    /**
     * Add an observer, sync triggers and return true if it was actually added, or false if already
     * added.
     */
    internal suspend fun addObserver(observer: Observer): Boolean {
        val shouldSync = addObserverOnly(observer)
        if (shouldSync) {
            syncTriggers()
        }
        return shouldSync
    }

    /** Add an observer and return true if it was actually added, or false if already added. */
    internal fun addObserverOnly(observer: Observer): Boolean {
        val (resolvedTableNames, tableIds) = validateTableNames(observer.tables)
        val wrapper =
            ObserverWrapper(
                observer = observer,
                tableIds = tableIds,
                tableNames = resolvedTableNames
            )

        val currentObserver =
            observerMapLock.withLock {
                if (observerMap.containsKey(observer)) {
                    observerMap.getValue(observer)
                } else {
                    observerMap.put(observer, wrapper)
                }
            }
        return currentObserver == null && observedTableStates.onObserverAdded(tableIds)
    }

    /**
     * Removes an observer, sync triggers and return true if it was actually removed, or false if it
     * was not found.
     */
    internal suspend fun removeObserver(observer: Observer): Boolean {
        val shouldSync = removeObserverOnly(observer)
        if (shouldSync) {
            syncTriggers()
        }
        return shouldSync
    }

    /**
     * Removes an observer and return true if it was actually removed, or false if it was not found.
     */
    private fun removeObserverOnly(observer: Observer): Boolean {
        val wrapper = observerMapLock.withLock { observerMap.remove(observer) }
        return wrapper != null && observedTableStates.onObserverRemoved(wrapper.tableIds)
    }

    /** Resolves the list of tables and views into unique table names and ids. */
    internal fun validateTableNames(names: Array<out String>): Pair<Array<String>, IntArray> {
        val tableNames = resolveViews(names)
        val tableIds =
            IntArray(tableNames.size) { i ->
                val tableName = tableNames[i]
                tableIdLookup[tableName.lowercase()]
                    ?: throw IllegalArgumentException("There is no table with name $tableName")
            }
        return tableNames to tableIds
    }

    /**
     * Resolves the list of tables and views into a list of unique tables, i.e. if given a view then
     * its underlying tables is expanded into the result.
     */
    private fun resolveViews(names: Array<out String>): Array<String> {
        return buildSet {
                names.forEach { name ->
                    viewTables[name.lowercase()]?.let { addAll(it) } ?: add(name)
                }
            }
            .toTypedArray()
    }

    /** Synchronizes database triggers with observed tables. */
    internal suspend fun syncTriggers() =
        database.closeBarrier.ifNotClosed {
            database.useConnection(isReadOnly = false) { connection ->
                if (connection.inTransaction()) {
                    // Triggers are not synced if the connection is already in a transaction, an
                    // indication that this is a nested transaction and sync is expected to be
                    // invoked before starting a top-level transaction.
                    return@useConnection
                }
                connection.withTransaction(SQLiteTransactionType.IMMEDIATE) {
                    observedTableStates.getTablesToSync()?.forEachIndexed { tableId, observeOp ->
                        when (observeOp) {
                            ObservedTableStates.ObserveOp.NO_OP -> {}
                            ObservedTableStates.ObserveOp.ADD ->
                                startTrackingTable(connection, tableId)
                            ObservedTableStates.ObserveOp.REMOVE ->
                                stopTrackingTable(connection, tableId)
                        }
                    }
                }
            }
        }

    private suspend fun startTrackingTable(connection: PooledConnection, tableId: Int) {
        connection.execSQL("INSERT OR IGNORE INTO $UPDATE_TABLE_NAME VALUES($tableId, 0)")
        val tableName = tablesNames[tableId]
        for (trigger in TRIGGERS) {
            val triggerName = getTriggerName(tableName, trigger)
            connection.execSQL(
                "CREATE TEMP TRIGGER IF NOT EXISTS `$triggerName` " +
                    "AFTER $trigger ON `$tableName` BEGIN " +
                    "UPDATE $UPDATE_TABLE_NAME SET $INVALIDATED_COLUMN_NAME = 1 " +
                    "WHERE $TABLE_ID_COLUMN_NAME = $tableId AND $INVALIDATED_COLUMN_NAME = 0; " +
                    "END"
            )
        }
    }

    private suspend fun stopTrackingTable(connection: PooledConnection, tableId: Int) {
        val tableName = tablesNames[tableId]
        for (trigger in TRIGGERS) {
            val triggerName = getTriggerName(tableName, trigger)
            connection.execSQL("DROP TRIGGER IF EXISTS `$triggerName`")
        }
    }

    /**
     * Attempts to notify invalidated observers if there is a pending refresh. If there is no
     * pending refresh (no previous call to [refreshInvalidationAsync] then this function does
     * nothing.
     *
     * This can be useful to accelerate a pending refresh instead of waiting for the coroutine to
     * launch.
     */
    internal suspend fun refreshInvalidation(
        onRefreshScheduled: () -> Unit = {},
        onRefreshCompleted: () -> Unit = {},
    ) {
        onRefreshScheduled.invoke()
        try {
            notifyInvalidatedObservers()
        } finally {
            onRefreshCompleted.invoke()
        }
    }

    /** Launches a coroutine to notify invalidated observers. */
    internal fun refreshInvalidationAsync(
        onRefreshScheduled: () -> Unit = {},
        onRefreshCompleted: () -> Unit = {},
    ) {
        if (pendingRefresh.compareAndSet(expect = false, update = true)) {
            onRefreshScheduled.invoke()
            database.getCoroutineScope().launch {
                try {
                    notifyInvalidatedObservers()
                } finally {
                    onRefreshCompleted.invoke()
                }
            }
        }
    }

    private suspend fun notifyInvalidatedObservers() =
        database.closeBarrier.ifNotClosed {
            if (!pendingRefresh.compareAndSet(expect = true, update = false)) {
                // No pending refresh
                return
            }
            if (!onAllowRefresh()) {
                // Compatibility callback is disallowing a refresh.
                return
            }
            val invalidatedTableIds =
                database.useConnection(isReadOnly = false) { connection ->
                    if (connection.inTransaction()) {
                        // Skip refresh if connection is already in a transaction, an indication
                        // that
                        // this is a nested transaction and refresh is expected to be invoked after
                        // completing a top-level transaction.
                        return@useConnection emptySet()
                    }
                    try {
                        connection.withTransaction(SQLiteTransactionType.IMMEDIATE) {
                            checkInvalidatedTables(this)
                        }
                    } catch (ex: SQLiteException) {
                        // TODO(b/309990302): We used to log the exception, add the log back.
                        emptySet()
                    }
                }
            if (invalidatedTableIds.isNotEmpty()) {
                notifyInvalidatedTableIds(invalidatedTableIds)
            }
        }

    /** Checks which tables have been invalidated and resets their invalidation state. */
    private suspend fun checkInvalidatedTables(connection: PooledConnection): Set<Int> {
        val invalidatedTableIds =
            connection.usePrepared(SELECT_UPDATED_TABLES_SQL) { statement ->
                buildSet {
                    while (statement.step()) {
                        add(statement.getLong(0).toInt())
                    }
                }
            }
        if (invalidatedTableIds.isNotEmpty()) {
            connection.execSQL(RESET_UPDATED_TABLES_SQL)
        }
        return invalidatedTableIds
    }

    private fun notifyInvalidatedTableIds(tableIds: Set<Int>) {
        observerMapLock.withLock { observerMap.values.forEach { it.notifyByTableIds(tableIds) } }
    }

    internal fun notifyInvalidatedTableNames(
        tableNames: Set<String>,
        filterPredicate: (Observer) -> Boolean = { true }
    ) {
        observerMapLock.withLock {
            observerMap.values.forEach {
                if (filterPredicate(it.observer)) {
                    it.notifyByTableNames(tableNames)
                }
            }
        }
    }

    internal fun getAllObservers() = observerMapLock.withLock { observerMap.keys.toList() }

    internal fun resetSync() {
        observedTableStates.resetTriggerState()
    }

    companion object {
        private val TRIGGERS = arrayOf("INSERT", "UPDATE", "DELETE")

        private const val UPDATE_TABLE_NAME = "room_table_modification_log"
        private const val TABLE_ID_COLUMN_NAME = "table_id"
        private const val INVALIDATED_COLUMN_NAME = "invalidated"

        private const val CREATE_TRACKING_TABLE_SQL =
            "CREATE TEMP TABLE IF NOT EXISTS $UPDATE_TABLE_NAME (" +
                "$TABLE_ID_COLUMN_NAME INTEGER PRIMARY KEY, " +
                "$INVALIDATED_COLUMN_NAME INTEGER NOT NULL DEFAULT 0)"

        private const val SELECT_UPDATED_TABLES_SQL =
            "SELECT * FROM $UPDATE_TABLE_NAME WHERE $INVALIDATED_COLUMN_NAME = 1"

        private const val RESET_UPDATED_TABLES_SQL =
            "UPDATE $UPDATE_TABLE_NAME SET $INVALIDATED_COLUMN_NAME = 0 " +
                "WHERE $INVALIDATED_COLUMN_NAME = 1"

        private fun getTriggerName(tableName: String, triggerType: String) =
            "room_table_modification_trigger_${tableName}_$triggerType"
    }
}

/**
 * Keeps track of which table has to be observed or not due to having one or more observer.
 *
 * Call [onObserverAdded] when an observer is added and [onObserverRemoved] when removing one. To
 * check if a table needs to be tracked or not, call [getTablesToSync].
 */
internal class ObservedTableStates(size: Int) {

    private val lock = reentrantLock()

    // The number of observers per table
    private val tableObserversCount = LongArray(size)

    // The observation state of each table, i.e. true or false if table at ith index should be
    // observed. These states are only valid if `needsSync` is false.
    private val tableObservedState = BooleanArray(size)

    private var needsSync = false

    /**
     * Gets an array of operations to be performed for table at index i from the last time this
     * function was called and based on the [onObserverAdded] and [onObserverRemoved] invocations
     * that occurred in-between.
     */
    internal fun getTablesToSync(): Array<ObserveOp>? =
        lock.withLock {
            if (!needsSync) {
                return null
            }
            needsSync = false
            Array(tableObserversCount.size) { i ->
                val newState = tableObserversCount[i] > 0
                if (newState != tableObservedState[i]) {
                    tableObservedState[i] = newState
                    if (newState) ObserveOp.ADD else ObserveOp.REMOVE
                } else {
                    ObserveOp.NO_OP
                }
            }
        }

    /** Notifies that an observer was added and return true if the state of some table changed. */
    internal fun onObserverAdded(tableIds: IntArray): Boolean =
        lock.withLock {
            var shouldSync = false
            tableIds.forEach { tableId ->
                val previousCount = tableObserversCount[tableId]
                tableObserversCount[tableId] = previousCount + 1
                if (previousCount == 0L) {
                    needsSync = true
                    shouldSync = true
                }
            }
            return shouldSync
        }

    /** Notifies that an observer was removed and return true if the state of some table changed. */
    internal fun onObserverRemoved(tableIds: IntArray): Boolean =
        lock.withLock {
            var shouldSync = false
            tableIds.forEach { tableId ->
                val previousCount = tableObserversCount[tableId]
                tableObserversCount[tableId] = previousCount - 1
                if (previousCount == 1L) {
                    needsSync = true
                    shouldSync = true
                }
            }
            return shouldSync
        }

    internal fun resetTriggerState() =
        lock.withLock {
            tableObservedState.fill(element = false)
            needsSync = true
        }

    internal fun forceNeedSync() {
        lock.withLock { needsSync = true }
    }

    internal enum class ObserveOp {
        NO_OP, // Don't change observation / tracking state for a table
        ADD, // Starting observation / tracking of a table
        REMOVE // Stop observation / tracking of a table
    }
}

/**
 * Wraps an [Observer] and keeps the table information.
 *
 * Internally table ids are used which may change from database to database so the table related
 * information is kept here rather than in the actual observer.
 */
internal class ObserverWrapper(
    internal val observer: Observer,
    internal val tableIds: IntArray,
    private val tableNames: Array<out String>
) {
    init {
        check(tableIds.size == tableNames.size)
    }

    // Optimization for a single-table observer
    private val singleTableSet = if (tableNames.isNotEmpty()) setOf(tableNames[0]) else emptySet()

    internal fun notifyByTableIds(invalidatedTablesIds: Set<Int>) {
        val invalidatedTables =
            when (tableIds.size) {
                0 -> emptySet()
                1 -> if (invalidatedTablesIds.contains(tableIds[0])) singleTableSet else emptySet()
                else ->
                    buildSet {
                        tableIds.forEachIndexed { id, tableId ->
                            if (invalidatedTablesIds.contains(tableId)) {
                                add(tableNames[id])
                            }
                        }
                    }
            }
        if (invalidatedTables.isNotEmpty()) {
            observer.onInvalidated(invalidatedTables)
        }
    }

    internal fun notifyByTableNames(invalidatedTablesNames: Set<String>) {
        val invalidatedTables =
            when (tableNames.size) {
                0 -> emptySet()
                1 ->
                    if (
                        invalidatedTablesNames.any { it.equals(tableNames[0], ignoreCase = true) }
                    ) {
                        singleTableSet
                    } else {
                        emptySet()
                    }
                else ->
                    buildSet {
                        invalidatedTablesNames.forEach { table ->
                            for (ourTable in tableNames) {
                                if (ourTable.equals(table, ignoreCase = true)) {
                                    add(ourTable)
                                    break
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
