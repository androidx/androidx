/*
 * Copyright 2022 The Android Open Source Project
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

import android.database.sqlite.SQLiteConstraintException
import androidx.annotation.RestrictTo

/**
 * This class knows how to insert an entity. When the insertion fails
 * due to a unique constraint conflict (i.e. primary key conflict),
 * it will perform an update.
 *
 * @constructor Creates an EntityUpsertionAdapter that can upsert entity of type T
 * into the database using the given insertionAdapter to perform insertion and
 * updateAdapter to perform update when the insertion fails
 *
 * @param T the type param of the entity to be upserted
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class EntityUpsertionAdapter<T>(
    private val insertionAdapter: EntityInsertionAdapter<T>,
    private val updateAdapter: EntityDeletionOrUpdateAdapter<T>
) {
    /**
     * Inserts the entity into the database. If a constraint exception is thrown
     * i.e. a primary key conflict, update the existing entity.
     *
     * @param entity The entity to insert
     */
    fun upsert(entity: T) {
        try {
            insertionAdapter.insert(entity)
        } catch (ex: SQLiteConstraintException) {
            updateAdapter.handle(entity)
        }
    }

    /**
     * Upserts (insert or update) the given entities into the database.
     * For each entity, insert if it is not already in the database
     * update if there is a constraint conflict.
     *
     * @param entities array of entities to upsert
     */
    fun upsert(entities: Array<T>) {
        entities.forEach { entity ->
            try {
                insertionAdapter.insert(entity)
            } catch (ex: SQLiteConstraintException) {
                updateAdapter.handle(entity)
            }
        }
    }

    fun upsert(entities: Iterable<T>) {
        entities.forEach { entity ->
            try {
                insertionAdapter.insert(entity)
            } catch (ex: SQLiteConstraintException) {
                updateAdapter.handle(entity)
            }
        }
    }

    /**
     * Upserts the given entity into the database and returns the row id.
     * If the insertion failed, update the existing entity and return -1.
     *
     * @param entity The entity to upsert
     * @return The SQLite row id or -1 if the insertion failed and update
     * is performed
     */
    fun upsertAndReturnId(entity: T): Long {
        return try {
            insertionAdapter.insertAndReturnId(entity)
        } catch (ex: SQLiteConstraintException) {
            updateAdapter.handle(entity)
            return -1
        }
    }

    /**
     * Upserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to upsert
     * @return The SQLite row ids, for entities that are not inserted the row id returned will be -1
     */
    fun upsertAndReturnIdsArray(entities: Array<T>): LongArray {
        return LongArray(entities.size) { index ->
            try {
                insertionAdapter.insertAndReturnId(entities[index])
            } catch (ex: SQLiteConstraintException) {
                updateAdapter.handle(entities[index])
                -1
            }
        }
    }

    fun upsertAndReturnIdsArray(entities: Collection<T>): LongArray {
        val iterator = entities.iterator()
        return LongArray(entities.size) {
            val entity = iterator.next()
            try {
                insertionAdapter.insertAndReturnId(entity)
            } catch (ex: SQLiteConstraintException) {
                updateAdapter.handle(entity)
                -1
            }
        }
    }

    fun upsertAndReturnIdsList(entities: Array<T>): List<Long> {
        return buildList {
            entities.forEach { entity ->
                try {
                    add(insertionAdapter.insertAndReturnId(entity))
                } catch (ex: SQLiteConstraintException) {
                    updateAdapter.handle(entity)
                    add(-1)
                }
            }
        }
    }

    fun upsertAndReturnIdsList(entities: Collection<T>): List<Long> {
        return buildList {
            entities.forEach { entity ->
                try {
                    add(insertionAdapter.insertAndReturnId(entity))
                } catch (ex: SQLiteConstraintException) {
                    updateAdapter.handle(entity)
                    add(-1)
                }
            }
        }
    }

    fun upsertAndReturnIdsArrayBox(entities: Array<T>): Array<Long> {
        return Array(entities.size) { index ->
            try {
                insertionAdapter.insertAndReturnId(entities[index])
            } catch (ex: SQLiteConstraintException) {
                updateAdapter.handle(entities[index])
                -1
            }
        }
    }

    fun upsertAndReturnIdsArrayBox(entities: Collection<T>): Array<Long> {
        val iterator = entities.iterator()
        return Array(entities.size) {
            val entity = iterator.next()
            try {
                insertionAdapter.insertAndReturnId(entity)
            } catch (ex: SQLiteConstraintException) {
                updateAdapter.handle(entity)
                -1
            }
        }
    }
}
