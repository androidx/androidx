/*
 * Copyright (C) 2016 The Android Open Source Project
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
package android.arch.persistence.room;

import android.arch.persistence.db.SupportSQLiteStatement;
import android.support.annotation.RestrictTo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a prepared SQLite state that can be re-used multiple times.
 * <p>
 * This class is used by generated code. After it is used, {@code release} must be called so that
 * it can be used by other threads.
 * <p>
 * To avoid re-entry even within the same thread, this class allows only 1 time access to the shared
 * statement until it is released.
 *
 * @hide
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class SharedSQLiteStatement {
    private final AtomicBoolean mLock = new AtomicBoolean(false);

    private final RoomDatabase mDatabase;
    private volatile SupportSQLiteStatement mStmt;

    /**
     * Creates an SQLite prepared statement that can be re-used across threads. If it is in use,
     * it automatically creates a new one.
     *
     * @param database The database to create the statement in.
     */
    public SharedSQLiteStatement(RoomDatabase database) {
        mDatabase = database;
    }

    /**
     * Create the query.
     *
     * @return The SQL query to prepare.
     */
    protected abstract String createQuery();

    protected void assertNotMainThread() {
        mDatabase.assertNotMainThread();
    }

    private SupportSQLiteStatement createNewStatement() {
        String query = createQuery();
        return mDatabase.compileStatement(query);
    }

    private SupportSQLiteStatement getStmt(boolean canUseCached) {
        final SupportSQLiteStatement stmt;
        if (canUseCached) {
            if (mStmt == null) {
                mStmt = createNewStatement();
            }
            stmt = mStmt;
        } else {
            // it is in use, create a one off statement
            stmt = createNewStatement();
        }
        return stmt;
    }

    /**
     * Call this to get the statement. Must call {@link #release(SupportSQLiteStatement)} once done.
     */
    public SupportSQLiteStatement acquire() {
        assertNotMainThread();
        return getStmt(mLock.compareAndSet(false, true));
    }

    /**
     * Must call this when statement will not be used anymore.
     *
     * @param statement The statement that was returned from acquire.
     */
    public void release(SupportSQLiteStatement statement) {
        if (statement == mStmt) {
            mLock.set(false);
        }
    }
}
