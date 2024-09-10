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
actual class InvalidationTracker
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
actual constructor(
    database: RoomDatabase,
    shadowTablesMap: Map<String, String>,
    viewTables: Map<String, Set<String>>,
    vararg tableNames: String
) {
    private val implementation =
        TriggerBasedInvalidationTracker(database, shadowTablesMap, viewTables, tableNames)

    /** Internal method to initialize table tracking. Invoked by generated code. */
    internal actual fun internalInit(connection: SQLiteConnection) {
        implementation.configureConnection(connection)
    }

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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    actual suspend fun subscribe(observer: Observer) {
        implementation.addObserver(observer)
    }

    /**
     * Unsubscribes the given [observer] from the tracker.
     *
     * If the observer was never subscribed in the first place, then this function does nothing.
     *
     * @param observer The observer to remove.
     */
    // TODO(b/329315924): Replace with Flow based API
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    actual suspend fun unsubscribe(observer: Observer) {
        implementation.removeObserver(observer)
    }

    /**
     * Synchronize subscribed observers with their tables.
     *
     * This function should be called before any write operation is performed on the database so
     * that a tracking link is created between observers and its interest tables.
     *
     * @see refreshAsync
     */
    internal actual suspend fun sync() {
        implementation.syncTriggers()
    }

    /**
     * Refresh subscribed observers asynchronously, invoking [Observer.onInvalidated] on those whose
     * tables have been invalidated.
     *
     * This function should be called after any write operation is performed on the database, such
     * that tracked tables and its associated observers are notified if invalidated.
     */
    actual fun refreshAsync() {
        implementation.refreshInvalidationAsync()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    actual suspend fun refreshInvalidation() {
        implementation.refreshInvalidation()
    }

    /** Stops invalidation tracker operations. */
    actual fun stop() {}

    /**
     * An observer that can listen for changes in the database by subscribing to an
     * [InvalidationTracker].
     *
     * @param tables The names of the tables this observer is interested in getting notified if they
     *   are modified.
     */
    actual abstract class Observer
    actual constructor(internal actual val tables: Array<out String>) {
        /**
         * Creates an observer for the given tables and views.
         *
         * @param firstTable The name of the table or view.
         * @param rest More names of tables or views.
         */
        protected actual constructor(
            firstTable: String,
            vararg rest: String
        ) : this(arrayOf(firstTable, *rest))

        /**
         * Invoked when one of the observed tables is invalidated (changed).
         *
         * @param tables A set of invalidated tables. When the observer is interested in multiple
         *   tables, this set can be used to distinguish which of the observed tables were
         *   invalidated. When observing a database view the names of underlying tables will be in
         *   the set instead of the view name.
         */
        actual abstract fun onInvalidated(tables: Set<String>)
    }
}
