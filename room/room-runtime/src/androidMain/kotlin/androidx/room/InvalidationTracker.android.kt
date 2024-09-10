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
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.InvalidationTracker.Observer
import androidx.room.support.AutoCloser
import androidx.sqlite.SQLiteConnection
import java.lang.ref.WeakReference
import java.util.concurrent.Callable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
actual open class InvalidationTracker
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
actual constructor(
    internal val database: RoomDatabase,
    private val shadowTablesMap: Map<String, String>,
    private val viewTables: Map<String, @JvmSuppressWildcards Set<String>>,
    internal vararg val tableNames: String
) {
    private val implementation =
        TriggerBasedInvalidationTracker(database, shadowTablesMap, viewTables, tableNames)

    private var autoCloser: AutoCloser? = null

    private val onRefreshScheduled: () -> Unit = {
        // refreshVersionsAsync() is called with the ref count incremented from
        // RoomDatabase, so the db can't be closed here, but we need to be sure that our
        // db isn't closed until refresh is completed. This increment call must be
        // matched with a corresponding call in refreshRunnable.
        autoCloser?.incrementCountAndEnsureDbIsOpen()
    }

    private val onRefreshCompleted: () -> Unit = { autoCloser?.decrementCountAndScheduleClose() }

    private val invalidationLiveDataContainer: InvalidationLiveDataContainer =
        InvalidationLiveDataContainer(database)

    /** The initialization state for restarting invalidation after auto-close. */
    private var multiInstanceClientInitState: MultiInstanceClientInitState? = null

    /** The multi instance invalidation client. */
    private var multiInstanceInvalidationClient: MultiInstanceInvalidationClient? = null

    private val trackerLock = Any()

    @Deprecated("No longer called by generated implementation")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    constructor(
        database: RoomDatabase,
        vararg tableNames: String
    ) : this(
        database = database,
        shadowTablesMap = emptyMap(),
        viewTables = emptyMap(),
        tableNames = tableNames
    )

    init {
        // TODO(b/316944352): Figure out auto-close with driver APIs
        // Setup a callback to disallow invalidation refresh when underlying compat database
        // is closed. This is done to support auto-close feature.
        implementation.onAllowRefresh = {
            !database.inCompatibilityMode() || database.isOpenInternal
        }
    }

    /**
     * Sets the auto closer for this invalidation tracker so that the invalidation tracker can
     * ensure that the database is not closed if there are pending invalidations that haven't yet
     * been flushed.
     *
     * This also adds a callback to the autocloser to ensure that the InvalidationTracker is in an
     * ok state once the table is invalidated.
     *
     * This must be called before the database is used.
     *
     * @param autoCloser the autocloser associated with the db
     */
    internal fun setAutoCloser(autoCloser: AutoCloser) {
        this.autoCloser = autoCloser
        autoCloser.setAutoCloseCallback(::onAutoCloseCallback)
    }

    /** Internal method to initialize table tracking. */
    internal actual fun internalInit(connection: SQLiteConnection) {
        implementation.configureConnection(connection)
        synchronized(trackerLock) {
            if (multiInstanceInvalidationClient == null && multiInstanceClientInitState != null) {
                // Start multi-instance invalidation, based in info from the saved initState.
                startMultiInstanceInvalidation()
            }
        }
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
        if (database.inCompatibilityMode() && !database.isOpenInternal) {
            return
        }
        implementation.syncTriggers()
    }

    // TODO(b/309990302): Needed for compatibility with internalBeginTransaction(), not great.
    internal fun syncBlocking(): Unit = runBlocking { sync() }

    /**
     * Refresh subscribed observers asynchronously, invoking [Observer.onInvalidated] on those whose
     * tables have been invalidated.
     *
     * This function should be called after any write operation is performed on the database, such
     * that tracked tables and its associated observers are notified if invalidated.
     */
    actual fun refreshAsync() {
        implementation.refreshInvalidationAsync(onRefreshScheduled, onRefreshCompleted)
    }

    private fun onAutoCloseCallback() {
        synchronized(trackerLock) {
            val isObserverMapEmpty =
                implementation.getAllObservers().filterNot { it.isRemote }.isEmpty()
            if (multiInstanceInvalidationClient != null && isObserverMapEmpty) {
                stopMultiInstanceInvalidation()
            }
            implementation.resetSync()
        }
    }

    private fun startMultiInstanceInvalidation() {
        val state = checkNotNull(multiInstanceClientInitState)
        multiInstanceInvalidationClient =
            MultiInstanceInvalidationClient(
                    context = state.context,
                    name = state.name,
                    invalidationTracker = this,
                )
                .apply { start(state.serviceIntent) }
    }

    private fun stopMultiInstanceInvalidation() {
        multiInstanceInvalidationClient?.stop()
        multiInstanceInvalidationClient = null
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
    open fun addObserver(observer: Observer): Unit = runBlocking {
        implementation.addObserver(observer)
    }

    /** An internal [addObserver] for remote observer only that skips trigger syncing. */
    internal fun addRemoteObserver(observer: Observer): Unit {
        check(observer.isRemote) { "isRemote was false of observer argument" }
        implementation.addObserverOnly(observer)
    }

    /**
     * Adds an observer but keeps a weak reference back to it.
     *
     * Note that you cannot remove this observer once added. It will be automatically removed when
     * the observer is GC'ed.
     *
     * @param observer The observer to which InvalidationTracker will keep a weak reference.
     */
    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    open fun addWeakObserver(observer: Observer) {
        addObserver(WeakObserver(this, database.getCoroutineScope(), observer))
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
     * Removes the observer from the observers list.
     *
     * This method should be called on a background/worker thread as it performs database
     * operations.
     *
     * @param observer The observer to remove.
     */
    @WorkerThread
    open fun removeObserver(observer: Observer): Unit = runBlocking {
        implementation.removeObserver(observer)
    }

    /**
     * Enqueues a task to refresh the list of updated tables.
     *
     * This method is automatically called when [RoomDatabase.endTransaction] is called but if you
     * have another connection to the database or directly use
     * [androidx.sqlite.db.SupportSQLiteDatabase], you may need to call this manually.
     */
    open fun refreshVersionsAsync() {
        implementation.refreshInvalidationAsync(onRefreshScheduled, onRefreshCompleted)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    actual suspend fun refreshInvalidation() {
        implementation.refreshInvalidation(onRefreshScheduled, onRefreshCompleted)
    }

    /** Check versions for tables, and run observers synchronously if tables have been updated. */
    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    open fun refreshVersionsSync(): Unit = runBlocking {
        implementation.refreshInvalidation(onRefreshScheduled, onRefreshCompleted)
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
    internal fun notifyObserversByTableNames(vararg tables: String) {
        implementation.notifyInvalidatedTableNames(setOf(*tables)) { !it.isRemote }
    }

    /**
     * Creates a LiveData that computes the given function once and for every other invalidation of
     * the database.
     *
     * Holds a strong reference to the created LiveData as long as it is active.
     *
     * @param computeFunction The function that calculates the value
     * @param tableNames The list of tables to observe
     * @param T The return type
     * @return A new LiveData that computes the given function when the given list of tables
     *   invalidates.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Deprecated(
        message = "Replaced with overload that takes 'inTransaction 'parameter.",
        replaceWith = ReplaceWith("createLiveData(tableNames, false, computeFunction")
    )
    open fun <T> createLiveData(
        tableNames: Array<out String>,
        computeFunction: Callable<T?>
    ): LiveData<T> {
        return createLiveData(tableNames, false, computeFunction)
    }

    /**
     * Creates a LiveData that computes the given function once and for every other invalidation of
     * the database.
     *
     * Holds a strong reference to the created LiveData as long as it is active.
     *
     * @param tableNames The list of tables to observe
     * @param inTransaction True if the computeFunction will be done in a transaction, false
     *   otherwise.
     * @param computeFunction The function that calculates the value
     * @param T The return type
     * @return A new LiveData that computes the given function when the given list of tables
     *   invalidates.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    open fun <T> createLiveData(
        tableNames: Array<out String>,
        inTransaction: Boolean,
        computeFunction: Callable<T?>
    ): LiveData<T> {
        // Validate names early to fail fast as actual observer subscription is done once LiveData
        // is observed.
        implementation.validateTableNames(tableNames)
        return invalidationLiveDataContainer.create(tableNames, inTransaction, computeFunction)
    }

    /**
     * Creates a LiveData that computes the given function once and for every other invalidation of
     * the database.
     *
     * Holds a strong reference to the created LiveData as long as it is active.
     *
     * @param tableNames The list of tables to observe
     * @param inTransaction True if the computeFunction will be done in a transaction, false
     *   otherwise.
     * @param computeFunction The function that calculates the value
     * @param T The return type
     * @return A new LiveData that computes the given function when the given list of tables
     *   invalidates.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    fun <T> createLiveData(
        tableNames: Array<out String>,
        inTransaction: Boolean,
        computeFunction: (SQLiteConnection) -> T?
    ): LiveData<T> {
        // Validate names early to fail fast as actual observer subscription is done once LiveData
        // is observed.
        implementation.validateTableNames(tableNames)
        // TODO(329315924): Could we use createFlow(...).asLiveData() ?
        return invalidationLiveDataContainer.create(tableNames, inTransaction, computeFunction)
    }

    internal fun initMultiInstanceInvalidation(
        context: Context,
        name: String,
        serviceIntent: Intent
    ) {
        multiInstanceClientInitState =
            MultiInstanceClientInitState(
                context = context,
                name = name,
                serviceIntent = serviceIntent
            )
    }

    /** Stops invalidation tracker operations. */
    internal actual fun stop() {
        stopMultiInstanceInvalidation()
    }

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
        val coroutineScope: CoroutineScope,
        delegate: Observer
    ) : Observer(delegate.tables) {
        private val delegateRef: WeakReference<Observer> = WeakReference(delegate)

        override fun onInvalidated(tables: Set<String>) {
            val observer = delegateRef.get()
            if (observer == null) {
                coroutineScope.launch { tracker.unsubscribe(this@WeakObserver) }
            } else {
                observer.onInvalidated(tables)
            }
        }
    }

    /** Stores needed info to restart the invalidation after it was auto-closed. */
    private data class MultiInstanceClientInitState(
        val context: Context,
        val name: String,
        val serviceIntent: Intent
    )

    // Kept for binary compatibility even if empty. :(
    companion object
}
