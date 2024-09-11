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
import androidx.sqlite.SQLiteConnection
import kotlin.jvm.JvmOverloads
import kotlinx.coroutines.flow.Flow

/**
 * The invalidation tracker keeps track of tables modified by queries and notifies its created
 * [Flow]s about such modifications.
 *
 * A [Flow] tracking one or more tables can be created via [createFlow]. Once the [Flow] stream
 * starts being collected, if a database operation changes one of the tables that the [Flow] was
 * created from, then such table is considered 'invalidated' and the [Flow] will emit a new value.
 */
actual class InvalidationTracker
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
actual constructor(
    private val database: RoomDatabase,
    shadowTablesMap: Map<String, String>,
    viewTables: Map<String, Set<String>>,
    vararg tableNames: String
) {
    private val implementation =
        TriggerBasedInvalidationTracker(
            database = database,
            shadowTablesMap = shadowTablesMap,
            viewTables = viewTables,
            tableNames = tableNames,
            onInvalidatedTablesIds = {}
        )

    /** Internal method to initialize table tracking. Invoked by generated code. */
    internal actual fun internalInit(connection: SQLiteConnection) {
        implementation.configureConnection(connection)
    }

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
    actual fun createFlow(vararg tables: String, emitInitialState: Boolean): Flow<Set<String>> {
        return implementation.createFlow(tables, emitInitialState)
    }

    /**
     * Synchronize created [Flow]s with their tables.
     *
     * This function should be called before any write operation is performed on the database so
     * that a tracking link is created between the flows and its interested tables.
     *
     * @see refreshAsync
     */
    internal actual suspend fun sync() {
        implementation.syncTriggers()
    }

    /**
     * Refresh created [Flow]s asynchronously, emitting new values on those whose tables have been
     * invalidated.
     *
     * This function should be called after any write operation is performed on the database, such
     * that tracked tables and its associated flows are notified if invalidated. In most cases Room
     * will call this function automatically but if a write operation is performed on the database
     * via another connection or through [RoomDatabase.useConnection] you might need to invoke this
     * function to trigger invalidation.
     */
    actual fun refreshAsync() {
        implementation.refreshInvalidationAsync()
    }

    /**
     * Non-asynchronous version of [refreshAsync] with the addition that it will return true if
     * there were any pending invalidations.
     *
     * An optional array of tables can be given to validate if any of those tables had pending
     * invalidations, if so causing this function to return true.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    actual suspend fun refresh(vararg tables: String): Boolean {
        return implementation.refreshInvalidation(tables)
    }

    /** Stops invalidation tracker operations. */
    internal actual fun stop() {}
}
