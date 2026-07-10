/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room3.autoclose

import android.os.SystemClock
import androidx.annotation.GuardedBy
import androidx.room3.concurrent.AtomicInt
import androidx.room3.concurrent.AtomicLong
import androidx.sqlite.SQLiteConnection
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * This class is responsible for automatically closing (on a timer started when there are no
 * remaining references) connections of a [AutoClosingSQLiteDriver]. When all connections are closed
 * the database is considered closed.
 *
 * It is important to ensure that the reference count is incremented when using any of the database
 * connections from the [AutoClosingSQLiteDriver] this closer belongs to.
 */
internal class AutoCloser(
    private val config: AutoCloserConfig,
    private val watch: Watch = Watch { SystemClock.uptimeMillis() },
) {
    private val lock = ReentrantLock()
    @GuardedBy("lock") private val connections = mutableSetOf<AutoClosingSQLiteConnection>()
    private val referenceCount = AtomicInt(0)
    private val lastReferenceTimestamp = AtomicLong(0)
    private var autoCloseJob: Job? = null

    @Volatile private var explicitlyClose = false

    private lateinit var coroutineScope: CoroutineScope

    // Callback for when all connections are closed (database is closed)
    private lateinit var onAutoCloseCallback: () -> Unit
    // Callback for when a database connection is re-opened
    private lateinit var onAutoOpenCallback: (SQLiteConnection) -> Unit

    /**
     * Sets the scope to launch the timer coroutine. Since an instance of this class is created
     * during [androidx.room3.RoomDatabase.init] and is needed for the connection manager, before
     * Room's scope is created, we use this function to initialize the scope and break the dep
     * cycle.
     */
    fun initCoroutineScope(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
    }

    /** Increments the usage count. An increment should be followed by a decrement. */
    fun incrementCount() {
        autoCloseJob?.cancel()
        autoCloseJob = null
        check(!explicitlyClose) { "Attempting to use an already closed database." }
        referenceCount.incrementAndGet()
    }

    /**
     * Decrements the usage count. At least one increment must have been done before a decrement,
     * otherwise this functions throw due to unbalanced invocations.
     */
    fun decrementCount() {
        val newCount = referenceCount.decrementAndGet()
        check(newCount >= 0) { "Unbalanced reference count." }
        lastReferenceTimestamp.set(watch.getMillis())
        if (newCount == 0) {
            autoCloseJob =
                coroutineScope.launch {
                    delay(config.timeoutInMs)
                    autoCloseConnections()
                }
        }
    }

    fun dispatchOnAutoOpenCallback(connection: SQLiteConnection) {
        onAutoOpenCallback.invoke(connection)
    }

    /**
     * Checks if the database is open.
     *
     * A database is considered open if there is at least one connection that has not been closed,
     * either due to auto-close or explicitly being closed.
     */
    fun isOpen(): Boolean {
        return lock.withLock { connections.any { !it.isAutoClosed() } }
    }

    /**
     * Registers an auto-closing connection.
     *
     * All connections opened by the [AutoClosingSQLiteDriver] for which an instance of this class
     * belongs to should register its connections.
     */
    fun register(connection: AutoClosingSQLiteConnection) {
        lock.withLock { connections.add(connection) }
    }

    /**
     * Unregisters an auto-closing connection.
     *
     * Only explicitly closed connections should be unregistered.
     */
    fun unregister(connection: AutoClosingSQLiteConnection) {
        lock.withLock { connections.remove(connection) }
    }

    /** Sets a callback to be invoked when the database has been auto-closed. */
    fun setAutoCloseCallback(callback: () -> Unit) {
        onAutoCloseCallback = callback
    }

    /**
     * Sets a callback to be invoked whenever a connection is re-opened after being auto-closed,
     * thus requiring configuration.
     */
    fun setAutoOpenCallback(callback: (SQLiteConnection) -> Unit) {
        onAutoOpenCallback = callback
    }

    fun close() {
        if (!explicitlyClose) {
            explicitlyClose = true
            autoCloseJob?.cancel()
            autoCloseJob = null
            lock.withLock {
                // Copy connections since explicitly closing them will cause them to unregister,
                // removing them from the original list.
                val connectionsToClose = connections.toList()
                connectionsToClose.forEach { it.close() }
                check(connections.isEmpty())
            }
        }
    }

    private fun autoCloseConnections() {
        if (watch.getMillis() - lastReferenceTimestamp.get() < config.timeoutInMs) {
            // An increment + decrement beat us to closing the database. We will not close the
            // database, and there should be at least one more auto-close job launched.
            return
        }
        if (referenceCount.get() != 0) {
            // An increment beat us to closing the database. We will not close the database, and
            // another auto-close job will be launched once the ref count is decremented.
            return
        }
        lock.withLock { connections.forEach { it.autoClose() } }
        onAutoCloseCallback.invoke()
    }

    /** Represents a counting time tracker function. */
    fun interface Watch {
        fun getMillis(): Long
    }
}
