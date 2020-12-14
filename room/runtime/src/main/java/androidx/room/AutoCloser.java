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

package androidx.room;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import androidx.room.util.SneakyThrow;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * AutoCloser is responsible for automatically opening (using
 * delegateOpenHelper) and closing (on a timer started when there are no remaining references) a
 * SupportSqliteDatabase.
 *
 * It is important to ensure that the ref count is incremented when using a returned database.
 */
final class AutoCloser {

    @Nullable
    private SupportSQLiteOpenHelper mDelegateOpenHelper = null;

    @NonNull
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    // Package private for access from mAutoCloser
    @NonNull
    final Object mLock = new Object();

    // Package private for access from mAutoCloser
    final long mAutoCloseTimeoutInMs;

    // Package private for access from mExecuteAutoCloser
    @NonNull
    final Executor mExecutor;

    // Package private for access from mAutoCloser
    @GuardedBy("mLock")
    int mRefCount = 0;

    // Package private for access from mAutoCloser
    @GuardedBy("mLock")
    long mLastDecrementRefCountTimeStamp = SystemClock.uptimeMillis();

    // The unwrapped SupportSqliteDatabase
    // Package private for access from mAutoCloser
    @GuardedBy("mLock")
    @Nullable
    SupportSQLiteDatabase mDelegateDatabase;

    private final Runnable mExecuteAutoCloser = new Runnable() {
        @Override
        public void run() {
            mExecutor.execute(mAutoCloser);
        }
    };

    // Package private for access from mExecuteAutoCloser
    @NonNull
    final Runnable mAutoCloser = new Runnable() {
        @Override
        public void run() {
            synchronized (mLock) {
                if (SystemClock.uptimeMillis() - mLastDecrementRefCountTimeStamp
                        < mAutoCloseTimeoutInMs) {
                    // An increment + decrement beat us to closing the db. We
                    // will not close the database, and there should be at least
                    // one more auto-close scheduled.
                    return;
                }

                if (mRefCount != 0) {
                    // An increment beat us to closing the db. We don't close the
                    // db, and another closer will be scheduled once the ref
                    // count is decremented.
                    return;
                }

                if (mDelegateDatabase != null) {
                    try {
                        mDelegateDatabase.close();
                    } catch (IOException e) {
                        SneakyThrow.reThrow(e);
                    }
                    mDelegateDatabase = null;
                }
            }
        }
    };


    /**
     * Construct an AutoCloser.
     *
     * @param autoCloseTimeoutAmount time for auto close timer
     * @param autoCloseTimeUnit      time unit for autoCloseTimeoutAmount
     * @param autoCloseExecutor      the executor on which the auto close operation will happen
     */
    AutoCloser(long autoCloseTimeoutAmount,
            @NonNull TimeUnit autoCloseTimeUnit,
            @NonNull Executor autoCloseExecutor) {
        mAutoCloseTimeoutInMs = autoCloseTimeUnit.toMillis(autoCloseTimeoutAmount);
        mExecutor = autoCloseExecutor;
    }

    /**
     * Since we need to construct the AutoCloser in the RoomDatabase.Builder, we need to set the
     * delegateOpenHelper after construction.
     *
     * @param delegateOpenHelper the open helper that is used to create
     *                           new SupportSqliteDatabases
     */
    public void init(@NonNull SupportSQLiteOpenHelper delegateOpenHelper) {
        if (mDelegateOpenHelper != null) {
            Log.e(Room.LOG_TAG, "AutoCloser initialized multiple times. This is probably a bug in"
                    + " the room code.");
            return;
        }
        this.mDelegateOpenHelper = delegateOpenHelper;
    }

    /**
     * Execute a ref counting function. The function will receive an unwrapped open database and
     * this database will stay open until at least after function returns. If there are no more
     * references in use for the db once function completes, an auto close operation will be
     * scheduled.
     */
    @Nullable
    public <V> V executeRefCountingFunction(@NonNull Function<SupportSQLiteDatabase, V> function) {
        try {
            SupportSQLiteDatabase db = incrementCountAndEnsureDbIsOpen();
            return function.apply(db);
        } finally {
            decrementCountAndScheduleClose();
        }
    }

    /**
     * Confirms that autoCloser is no longer running and confirms that mDelegateDatabase is set
     * and open. mDelegateDatabase will not be auto closed until
     * decrementRefCountAndScheduleClose is called. decrementRefCountAndScheduleClose must be
     * called once for each call to incrementCountAndEnsureDbIsOpen.
     *
     * If this throws an exception, decrementCountAndScheduleClose must still be called!
     *
     * @return the *unwrapped* SupportSQLiteDatabase.
     */
    @NonNull
    public SupportSQLiteDatabase incrementCountAndEnsureDbIsOpen() {
        //TODO(rohitsat): avoid synchronized(mLock) when possible. We should be able to avoid it
        // when refCount is not hitting zero or if there is no auto close scheduled if we use
        // Atomics.
        synchronized (mLock) {
            // If there is a scheduled autoclose operation, we should remove it from the handler.
            mHandler.removeCallbacks(mExecuteAutoCloser);

            mRefCount++;

            if (mDelegateDatabase != null && mDelegateDatabase.isOpen()) {
                return mDelegateDatabase;
            } else if (mDelegateDatabase != null) {
                // This shouldn't happen
                throw new IllegalStateException("mDelegateDatabase is closed but non-null");
            }

            // Get the database while holding `mLock` so no other threads try to create it or
            // destroy it.
            if (mDelegateOpenHelper != null) {
                mDelegateDatabase = mDelegateOpenHelper.getWritableDatabase();
            } else {
                throw new IllegalStateException("AutoCloser has not beeninitialized. This "
                        + "shouldn't happen, but if it does it means there's a bug in our code");
            }

            return mDelegateDatabase;
        }
    }

    /**
     * Decrements the ref count and schedules a close if there are no other references to the db.
     * This must only be called after a corresponding incrementCountAndEnsureDbIsOpen call.
     */
    public void decrementCountAndScheduleClose() {
        //TODO(rohitsat): avoid synchronized(mLock) when possible
        synchronized (mLock) {
            if (mRefCount <= 0) {
                throw new IllegalStateException("ref count is 0 or lower but we're supposed to "
                        + "decrement");
            }

            // decrement refCount
            mRefCount--;

            // if refcount is zero, schedule close operation
            if (mRefCount == 0) {
                if (mDelegateDatabase == null) {
                    // No db to close, this can happen due to exceptions when creating db...
                    return;
                }
                mHandler.postDelayed(mExecuteAutoCloser, mAutoCloseTimeoutInMs);
            }
        }
    }

    /**
     * Returns the underlying database. This does not ensure that the database is open; the
     * caller is responsible for ensuring that the database is open and the ref count is non-zero.
     *
     * This is primarily meant for use cases where we don't want to open the database (isOpen) or
     * we know that the database is already open (KeepAliveCursor).
     */
    @Nullable // Since the db might be closed
    public SupportSQLiteDatabase getDelegateDatabase() {
        synchronized (mLock) {
            return mDelegateDatabase;
        }
    }

    /**
     * Returns the current ref count for this auto closer. This is only visible for testing.
     *
     * @return current ref count
     */
    @VisibleForTesting
    public int getRefCountForTest() {
        synchronized (mLock) {
            return mRefCount;
        }
    }
}
