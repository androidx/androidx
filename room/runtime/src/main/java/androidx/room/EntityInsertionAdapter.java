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

package androidx.room;

import androidx.annotation.RestrictTo;
import androidx.sqlite.db.SupportSQLiteStatement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Implementations of this class knows how to insert a particular entity.
 * <p>
 * This is an internal library class and all of its implementations are auto-generated.
 *
 * @param <T> The type parameter of the entity to be inserted
 * @hide
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class EntityInsertionAdapter<T> extends SharedSQLiteStatement {
    /**
     * Creates an InsertionAdapter that can insert the entity type T into the given database.
     *
     * @param database The database to insert into.
     */
    public EntityInsertionAdapter(RoomDatabase database) {
        super(database);
    }

    /**
     * Binds the entity into the given statement.
     *
     * @param statement The SQLite statement that prepared for the query returned from
     *                  createInsertQuery.
     * @param entity    The entity of type T.
     */
    protected abstract void bind(SupportSQLiteStatement statement, T entity);

    /**
     * Inserts the entity into the database.
     *
     * @param entity The entity to insert
     */
    public final void insert(T entity) {
        final SupportSQLiteStatement stmt = acquire();
        try {
            bind(stmt, entity);
            stmt.executeInsert();
        } finally {
            release(stmt);
        }
    }

    /**
     * Inserts the given entities into the database.
     *
     * @param entities Entities to insert
     */
    public final void insert(T[] entities) {
        final SupportSQLiteStatement stmt = acquire();
        try {
            for (T entity : entities) {
                bind(stmt, entity);
                stmt.executeInsert();
            }
        } finally {
            release(stmt);
        }
    }

    /**
     * Inserts the given entities into the database.
     *
     * @param entities Entities to insert
     */
    public final void insert(Iterable<T> entities) {
        final SupportSQLiteStatement stmt = acquire();
        try {
            for (T entity : entities) {
                bind(stmt, entity);
                stmt.executeInsert();
            }
        } finally {
            release(stmt);
        }
    }

    /**
     * Inserts the given entity into the database and returns the row id.
     *
     * @param entity The entity to insert
     * @return The SQLite row id
     */
    public final long insertAndReturnId(T entity) {
        final SupportSQLiteStatement stmt = acquire();
        try {
            bind(stmt, entity);
            return stmt.executeInsert();
        } finally {
            release(stmt);
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids
     */
    public final long[] insertAndReturnIdsArray(Collection<T> entities) {
        final SupportSQLiteStatement stmt = acquire();
        try {
            final long[] result = new long[entities.size()];
            int index = 0;
            for (T entity : entities) {
                bind(stmt, entity);
                result[index] = stmt.executeInsert();
                index++;
            }
            return result;
        } finally {
            release(stmt);
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids
     */
    public final long[] insertAndReturnIdsArray(T[] entities) {
        final SupportSQLiteStatement stmt = acquire();
        try {
            final long[] result = new long[entities.length];
            int index = 0;
            for (T entity : entities) {
                bind(stmt, entity);
                result[index] = stmt.executeInsert();
                index++;
            }
            return result;
        } finally {
            release(stmt);
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids
     */
    public final Long[] insertAndReturnIdsArrayBox(Collection<T> entities) {
        final SupportSQLiteStatement stmt = acquire();
        try {
            final Long[] result = new Long[entities.size()];
            int index = 0;
            for (T entity : entities) {
                bind(stmt, entity);
                result[index] = stmt.executeInsert();
                index++;
            }
            return result;
        } finally {
            release(stmt);
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids
     */
    public final Long[] insertAndReturnIdsArrayBox(T[] entities) {
        final SupportSQLiteStatement stmt = acquire();
        try {
            final Long[] result = new Long[entities.length];
            int index = 0;
            for (T entity : entities) {
                bind(stmt, entity);
                result[index] = stmt.executeInsert();
                index++;
            }
            return result;
        } finally {
            release(stmt);
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids
     */
    public final List<Long> insertAndReturnIdsList(T[] entities) {
        final SupportSQLiteStatement stmt = acquire();
        try {
            final List<Long> result = new ArrayList<>(entities.length);
            int index = 0;
            for (T entity : entities) {
                bind(stmt, entity);
                result.add(index, stmt.executeInsert());
                index++;
            }
            return result;
        } finally {
            release(stmt);
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids
     */
    public final List<Long> insertAndReturnIdsList(Collection<T> entities) {
        final SupportSQLiteStatement stmt = acquire();
        try {
            final List<Long> result = new ArrayList<>(entities.size());
            int index = 0;
            for (T entity : entities) {
                bind(stmt, entity);
                result.add(index, stmt.executeInsert());
                index++;
            }
            return result;
        } finally {
            release(stmt);
        }
    }
}
