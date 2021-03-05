/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.sqlite.inspection;

import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.sqlite.inspection.SqliteInspector.DatabaseConnection;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Handles database locking and associated bookkeeping.
 * Thread-safe.
 */
public class DatabaseLockRegistry {
    @VisibleForTesting public static int sTimeoutMs = 5000;

    private final Object mLock = new Object(); // used for synchronization within the class
    @GuardedBy("mLock") private final Map<Integer, Lock> mLockIdToLockMap = new HashMap<>();
    @GuardedBy("mLock") private final Map<Integer, Lock> mDatabaseIdToLockMap = new HashMap<>();
    @GuardedBy("mLock") private int mNextLockId = 1;

    // A dedicated thread required as database transactions are tied to a thread. In order to
    // release a lock, we need to use the same thread as the one we used to establish the lock.
    // Thread names need to start with 'Studio:' as per some framework limitations.
    @NonNull private final Executor mExecutor =
            Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "Studio:Sql:Lock"); // limit = 15 characters
                    thread.setDaemon(true);
                    return thread;
                }
            });

    /**
     * Locks a database identified by the provided database id. If a lock on the database is
     * already in place, an existing lock will be issued. Locks keep count of simultaneous
     * requests, so that the database is only unlocked once all callers release their issued locks.
     */
    public int acquireLock(int databaseId, @NonNull SQLiteDatabase database) throws Exception {
        synchronized (mLock) {
            Lock lock = mDatabaseIdToLockMap.get(databaseId);
            if (lock == null) {
                lock = new Lock(mNextLockId++, databaseId, database);
                lockDatabase(lock.mDatabase);
                mLockIdToLockMap.put(lock.mLockId, lock);
                mDatabaseIdToLockMap.put(lock.mDatabaseId, lock);
            }
            lock.mCount++;
            return lock.mLockId;
        }
    }

    /**
     * Releases a lock on a database identified by the provided lock id. If the same lock has been
     * provided multiple times (for lock requests on an already locked database), the lock
     * needs to be released by all previous requestors for the database to get unlocked.
     */
    public void releaseLock(int lockId) throws Exception {
        synchronized (mLock) {
            Lock lock = mLockIdToLockMap.get(lockId);
            if (lock == null) throw new IllegalArgumentException("No lock with id: " + lockId);

            if (--lock.mCount == 0) {
                try {
                    unlockDatabase(lock.mDatabase);
                } catch (Exception e) {
                    lock.mCount++; // correct the count
                    throw e;
                }
                mLockIdToLockMap.remove(lock.mLockId);
                mDatabaseIdToLockMap.remove(lock.mDatabaseId);
            }
        }
    }

    /**
     * @return `null` if the database is not locked; the database and the executor that locked the
     * database otherwise
     */
    @Nullable DatabaseConnection getConnection(int databaseId) {
        synchronized (mLock) {
            Lock lock = mDatabaseIdToLockMap.get(databaseId);
            return (lock == null)
                    ? null
                    : new DatabaseConnection(lock.mDatabase, mExecutor);
        }
    }

    /**
     * Starts a database transaction and acquires an extra database reference to keep the database
     * open while the lock is in place.
     */
    private void lockDatabase(final SQLiteDatabase database) throws Exception {
        // keeps the database open while a lock is in place; released when the lock is released
        boolean keepOpenReferenceAcquired = false;

        final CancellationSignal cancellationSignal = new CancellationSignal();
        Future<?> future = null;
        try {
            database.acquireReference();
            keepOpenReferenceAcquired = true;

            // Submitting a Runnable, so we can set a timeout.
            future = SqliteInspectionExecutors.submit(mExecutor, new Runnable() {
                @Override
                public void run() {
                    // starts a transaction
                    database.rawQuery("BEGIN IMMEDIATE;", new String[0], cancellationSignal)
                            .getCount(); // forces the cursor to execute the query
                }
            });
            future.get(sTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            if (keepOpenReferenceAcquired) database.releaseReference();
            cancellationSignal.cancel();
            if (future != null) future.cancel(true);
            throw e;
        }
    }

    /**
     * Ends the database transaction and releases the extra database reference that kept the
     * database open while the lock was in place.
     */
    private void unlockDatabase(final SQLiteDatabase database) throws Exception {
        final CancellationSignal cancellationSignal = new CancellationSignal();
        Future<?> future = null;
        try {
            // Submitting a Runnable, so we can set a timeout.
            future = SqliteInspectionExecutors.submit(mExecutor, new Runnable() {
                @Override
                public void run() {
                    // ends the transaction
                    database.rawQuery("ROLLBACK;", new String[0], cancellationSignal)
                            .getCount(); // forces the cursor to execute the query
                    database.releaseReference();
                }
            });
            future.get(sTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            cancellationSignal.cancel();
            if (future != null) future.cancel(true);
            throw e;
        }
    }

    private static final class Lock {
        final int mLockId;
        final int mDatabaseId;
        final SQLiteDatabase mDatabase;
        int mCount = 0; // number of simultaneous locks secured on the database

        Lock(int lockId, int databaseId, SQLiteDatabase database) {
            this.mLockId = lockId;
            this.mDatabaseId = databaseId;
            this.mDatabase = database;
        }
    }
}
