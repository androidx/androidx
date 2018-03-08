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
public abstract class EntityDeletionOrUpdateAdapter<T> extends SharedSQLiteStatement {
    /**
     * Creates a DeletionOrUpdateAdapter that can delete or update the entity type T on the given
     * database.
     *
     * @param database The database to delete / update the item in.
     */
    public EntityDeletionOrUpdateAdapter(RoomDatabase database) {
        super(database);
    }

    /**
     * Create the deletion or update query
     *
     * @return An SQL query that can delete or update instances of T.
     */
    @Override
    protected abstract String createQuery();

    /**
     * Binds the entity into the given statement.
     *
     * @param statement The SQLite statement that prepared for the query returned from
     *                  createQuery.
     * @param entity    The entity of type T.
     */
    protected abstract void bind(SupportSQLiteStatement statement, T entity);

    /**
     * Deletes or updates the given entities in the database and returns the affected row count.
     *
     * @param entity The entity to delete or update
     * @return The number of affected rows
     */
    public final int handle(T entity) {
        final SupportSQLiteStatement stmt = acquire();
        try {
            bind(stmt, entity);
            return stmt.executeUpdateDelete();
        } finally {
            release(stmt);
        }
    }

    /**
     * Deletes or updates the given entities in the database and returns the affected row count.
     *
     * @param entities Entities to delete or update
     * @return The number of affected rows
     */
    public final int handleMultiple(Iterable<T> entities) {
        final SupportSQLiteStatement stmt = acquire();
        try {
            int total = 0;
            for (T entity : entities) {
                bind(stmt, entity);
                total += stmt.executeUpdateDelete();
            }
            return total;
        } finally {
            release(stmt);
        }
    }

    /**
     * Deletes or updates the given entities in the database and returns the affected row count.
     *
     * @param entities Entities to delete or update
     * @return The number of affected rows
     */
    public final int handleMultiple(T[] entities) {
        final SupportSQLiteStatement stmt = acquire();
        try {
            int total = 0;
            for (T entity : entities) {
                bind(stmt, entity);
                total += stmt.executeUpdateDelete();
            }
            return total;
        } finally {
            release(stmt);
        }
    }
}
