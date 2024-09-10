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
package androidx.room

import androidx.annotation.RestrictTo
import androidx.sqlite.db.SupportSQLiteStatement

/**
 * Implementations of this class knows how to delete or update a particular entity.
 *
 * This is an internal library class and all of its implementations are auto-generated.
 *
 * @param T The type parameter of the entity to be deleted
 * @constructor Creates a DeletionOrUpdateAdapter that can delete or update the entity type T on the
 *   given database.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Deprecated("No longer used by generated code.", ReplaceWith("EntityDeleteOrUpdateAdapter"))
abstract class EntityDeletionOrUpdateAdapter<T>(database: RoomDatabase) :
    SharedSQLiteStatement(database) {
    /**
     * Create the deletion or update query
     *
     * @return An SQL query that can delete or update instances of T.
     */
    abstract override fun createQuery(): String

    /**
     * Binds the entity into the given statement.
     *
     * @param statement The SQLite statement that prepared for the query returned from createQuery.
     * @param entity The entity of type T.
     */
    protected abstract fun bind(statement: SupportSQLiteStatement, entity: T)

    /**
     * Deletes or updates the given entities in the database and returns the affected row count.
     *
     * @param entity The entity to delete or update
     * @return The number of affected rows
     */
    fun handle(entity: T): Int {
        val stmt: SupportSQLiteStatement = acquire()
        return try {
            bind(stmt, entity)
            stmt.executeUpdateDelete()
        } finally {
            release(stmt)
        }
    }

    /**
     * Deletes or updates the given entities in the database and returns the affected row count.
     *
     * @param entities Entities to delete or update
     * @return The number of affected rows
     */
    fun handleMultiple(entities: Iterable<T>): Int {
        val stmt: SupportSQLiteStatement = acquire()
        return try {
            var total = 0
            entities.forEach { entity ->
                bind(stmt, entity)
                total += stmt.executeUpdateDelete()
            }
            total
        } finally {
            release(stmt)
        }
    }

    /**
     * Deletes or updates the given entities in the database and returns the affected row count.
     *
     * @param entities Entities to delete or update
     * @return The number of affected rows
     */
    fun handleMultiple(entities: Array<out T>): Int {
        val stmt: SupportSQLiteStatement = acquire()
        return try {
            var total = 0
            entities.forEach { entity ->
                bind(stmt, entity)
                total += stmt.executeUpdateDelete()
            }
            total
        } finally {
            release(stmt)
        }
    }
}
