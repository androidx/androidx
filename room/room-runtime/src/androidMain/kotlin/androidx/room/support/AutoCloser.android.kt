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
package androidx.room.support

import android.os.SystemClock
import androidx.annotation.GuardedBy
import androidx.room.support.AutoCloser.Watch
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * AutoCloser is responsible for automatically opening (using `delegateOpenHelper`) and closing (on
 * a timer started when there are no remaining references) a [SupportSQLiteDatabase].
 *
 * It is important to ensure that the reference count is incremented when using a returned database.
 *
 * @param timeoutAmount time for auto close timer
 * @param timeUnit time unit for `timeoutAmount`
 * @param watch A [Watch] implementation to get an increasing timestamp.
 */
internal class AutoCloser(
    timeoutAmount: Long,
    timeUnit: TimeUnit,
    private val watch: Watch = Watch { SystemClock.uptimeMillis() }
) {
    // The unwrapped SupportSQLiteOpenHelper (i.e. not AutoClosingRoomOpenHelper)
    private lateinit var delegateOpenHelper: SupportSQLiteOpenHelper

    private lateinit var coroutineScope: CoroutineScope

    private var onAutoCloseCallback: (() -> Unit)? = null

    private val lock = Any()

    private val autoCloseTimeoutInMs = timeUnit.toMillis(timeoutAmount)

    private val referenceCount = AtomicInteger(0)

    private var lastDecrementRefCountTimeStamp = AtomicLong(watch.getMillis())

    // The unwrapped SupportSqliteDatabase (i.e. not AutoCloseSupportSQLiteDatabase)
    @GuardedBy("lock") internal var delegateDatabase: SupportSQLiteDatabase? = null

    private var manuallyClosed = false

    private var autoCloseJob: Job? = null

    private fun autoCloseDatabase(): Unit =
        synchronized(lock) {
            if (watch.getMillis() - lastDecrementRefCountTimeStamp.get() < autoCloseTimeoutInMs) {
                // An increment + decrement beat us to closing the db. We
                // will not close the database, and there should be at least
                // one more auto-close scheduled.
                return
            }
            if (referenceCount.get() != 0) {
                // An increment beat us to closing the db. We don't close the
                // db, and another closer will be scheduled once the ref
                // count is decremented.
                return
            }
            onAutoCloseCallback?.invoke()
                ?: error(
                    "onAutoCloseCallback is null but it should  have been set before use. " +
                        "Please file a bug against Room at: $BUG_LINK"
                )
            delegateDatabase?.let {
                if (it.isOpen) {
                    it.close()
                }
            }
            delegateDatabase = null
        }

    /**
     * Since we need to construct the AutoCloser in the [androidx.room.RoomDatabase.Builder], we
     * need to set the `delegateOpenHelper` after construction.
     *
     * @param delegateOpenHelper the open helper that is used to create new [SupportSQLiteDatabase].
     */
    fun initOpenHelper(delegateOpenHelper: SupportSQLiteOpenHelper) {
        require(delegateOpenHelper !is AutoClosingRoomOpenHelper)
        this.delegateOpenHelper = delegateOpenHelper
    }

    /**
     * Since we need to construct the AutoCloser in the [androidx.room.RoomDatabase.Builder], we
     * need to set the `coroutineScope` after construction.
     *
     * @param coroutineScope where the auto close will execute.
     */
    fun initCoroutineScope(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
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
     * Confirms that auto-close function is no longer running and confirms that `delegateDatabase`
     * is set and open. `delegateDatabase` will not be auto closed until
     * [decrementCountAndScheduleClose] is called. [decrementCountAndScheduleClose] must be called
     * once for each call to [incrementCountAndEnsureDbIsOpen].
     *
     * If this throws an exception, [decrementCountAndScheduleClose] must still be called!
     *
     * @return the *unwrapped* SupportSQLiteDatabase.
     */
    fun incrementCountAndEnsureDbIsOpen(): SupportSQLiteDatabase {
        // If there is a scheduled auto close operation, cancel it.
        autoCloseJob?.cancel()
        autoCloseJob = null

        referenceCount.incrementAndGet()
        check(!manuallyClosed) { "Attempting to open already closed database." }
        synchronized(lock) {
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
     * This must only be called after a corresponding [incrementCountAndEnsureDbIsOpen] call.
     */
    fun decrementCountAndScheduleClose() {
        val newCount = referenceCount.decrementAndGet()
        check(newCount >= 0) { "Unbalanced reference count." }
        lastDecrementRefCountTimeStamp.set(watch.getMillis())
        if (newCount == 0) {
            autoCloseJob =
                coroutineScope.launch {
                    delay(autoCloseTimeoutInMs)
                    autoCloseDatabase()
                }
        }
    }

    /** Close the database if it is still active. */
    fun closeDatabaseIfOpen() {
        synchronized(lock) {
            manuallyClosed = true
            autoCloseJob?.cancel()
            autoCloseJob = null
            delegateDatabase?.close()
            delegateDatabase = null
        }
    }

    /**
     * The auto closer is still active if the database has not been closed. This means that whether
     * or not the underlying database is closed, when active we will re-open it on the next access.
     *
     * @return a boolean indicating whether the auto closer is still active
     */
    val isActive: Boolean
        get() = !manuallyClosed

    /**
     * Sets a callback that will be run every time the database is auto-closed. This callback needs
     * to be lightweight since it is run while holding a lock.
     *
     * @param onAutoClose the callback to run
     */
    fun setAutoCloseCallback(onAutoClose: () -> Unit) {
        onAutoCloseCallback = onAutoClose
    }

    /** Returns the current auto close callback. This is only visible for testing. */
    internal val autoCloseCallbackForTest
        get() = onAutoCloseCallback

    /** Returns the current ref count for this auto closer. This is only visible for testing. */
    internal val refCountForTest: Int
        get() = referenceCount.get()

    /** Represents a counting time tracker function. */
    fun interface Watch {
        fun getMillis(): Long
    }

    companion object {
        const val BUG_LINK =
            "https://issuetracker.google.com/issues/new?component=413107&template=1096568"
    }
}
