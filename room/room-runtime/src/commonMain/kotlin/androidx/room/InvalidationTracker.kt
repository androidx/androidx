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
import androidx.sqlite.SQLiteConnection
import kotlin.jvm.JvmSuppressWildcards
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

/**
 * The invalidation tracker keeps track of modified tables by queries and notifies its registered
 * [Observer]s about such modifications.
 */
expect class InvalidationTracker
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
constructor(
    database: RoomDatabase,
    shadowTablesMap: Map<String, String>,
    viewTables: Map<String, @JvmSuppressWildcards Set<String>>,
    vararg tableNames: String
) {
    /**
     * Internal method to initialize table tracking. Invoked by generated code.
     */
    internal fun internalInit(connection: SQLiteConnection)

    /**
     * An observer that can listen for changes in the database by subscribing to an
     * [InvalidationTracker].
     *
     * @param tables The names of the tables this observer is interested in getting notified if
     * they are modified.
     */
    abstract class Observer(tables: Array<out String>) {

        internal val tables: Array<out String>

        /**
         * Creates an observer for the given tables and views.
         *
         * @param firstTable The name of the table or view.
         * @param rest       More names of tables or views.
         */
        protected constructor(firstTable: String, vararg rest: String)

        /**
         * Invoked when one of the observed tables is invalidated (changed).
         *
         * @param tables A set of invalidated tables. When the observer is interested in multiple
         * tables, this set can be used to distinguish which of the observed tables were
         * invalidated. When observing a database view the names of underlying tables will be in
         * the set instead of the view name.
         */
        abstract fun onInvalidated(tables: Set<String>)
    }
}

/**
 * Keeps track of which table has to be observed or not due to having one or more observer.
 *
 * Call [onObserverAdded] when an observer is added and [onObserverRemoved] when removing one.
 * To check if a table needs to be tracked or not, call [getTablesToSync].
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
    internal fun getTablesToSync(): Array<ObserveOp>? = lock.withLock {
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

    /**
     * Notifies that an observer was added and return true if the state of some table changed.
     */
    internal fun onObserverAdded(tableIds: IntArray): Boolean = lock.withLock {
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

    /**
     * Notifies that an observer was removed and return true if the state of some table changed.
     */
    internal fun onObserverRemoved(tableIds: IntArray): Boolean = lock.withLock {
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

    internal fun resetTriggerState() = lock.withLock {
        tableObservedState.fill(element = false)
        needsSync = true
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
 * Internally table ids are used which may change from database to database so the table
 * related information is kept here rather than in the actual observer.
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
        val invalidatedTables = when (tableIds.size) {
            0 -> emptySet()
            1 -> if (invalidatedTablesIds.contains(tableIds[0])) singleTableSet else emptySet()
            else -> buildSet {
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
        val invalidatedTables = when (tableNames.size) {
            0 -> emptySet()
            1 -> if (invalidatedTablesNames.any { it.equals(tableNames[0], ignoreCase = true) }) {
                singleTableSet
            } else {
                emptySet()
            }
            else -> buildSet {
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
