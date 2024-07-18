/*
 * Copyright 2020 The Android Open Source Project
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

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * AutoCloser is responsible for automatically opening (using
 * delegateOpenHelper) and closing (on a timer started when there are no remaining references) a
 * SupportSqliteDatabase.
 *
 * It is important to ensure that the ref count is incremented when using a returned database.
 *
 * @param autoCloseTimeoutAmount time for auto close timer
 * @param autoCloseTimeUnit      time unit for autoCloseTimeoutAmount
 * @param autoCloseExecutor      the executor on which the auto close operation will happen
 */
internal class AutoCloser(
    autoCloseTimeoutAmount: Long,
    autoCloseTimeUnit: TimeUnit,
    autoCloseExecutor: Executor
) {
    lateinit var delegateOpenHelper: SupportSQLiteOpenHelper
    private val handler = Handler(Looper.getMainLooper())

    internal var onAutoCloseCallback: Runnable? = null

    private val lock = Any()

    private var autoCloseTimeoutInMs: Long = autoCloseTimeUnit.toMillis(autoCloseTimeoutAmount)

    private val executor: Executor = autoCloseExecutor

    @GuardedBy("lock")
    internal var refCount = 0

    @GuardedBy("lock")
    internal var lastDecrementRefCountTimeStamp = SystemClock.uptimeMillis()

    // The unwrapped SupportSqliteDatabase
    @GuardedBy("lock")
    internal var delegateDatabase: SupportSQLiteDatabase? = null

    private var manuallyClosed = false

    private val executeAutoCloser = Runnable { executor.execute(autoCloser) }

    private val autoCloser = Runnable {
        synchronized(lock) {
            if (SystemClock.uptimeMillis() - lastDecrementRefCountTimeStamp
                < autoCloseTimeoutInMs
            ) {
                // An increment + decrement beat us to closing the db. We
                // will not close the database, and there should be at least
                // one more auto-close scheduled.
                return@Runnable
            }
            if (refCount != 0) {
                // An increment beat us to closing the db. We don't close the
                // db, and another closer will be scheduled once the ref
                // count is decremented.
                return@Runnable
            }
            onAutoCloseCallback?.run() ?: error(
                "onAutoCloseCallback is null but it should" +
                    " have been set before use. Please file a bug " +
                    "against Room at: $autoCloseBug"
            )

            delegateDatabase?.let {
                if (it.isOpen) {
                    it.close()
                }
            }
            delegateDatabase = null
        }
    }

    /**
     * Since we need to construct the AutoCloser in the RoomDatabase.Builder, we need to set the
     * delegateOpenHelper after construction.
     *
     * @param delegateOpenHelper the open helper that is used to create
     * new SupportSqliteDatabases
     */
    fun init(delegateOpenHelper: SupportSQLiteOpenHelper) {
        this.delegateOpenHelper = delegateOpenHelper
    }

    /**
     * Execute a ref counting function. The function will receive an unwrapped open database and
     * this database will stay open until at least after function returns. If there are no more
     * references in use for the db once function completes, an auto close operation will be
     * scheduled.
     */
    fun <V> executeRefCountingFunction(block: (SupportSQLiteDatabase) -> V): V =
        try {
            block(incrementCountAndEnsureDbIsOpen())
        } finally {
            decrementCountAndScheduleClose()
        }

    /**
     * Confirms that autoCloser is no longer running and confirms that delegateDatabase is set
     * and open. delegateDatabase will not be auto closed until
     * decrementRefCountAndScheduleClose is called. decrementRefCountAndScheduleClose must be
     * called once for each call to incrementCountAndEnsureDbIsOpen.
     *
     * If this throws an exception, decrementCountAndScheduleClose must still be called!
     *
     * @return the *unwrapped* SupportSQLiteDatabase.
     */
    fun incrementCountAndEnsureDbIsOpen(): SupportSQLiteDatabase {
        // TODO(rohitsat): avoid synchronized(lock) when possible. We should be able to avoid it
        // when refCount is not hitting zero or if there is no auto close scheduled if we use
        // Atomics.
        synchronized(lock) {

            // If there is a scheduled autoclose operation, we should remove it from the handler.
            handler.removeCallbacks(executeAutoCloser)
            refCount++
            check(!manuallyClosed) { "Attempting to open already closed database." }
            delegateDatabase?.let {
                if (it.isOpen) {
                    return it
                }
            }
            return delegateOpenHelper.writableDatabase.also { delegateDatabase = it }
        }
    }

    /**
     * Decrements the ref count and schedules a close if there are no other references to the db.
     * This must only be called after a corresponding incrementCountAndEnsureDbIsOpen call.
     */
    fun decrementCountAndScheduleClose() {
        // TODO(rohitsat): avoid synchronized(lock) when possible
        synchronized(lock) {
            check(refCount > 0) {
                "ref count is 0 or lower but we're supposed to decrement"
            }
            // decrement refCount
            refCount--

            // if refcount is zero, schedule close operation
            if (refCount == 0) {
                if (delegateDatabase == null) {
                    // No db to close, this can happen due to exceptions when creating db...
                    return
                }
                handler.postDelayed(executeAutoCloser, autoCloseTimeoutInMs)
            }
        }
    }

    /**
     * Close the database if it is still active.
     *
     * @throws IOException if an exception is encountered when closing the underlying db.
     */
    @Throws(IOException::class)
    fun closeDatabaseIfOpen() {
        synchronized(lock) {
            manuallyClosed = true
            delegateDatabase?.close()
            delegateDatabase = null
        }
    }

    /**
     * The auto closer is still active if the database has not been closed. This means that
     * whether or not the underlying database is closed, when active we will re-open it on the
     * next access.
     *
     * @return a boolean indicating whether the auto closer is still active
     */
    val isActive: Boolean
        get() = !manuallyClosed

    /**
     * Returns the current ref count for this auto closer. This is only visible for testing.
     *
     * @return current ref count
     */
    @get:VisibleForTesting
    internal val refCountForTest: Int
        get() {
            synchronized(lock) { return refCount }
        }

    /**
     * Sets a callback that will be run every time the database is auto-closed. This callback
     * needs to be lightweight since it is run while holding a lock.
     *
     * @param onAutoClose the callback to run
     */
    fun setAutoCloseCallback(onAutoClose: Runnable) {
        onAutoCloseCallback = onAutoClose
    }

    companion object {
        const val autoCloseBug = "https://issuetracker.google.com/issues/new?component=" +
            "413107&template=1096568"
    }
}
