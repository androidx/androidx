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

import com.android.support.db.SupportSQLiteDatabase;
import com.android.support.db.SupportSQLiteStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementations of this class knows how to insert a particular entity.
 * <p>
 * This is an internal library class and all of its implementations are auto-generated.
 *
 * @param <T> The type parameter of the entity to be inserted
 * @hide
 */

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class EntityInsertionAdapter<T> {
    private final AtomicBoolean mStmtLock = new AtomicBoolean(false);
    private final SupportSQLiteDatabase mDatabase;
    private volatile SupportSQLiteStatement mStmt;

    /**
     * Creates an InsertionAdapter that can insert the entity type T into the given database.
     *
     * @param database The database to insert into.
     */
    public EntityInsertionAdapter(SupportSQLiteDatabase database) {
        mDatabase = database;
    }

    /**
     * Create the insertion query
     *
     * @return An SQL query that can insert instances of T.
     */
    protected abstract String createInsertQuery();

    /**
     * Binds the entity into the given statement.
     *
     * @param statement The sqlite statement that prepared for the query returned from
     *                  createInsertQuery.
     * @param entity    The entity of type T.
     */
    protected abstract void bind(SupportSQLiteStatement statement, T entity);

    private SupportSQLiteStatement createNewStatement() {
        String query = createInsertQuery();
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
     * Inserts the entity into the database.
     *
     * @param entity The entity to insert
     */
    public final void insert(T entity) {
        boolean useCached = !mStmtLock.getAndSet(true);
        try {
            final SupportSQLiteStatement stmt = getStmt(useCached);
            bind(stmt, entity);
            stmt.executeInsert();
        } finally {
            if (useCached) {
                mStmtLock.set(false);
            }
        }
    }

    /**
     * Inserts the given entities into the database.
     *
     * @param entities Entities to insert
     */
    public final void insert(T[] entities) {
        boolean useCached = !mStmtLock.getAndSet(true);
        try {
            final SupportSQLiteStatement stmt = getStmt(useCached);
            for (T entity : entities) {
                bind(stmt, entity);
            }
        } finally {
            if (useCached) {
                mStmtLock.set(false);
            }
        }
    }

    /**
     * Inserts the given entities into the database.
     *
     * @param entities Entities to insert
     */
    public final void insert(List<T> entities) {
        boolean useCached = !mStmtLock.getAndSet(true);
        try {
            final SupportSQLiteStatement stmt = getStmt(useCached);
            for (T entity : entities) {
                bind(stmt, entity);
            }
        } finally {
            if (useCached) {
                mStmtLock.set(false);
            }
        }
    }

    /**
     * Inserts the given entity into the database and returns the row id.
     *
     * @param entity The entity to insert
     * @return The Sqlite row id
     */
    public final long insertAndReturnId(T entity) {
        boolean useCached = !mStmtLock.getAndSet(true);
        try {
            final SupportSQLiteStatement stmt = getStmt(useCached);
            bind(stmt, entity);
            return stmt.executeInsert();
        } finally {
            if (useCached) {
                mStmtLock.set(false);
            }
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The Sqlite row ids
     */
    public final long[] insertAndReturnIdsArray(List<T> entities) {
        boolean useCached = !mStmtLock.getAndSet(true);
        try {
            final long[] result = new long[entities.size()];
            final SupportSQLiteStatement stmt = getStmt(useCached);
            int index = 0;
            for (T entity : entities) {
                bind(stmt, entity);
                result[index] = stmt.executeInsert();
                index++;
            }
            return result;
        } finally {
            if (useCached) {
                mStmtLock.set(false);
            }
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The Sqlite row ids
     */
    public final long[] insertAndReturnIdsArray(T[] entities) {
        boolean useCached = !mStmtLock.getAndSet(true);
        try {
            final long[] result = new long[entities.length];
            final SupportSQLiteStatement stmt = getStmt(useCached);
            int index = 0;
            for (T entity : entities) {
                bind(stmt, entity);
                result[index] = stmt.executeInsert();
                index++;
            }
            return result;
        } finally {
            if (useCached) {
                mStmtLock.set(false);
            }
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The Sqlite row ids
     */
    public final List<Long> insertAndReturnIdsList(T[] entities) {
        boolean useCached = !mStmtLock.getAndSet(true);
        try {
            final List<Long> result = new ArrayList<>(entities.length);
            final SupportSQLiteStatement stmt = getStmt(useCached);
            int index = 0;
            for (T entity : entities) {
                bind(stmt, entity);
                result.add(index, stmt.executeInsert());
                index++;
            }
            return result;
        } finally {
            if (useCached) {
                mStmtLock.set(false);
            }
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The Sqlite row ids
     */
    public final List<Long> insertAndReturnIdsList(List<T> entities) {
        boolean useCached = !mStmtLock.getAndSet(true);
        try {
            final List<Long> result = new ArrayList<>(entities.size());
            final SupportSQLiteStatement stmt = getStmt(useCached);
            int index = 0;
            for (T entity : entities) {
                bind(stmt, entity);
                result.add(index, stmt.executeInsert());
                index++;
            }
            return result;
        } finally {
            if (useCached) {
                mStmtLock.set(false);
            }
        }
    }
}
