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
package androidx.room3

import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo
import androidx.room3.autoclose.AutoCloser
import androidx.room3.concurrent.AtomicInt
import androidx.sqlite.SQLiteConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

/**
 * The invalidation tracker keeps track of tables modified by queries and notifies its created
 * [Flow]s about such modifications.
 *
 * A [Flow] tracking one or more tables can be created via [createFlow]. Once the [Flow] stream
 * starts being collected, if a database operation changes one of the tables that the [Flow] was
 * created from, then such table is considered 'invalidated' and the [Flow] will emit a new value.
 */
public actual class InvalidationTracker
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
actual constructor(
    internal val database: RoomDatabase,
    private val shadowTablesMap: Map<String, String>,
    private val viewTables: Map<String, @JvmSuppressWildcards Set<String>>,
    internal vararg val tableNames: String,
) {
    private val implementation =
        TriggerBasedInvalidationTracker(
            database = database,
            shadowTablesMap = shadowTablesMap,
            viewTables = viewTables,
            tableNames = tableNames,
            useTempTable = database.useTempTrackingTable,
        )

    /** The number of active flows, excluding the multi-instance one for auto-closing purposes */
    private val flowCount = AtomicInt(0)

    private var autoCloser: AutoCloser? = null

    private val onRefreshScheduled: () -> Unit = {
        // When a refresh async is scheduled the ref count is incremented from RoomDatabase, so the
        // db can't be closed, but we need to be sure that our db isn't closed until refresh is
        // completed. This increment call must be matched with a corresponding decrement at the
        // end of a refresh.
        autoCloser?.incrementCount()
    }

    private val onRefreshCompleted: () -> Unit = { autoCloser?.decrementCount() }

    /** The intent for restarting invalidation after auto-close. */
    private var multiInstanceInvalidationIntent: Intent? = null

    /** The multi instance invalidation client. */
    private var multiInstanceInvalidationClient: MultiInstanceInvalidationClient? = null

    private val trackerLock = Any()

    init {
        // Setup a callback to disallow invalidation refresh when underlying database
        // is closed. This is done to support auto-close feature.
        implementation.onAllowRefresh = { database.isOpenInternal }
    }

    /**
     * Sets the auto closer for this invalidation tracker so that the invalidation tracker can
     * ensure that the database is not closed if there are pending invalidations that haven't yet
     * been flushed.
     *
     * This also adds a callback to the auto closer to ensure that the InvalidationTracker is in an
     * ok state once the table is invalidated.
     *
     * This must be called before the database is used.
     *
     * @param autoCloser the auto closer associated with the db
     */
    internal fun setAutoCloser(autoCloser: AutoCloser) {
        this.autoCloser = autoCloser
        autoCloser.setAutoCloseCallback(::onAutoCloseCallback)
    }

    /** Internal method to initialize table tracking. */
    internal actual suspend fun internalInit(connection: SQLiteConnection) {
        implementation.configureConnection(connection)
        synchronized(trackerLock) {
            multiInstanceInvalidationClient?.start(checkNotNull(multiInstanceInvalidationIntent))
        }
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
     * function manually to trigger invalidation.
     */
    public actual fun refreshAsync() {
        implementation.refreshInvalidationAsync(onRefreshScheduled, onRefreshCompleted)
    }

    /**
     * Non-asynchronous version of [refreshAsync] with the addition that it will return true if
     * there were any pending invalidations.
     *
     * An optional array of tables can be given to validate if any of those tables had pending
     * invalidations, if so causing this function to return true.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual suspend fun refresh(vararg tables: String): Boolean {
        return implementation.refreshInvalidation(tables, onRefreshScheduled, onRefreshCompleted)
    }

    private fun onAutoCloseCallback() {
        synchronized(trackerLock) {
            multiInstanceInvalidationClient?.let { client ->
                if (flowCount.get() == 0) {
                    client.stop()
                }
            }
            implementation.resetSync()
        }
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
    public actual fun createFlow(
        vararg tables: String,
        emitInitialState: Boolean,
    ): Flow<Set<String>> {
        val (resolvedTableNames, tableIds) = implementation.validateTableNames(tables)
        val trackerFlow = implementation.createFlow(resolvedTableNames, tableIds, emitInitialState)
        val multiInstanceFlow = multiInstanceInvalidationClient?.createFlow(resolvedTableNames)
        val flow =
            if (multiInstanceFlow != null) {
                merge(trackerFlow, multiInstanceFlow)
            } else {
                trackerFlow
            }
        return flow
            .onStart { flowCount.incrementAndGet() }
            .onCompletion { flowCount.decrementAndGet() }
    }

    /** Invalidation flow for the [MultiInstanceInvalidationClient] */
    internal fun createAllTablesFlow(): Flow<Set<String>> {
        // Note that this function goes directly to the impl, skipping flow count and
        // multi-instance merge to avoid recursive invalidation and enable auto-close stopping the
        // multi-instance client, i.e. this is like a 'weak' invalidation flow.
        val (resolvedTableNames, tableIds) = implementation.validateTableNames(tableNames)
        return implementation.createFlow(
            resolvedTableNames = resolvedTableNames,
            tableIds = tableIds,
            emitInitialState = false,
            canSync = false,
        )
    }

    internal fun initMultiInstanceInvalidation(
        context: Context,
        name: String,
        serviceIntent: Intent,
    ) {
        multiInstanceInvalidationIntent = serviceIntent
        multiInstanceInvalidationClient = MultiInstanceInvalidationClient(context, name, this)
    }

    /** Stops invalidation tracker operations. */
    internal actual fun stop() {
        multiInstanceInvalidationClient?.stop()
    }
}
