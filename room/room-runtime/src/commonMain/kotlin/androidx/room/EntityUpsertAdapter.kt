/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteException

/**
 * This class knows how to insert an entity. When the insertion fails due to a unique constraint
 * conflict (i.e. primary key conflict), it will perform an update.
 *
 * @param T the type param of the entity to be upserted
 * @constructor Creates an EntityUpsertionAdapter that can upsert entity of type T into the database
 *   using the given insertionAdapter to perform insertion and updateAdapter to perform update when
 *   the insertion fails
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class EntityUpsertAdapter<T>(
    private val entityInsertAdapter: EntityInsertAdapter<T>,
    private val updateAdapter: EntityDeleteOrUpdateAdapter<T>
) {

    /**
     * Inserts the entity into the database. If a constraint exception is thrown i.e. a primary key
     * conflict, update the existing entity.
     *
     * @param entity The entity to insert
     */
    fun upsert(connection: SQLiteConnection, entity: T?) {
        try {
            entityInsertAdapter.insert(connection, entity)
        } catch (ex: SQLiteException) {
            checkUniquenessException(ex)
            updateAdapter.handle(connection, entity)
        }
    }

    /**
     * Upserts (insert or update) the given entities into the database. For each entity, insert if
     * it is not already in the database update if there is a constraint conflict.
     *
     * @param entities array of entities to upsert
     */
    fun upsert(connection: SQLiteConnection, entities: Array<out T?>?) {
        if (entities == null) return
        entities.forEach { entity ->
            try {
                entityInsertAdapter.insert(connection, entity)
            } catch (ex: SQLiteException) {
                checkUniquenessException(ex)
                updateAdapter.handle(connection, entity)
            }
        }
    }

    fun upsert(connection: SQLiteConnection, entities: Iterable<T?>?) {
        if (entities == null) return
        entities.forEach { entity ->
            try {
                entityInsertAdapter.insert(connection, entity)
            } catch (ex: SQLiteException) {
                checkUniquenessException(ex)
                updateAdapter.handle(connection, entity)
            }
        }
    }

    /**
     * Upserts the given entity into the database and returns the row id. If the insertion failed,
     * update the existing entity and return -1L.
     *
     * @param entity The entity to upsert
     * @return The SQLite row id or -1L if the insertion failed and update is performed
     */
    fun upsertAndReturnId(connection: SQLiteConnection, entity: T?): Long {
        return try {
            entityInsertAdapter.insertAndReturnId(connection, entity)
        } catch (ex: SQLiteException) {
            checkUniquenessException(ex)
            updateAdapter.handle(connection, entity)
            -1L
        }
    }

    /**
     * Upserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to upsert
     * @return The SQLite row ids, for entities that are not inserted the row id returned will be
     *   -1L
     */
    fun upsertAndReturnIdsArray(connection: SQLiteConnection, entities: Array<out T?>?): LongArray {
        if (entities == null) return longArrayOf()
        return LongArray(entities.size) { index ->
            try {
                entityInsertAdapter.insertAndReturnId(connection, entities[index])
            } catch (ex: SQLiteException) {
                checkUniquenessException(ex)
                updateAdapter.handle(connection, entities[index])
                -1L
            }
        }
    }

    fun upsertAndReturnIdsArray(
        connection: SQLiteConnection,
        entities: Collection<T?>?
    ): LongArray {
        if (entities == null) return longArrayOf()
        return LongArray(entities.size) { index ->
            try {
                entityInsertAdapter.insertAndReturnId(connection, entities.elementAt(index))
            } catch (ex: SQLiteException) {
                checkUniquenessException(ex)
                updateAdapter.handle(connection, entities.elementAt(index))
                -1L
            }
        }
    }

    fun upsertAndReturnIdsList(connection: SQLiteConnection, entities: Array<out T?>?): List<Long> {
        if (entities == null) return emptyList()
        return buildList {
            entities.forEach { entity ->
                try {
                    add(entityInsertAdapter.insertAndReturnId(connection, entity))
                } catch (ex: SQLiteException) {
                    checkUniquenessException(ex)
                    updateAdapter.handle(connection, entity)
                    add(-1L)
                }
            }
        }
    }

    fun upsertAndReturnIdsList(
        connection: SQLiteConnection,
        entities: Collection<T?>?
    ): List<Long> {
        if (entities == null) return emptyList()
        return buildList {
            entities.forEach { entity ->
                try {
                    add(entityInsertAdapter.insertAndReturnId(connection, entity))
                } catch (ex: SQLiteException) {
                    checkUniquenessException(ex)
                    updateAdapter.handle(connection, entity)
                    add(-1L)
                }
            }
        }
    }

    fun upsertAndReturnIdsArrayBox(
        connection: SQLiteConnection,
        entities: Array<out T?>?
    ): Array<out Long> {
        if (entities == null) return emptyArray()
        return Array(entities.size) { index ->
            try {
                entityInsertAdapter.insertAndReturnId(connection, entities[index])
            } catch (ex: SQLiteException) {
                checkUniquenessException(ex)
                updateAdapter.handle(connection, entities[index])
                -1L
            }
        }
    }

    fun upsertAndReturnIdsArrayBox(
        connection: SQLiteConnection,
        entities: Collection<T?>?
    ): Array<out Long> {
        if (entities == null) return emptyArray()
        return Array(entities.size) { index ->
            try {
                entityInsertAdapter.insertAndReturnId(connection, entities.elementAt(index))
            } catch (ex: SQLiteException) {
                checkUniquenessException(ex)
                updateAdapter.handle(connection, entities.elementAt(index))
                -1L
            }
        }
    }

    /**
     * Verify if the exception is caused by Uniqueness constraint (Primary Key Conflict). If yes,
     * upsert should update the existing one. If not, upsert should re-throw the exception. For
     * android of version newer than KITKAT(19), SQLite supports ErrorCode. Otherwise, check with
     * Error Message.
     *
     * @param ex the exception thrown by the insert attempt
     */
    private fun checkUniquenessException(ex: SQLiteException) {
        val message = ex.message ?: throw ex
        val hasUniqueConstraintEx =
            message.contains(ErrorMsg, ignoreCase = true) ||
                message.contains(SQLITE_CONSTRAINT_UNIQUE) ||
                message.contains(SQLITE_CONSTRAINT_PRIMARYKEY)

        if (!hasUniqueConstraintEx) {
            throw ex
        }
    }

    companion object {
        /**
         * The error code defined by SQLite Library for SQLITE_CONSTRAINT_PRIMARYKEY error Only used
         * by android of version newer than 19.
         */
        private const val SQLITE_CONSTRAINT_PRIMARYKEY = "1555"

        /** The error code defined by SQLite Library for SQLITE_CONSTRAINT_UNIQUE error. */
        private const val SQLITE_CONSTRAINT_UNIQUE = "2067"

        /**
         * For android of version below and including 19, use error message instead of error code to
         * check
         */
        private const val ErrorMsg = "unique"
    }
}
