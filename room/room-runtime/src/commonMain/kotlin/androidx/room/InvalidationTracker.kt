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
import androidx.room.Transactor.SQLiteTransactionType
import androidx.room.concurrent.ifNotClosed
import androidx.room.util.getCoroutineContext
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteException
import androidx.sqlite.execSQL
import androidx.sqlite.use
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmSuppressWildcards
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The invalidation tracker keeps track of tables modified by queries and notifies its created
 * [Flow]s about such modifications.
 *
 * A [Flow] tracking one or more tables can be created via [createFlow]. Once the [Flow] stream
 * starts being collected, if a database operation changes one of the tables that the [Flow] was
 * created from, then such table is considered 'invalidated' and the [Flow] will emit a new value.
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
     * Creates a [Flow] that tracks modifications in the database and emits sets of the tables that
     * were invalidated.
     *
     * The [Flow] will emit at least one value, a set of all the tables registered for observation
     * to kick-start the stream unless [emitInitialState] is set to `false`.
     *
     * If one of the tables to observe does not exist in the database, this functions throws an
     * [IllegalArgumentException].
     *
     * The returned [Flow] can be used to create a stream that reacts to changes in the database:
     * ```
     * fun getArtistTours(from: Date, to: Date): Flow<Map<Artist, TourState>> {
     *   return db.invalidationTracker.createFlow("Artist").map { _ ->
     *     val artists = artistsDao.getAllArtists()
     *     val tours = tourService.fetchStates(artists.map { it.id })
     *     associateTours(artists, tours, from, to)
     *   }
     * }
     * ```
     *
     * @param tables The name of the tables or views to track.
     * @param emitInitialState Set to `false` if no initial emission is desired. Default value is
     *   `true`.
     */
    @JvmOverloads
    fun createFlow(vararg tables: String, emitInitialState: Boolean = true): Flow<Set<String>>

    /**
     * Synchronize created [Flow]s with their tables.
     *
     * This function should be called before any write operation is performed on the database so
     * that a tracking link is created between the flows and its interested tables.
     *
     * @see refreshAsync
     */
    internal suspend fun sync()

    /**
     * Refresh created [Flow]s asynchronously, emitting new values on those whose tables have been
     * invalidated.
     *
     * This function should be called after any write operation is performed on the database, such
     * that tracked tables and its associated flows are notified if invalidated. In most cases Room
     * will call this function automatically but if a write operation is performed on the database
     * via another connection or through [RoomDatabase.useConnection] you might need to invoke this
     * function manually to trigger invalidation.
     */
    fun refreshAsync()

    /**
     * Non-asynchronous version of [refreshAsync] with the addition that it will return true if
     * there were any pending invalidations.
     *
     * An optional array of tables can be given to validate if any of those tables had pending
     * invalidations, if so causing this function to return true.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) suspend fun refresh(vararg tables: String): Boolean

    /** Stops invalidation tracker operations. */
    internal fun stop()
}

/**
 * A TRIGGER based implementation of an invalidation tracker.
 *
 * Some details on how this tracker works:
 * * An in-memory table is created with two columns, 'table_id' and 'invalidated' to known which
 *   table has been modified.
 * * [ObservedTableStates] keeps the 'observed' state of each table helping the tracker know which
 *   tables should be watched (via an installed TRIGGER) based on the number of observers
 *   interested.
 * * Before a write transaction, Room will sync triggers by invoking [InvalidationTracker.sync].
 * * If in the write transaction a table was modified, the installed trigger will flip the table's
 *   invalidated column in the in-memory table to ON.
 * * After a write transaction, Room will check the invalidated rows by invoking
 *   [InvalidationTracker.refreshAsync], notifying observers if necessary via the provided
 *   [onInvalidatedTablesIds] callback.
 */
internal class TriggerBasedInvalidationTracker(
    private val database: RoomDatabase,
    // Table to shadow / content table names
    private val shadowTablesMap: Map<String, String>,
    // View to underlying table names
    private val viewTables: Map<String, Set<String>>,
    tableNames: Array<out String>,
    // Callback function for when a set of tables are invalidated, the 'id' of a table is its
    // index in the given `tableNames`
    private val onInvalidatedTablesIds: (Set<Int>) -> Unit
) {
    /** Table name (lowercase) to index (id) in [tablesNames], used as a quick lookup map. */
    private val tableIdLookup: Map<String, Int>
    /** Table names (lowercase), the index is at which a table is in the array is its 'id'. */
    private val tablesNames: Array<String>

    private val observedTableStates: ObservedTableStates
    private val observedTableVersions: ObservedTableVersions

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

        observedTableStates = ObservedTableStates(tablesNames.size)
        observedTableVersions = ObservedTableVersions(tablesNames.size)
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

    internal fun createFlow(
        tables: Array<out String>,
        emitInitialState: Boolean
    ): Flow<Set<String>> {
        val (resolvedTableNames, tableIds) = validateTableNames(tables)
        return flow {
            val shouldSync = observedTableStates.onObserverAdded(tableIds)
            if (shouldSync) {
                // Syncing triggers is a database operation, we use the database context just for
                // the sync while adhering to flow context preservation.
                withContext(database.getCoroutineContext(inTransaction = false)) { syncTriggers() }
            }
            try {
                var currentVersions: IntArray? = null
                observedTableVersions.collect { newVersions ->
                    if (currentVersions == null) {
                        // Initial invalidation of all tables, to kick-start the flow
                        if (emitInitialState) {
                            emit(resolvedTableNames.toSet())
                        }
                    } else {
                        val invalidatedTablesNames =
                            resolvedTableNames.filterIndexed { i, _ ->
                                checkNotNull(currentVersions)[tableIds[i]] !=
                                    newVersions[tableIds[i]]
                            }
                        if (invalidatedTablesNames.isNotEmpty()) {
                            emit(invalidatedTablesNames.toSet())
                        }
                    }
                    currentVersions = newVersions
                }
            } finally {
                // For quick cancellation, no trigger sync is done. In practice this means
                // a trigger might be kept longer than needed if database interactions are done
                // through direct connections, but otherwise continues usage of Room will eventually
                // cleanup the triggers.
                observedTableStates.onObserverRemoved(tableIds)
            }
        }
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

    /** Notifies that an observer was added and return true if the state of some table changed. */
    internal fun onObserverAdded(tableIds: IntArray) = observedTableStates.onObserverAdded(tableIds)

    /** Notifies that an observer was removed and return true if the state of some table changed. */
    internal fun onObserverRemoved(tableIds: IntArray) =
        observedTableStates.onObserverRemoved(tableIds)

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
     * Attempts to notify invalidated trackers if there is a pending refresh. If there is no pending
     * refresh (no previous call to [refreshInvalidationAsync] then this function does nothing and
     * returns false.
     *
     * An optional array of table names can be provided to check if any of those tables where
     * invalidated, causing this function to return true if indeed any of them where invalidated.
     *
     * If no tables are provided then this function will return true due to any invalidation.
     *
     * This can be useful to accelerate a pending refresh instead of waiting for the coroutine to
     * launch.
     */
    internal suspend fun refreshInvalidation(
        tables: Array<out String>,
        onRefreshScheduled: () -> Unit = {},
        onRefreshCompleted: () -> Unit = {},
    ): Boolean {
        val (_, tableIds) = validateTableNames(tables)
        onRefreshScheduled.invoke()
        try {
            val invalidatesTableIds = notifyInvalidation()
            return if (tableIds.isNotEmpty()) {
                tableIds.any { it in invalidatesTableIds }
            } else {
                invalidatesTableIds.isNotEmpty()
            }
        } finally {
            onRefreshCompleted.invoke()
        }
    }

    /** Launches a coroutine to notify of invalidation. */
    internal fun refreshInvalidationAsync(
        onRefreshScheduled: () -> Unit = {},
        onRefreshCompleted: () -> Unit = {},
    ) {
        if (pendingRefresh.compareAndSet(expect = false, update = true)) {
            onRefreshScheduled.invoke()
            database.getCoroutineScope().launch(
                CoroutineName("Room Invalidation Tracker Refresh")
            ) {
                try {
                    notifyInvalidation()
                } finally {
                    onRefreshCompleted.invoke()
                }
            }
        }
    }

    /**
     * Checks for invalidates tables and emit notifications, returning true if there where any
     * invalidated tables or false if there were not.
     */
    private suspend fun notifyInvalidation(): Set<Int> {
        database.closeBarrier.ifNotClosed {
            if (!pendingRefresh.compareAndSet(expect = true, update = false)) {
                // No pending refresh
                return emptySet()
            }
            if (!onAllowRefresh()) {
                // Compatibility callback is disallowing a refresh.
                return emptySet()
            }
            val invalidatedTableIds =
                database.useConnection(isReadOnly = false) { connection ->
                    if (connection.inTransaction()) {
                        // Skip refresh if connection is already in a transaction, an indication
                        // that this is a nested transaction and refresh is expected to be invoked
                        // after completing a top-level transaction.
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
                observedTableVersions.increment(invalidatedTableIds)
                onInvalidatedTablesIds.invoke(invalidatedTableIds)
            }
            return invalidatedTableIds
        }
        return emptySet()
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
 * Keeps an ever incrementing version of each table as they get invalidated.
 *
 * The versions value is not persistent and its true meaning can be better described as the amount
 * of detected invalidations for a given table, it is solely used to drive invalidation of [Flow]s.
 */
internal class ObservedTableVersions(size: Int) {
    /* Table versions, the index at which a version is in the array is the table 'id'. */
    private val versions = MutableStateFlow(IntArray(size))

    /** Increment the version of the given table ids, causing flow emissions on any collectors. */
    fun increment(tableIds: Set<Int>) {
        if (tableIds.isEmpty()) {
            return
        }
        versions.update { currentVersions ->
            IntArray(currentVersions.size) { id ->
                if (id in tableIds) {
                    currentVersions[id] + 1
                } else {
                    currentVersions[id]
                }
            }
        }
    }

    /** Equivalent to [Flow.collect] specifically for the table versions. */
    suspend fun collect(collector: FlowCollector<IntArray>): Nothing = versions.collect(collector)
}
