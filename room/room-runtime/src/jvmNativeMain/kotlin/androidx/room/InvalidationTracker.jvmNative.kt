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
 * The invalidation tracker keeps track of modified tables by queries and notifies its registered
 * [Observer]s about such modifications.
 */
actual class InvalidationTracker
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
actual constructor(
    database: RoomDatabase,
    shadowTablesMap: Map<String, String>,
    viewTables: Map<String, Set<String>>,
    vararg tableNames: String
) {
    /**
     * Internal method to initialize table tracking. Invoked by generated code.
     */
    internal actual fun internalInit(connection: SQLiteConnection) {
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
    }
}
