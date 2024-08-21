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
 * Implementations of this class knows how to insert a particular entity.
 *
 * This is an internal library class and all of its implementations are auto-generated.
 *
 * @param T The type parameter of the entity to be inserted
 * @constructor Creates an InsertionAdapter that can insert the entity type T into the given
 *   database.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Deprecated("No longer used by generated code.", ReplaceWith("EntityInsertAdapter"))
abstract class EntityInsertionAdapter<T>(database: RoomDatabase) : SharedSQLiteStatement(database) {
    /**
     * Binds the entity into the given statement.
     *
     * @param statement The SQLite statement that prepared for the query returned from
     *   createInsertQuery.
     * @param entity The entity of type T.
     */
    protected abstract fun bind(statement: SupportSQLiteStatement, entity: T)

    /**
     * Inserts the entity into the database.
     *
     * @param entity The entity to insert
     */
    fun insert(entity: T) {
        val stmt: SupportSQLiteStatement = acquire()
        try {
            bind(stmt, entity)
            stmt.executeInsert()
        } finally {
            release(stmt)
        }
    }

    /**
     * Inserts the given entities into the database.
     *
     * @param entities Entities to insert
     */
    fun insert(entities: Array<out T>) {
        val stmt: SupportSQLiteStatement = acquire()
        try {
            entities.forEach { entity ->
                bind(stmt, entity)
                stmt.executeInsert()
            }
        } finally {
            release(stmt)
        }
    }

    /**
     * Inserts the given entities into the database.
     *
     * @param entities Entities to insert
     */
    fun insert(entities: Iterable<T>) {
        val stmt: SupportSQLiteStatement = acquire()
        try {
            entities.forEach { entity ->
                bind(stmt, entity)
                stmt.executeInsert()
            }
        } finally {
            release(stmt)
        }
    }

    /**
     * Inserts the given entity into the database and returns the row id.
     *
     * @param entity The entity to insert
     * @return The SQLite row id or -1 if no row is inserted
     */
    fun insertAndReturnId(entity: T): Long {
        val stmt: SupportSQLiteStatement = acquire()
        return try {
            bind(stmt, entity)
            stmt.executeInsert()
        } finally {
            release(stmt)
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids, for entities that are not inserted the row id returned will be -1
     */
    fun insertAndReturnIdsArray(entities: Collection<T>): LongArray {
        val stmt: SupportSQLiteStatement = acquire()
        return try {
            val result = LongArray(entities.size)
            entities.forEachIndexed { index, entity ->
                bind(stmt, entity)
                result[index] = stmt.executeInsert()
            }
            result
        } finally {
            release(stmt)
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids, for entities that are not inserted the row id returned will be -1
     */
    fun insertAndReturnIdsArray(entities: Array<out T>): LongArray {
        val stmt: SupportSQLiteStatement = acquire()
        return try {
            val result = LongArray(entities.size)
            entities.forEachIndexed { index, entity ->
                bind(stmt, entity)
                result[index] = stmt.executeInsert()
            }
            result
        } finally {
            release(stmt)
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids, for entities that are not inserted the row id returned will be -1
     */
    fun insertAndReturnIdsArrayBox(entities: Collection<T>): Array<out Long> {
        val stmt: SupportSQLiteStatement = acquire()
        val iterator = entities.iterator()
        return try {
            val result =
                Array(entities.size) {
                    val entity = iterator.next()
                    bind(stmt, entity)
                    stmt.executeInsert()
                }
            result
        } finally {
            release(stmt)
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids, for entities that are not inserted the row id returned will be -1
     */
    fun insertAndReturnIdsArrayBox(entities: Array<out T>): Array<out Long> {
        val stmt: SupportSQLiteStatement = acquire()
        val iterator = entities.iterator()
        return try {
            val result =
                Array(entities.size) {
                    val entity = iterator.next()
                    bind(stmt, entity)
                    stmt.executeInsert()
                }
            result
        } finally {
            release(stmt)
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids, for entities that are not inserted the row id returned will be -1
     */
    fun insertAndReturnIdsList(entities: Array<out T>): List<Long> {
        val stmt: SupportSQLiteStatement = acquire()
        return try {
            buildList {
                entities.forEach { entity ->
                    bind(stmt, entity)
                    add(stmt.executeInsert())
                }
            }
        } finally {
            release(stmt)
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids, for entities that are not inserted the row id returned will be -1
     */
    fun insertAndReturnIdsList(entities: Collection<T>): List<Long> {
        val stmt: SupportSQLiteStatement = acquire()
        return try {
            buildList {
                entities.forEach { entity ->
                    bind(stmt, entity)
                    add(stmt.executeInsert())
                }
            }
        } finally {
            release(stmt)
        }
    }
}
