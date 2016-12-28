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

package com.android.support.room;

import android.support.annotation.RestrictTo;

import com.android.support.db.SupportSQLiteStatement;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementations of this class knows how to delete or update a particular entity.
 * <p>
 * This is an internal library class and all of its implementations are auto-generated.
 *
 * @param <T> The type parameter of the entity to be deleted
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class EntityDeletionOrUpdateAdapter<T> {
    private final AtomicBoolean mStmtLock = new AtomicBoolean(false);
    private final RoomDatabase mDatabase;
    private volatile SupportSQLiteStatement mStmt;

    /**
     * Creates a DeletionOrUpdateAdapter that can delete or update the entity type T on the given
     * database.
     *
     * @param database The database to delete / update the item in.
     */
    public EntityDeletionOrUpdateAdapter(RoomDatabase database) {
        mDatabase = database;
    }

    /**
     * Create the deletion or update query
     *
     * @return An SQL query that can delete or update instances of T.
     */
    protected abstract String createQuery();

    /**
     * Binds the entity into the given statement.
     *
     * @param statement The SQLite statement that prepared for the query returned from
     *                  createQuery.
     * @param entity    The entity of type T.
     */
    protected abstract void bind(SupportSQLiteStatement statement, T entity);

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
     * Deletes or updates the given entities in the database and returns the affected row count.
     *
     * @param entity The entity to delete or update
     * @return The number of affected rows
     */
    public final int handle(T entity) {
        boolean useCached = !mStmtLock.getAndSet(true);
        try {
            final SupportSQLiteStatement stmt = getStmt(useCached);
            bind(stmt, entity);
            return stmt.executeUpdateDelete();
        } finally {
            if (useCached) {
                mStmtLock.set(false);
            }
        }
    }

    /**
     * Deletes or updates the given entities in the database and returns the affected row count.
     *
     * @param entities Entities to delete or update
     * @return The number of affected rows
     */
    public final int handleMultiple(Collection<T> entities) {
        boolean useCached = !mStmtLock.getAndSet(true);
        try {
            int total = 0;
            final SupportSQLiteStatement stmt = getStmt(useCached);
            for (T entity : entities) {
                bind(stmt, entity);
                total += stmt.executeUpdateDelete();
            }
            return total;
        } finally {
            if (useCached) {
                mStmtLock.set(false);
            }
        }
    }

    /**
     * Deletes or updates the given entities in the database and returns the affected row count.
     *
     * @param entities Entities to delete or update
     * @return The number of affected rows
     */
    public final int handleMultiple(T[] entities) {
        boolean useCached = !mStmtLock.getAndSet(true);
        try {
            int total = 0;
            final SupportSQLiteStatement stmt = getStmt(useCached);
            for (T entity : entities) {
                bind(stmt, entity);
                total += stmt.executeUpdateDelete();
            }
            return total;
        } finally {
            if (useCached) {
                mStmtLock.set(false);
            }
        }
    }
}
