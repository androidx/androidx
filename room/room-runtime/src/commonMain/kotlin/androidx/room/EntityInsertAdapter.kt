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
import androidx.room.util.getLastInsertedRowId
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.use

/**
 * Implementations of this class knows how to insert a particular entity.
 *
 * This is a library class and all of its implementations are auto-generated.
 *
 * @constructor Creates an InsertionAdapter that can insert the entity type T into the given
 * database.
 *
 * @param T The type parameter of the entity to be inserted
*/
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
abstract class EntityInsertAdapter<T> {
    /**
     * Create the query.
     *
     * @return The SQL query to prepare.
     */
    protected abstract fun createQuery(): String

    /**
     * Binds the entity into the given statement.
     *
     * @param statement The SQLite statement that prepared for the query returned from
     * createInsertQuery.
     * @param entity    The entity of type T.
     */
    protected abstract fun bind(statement: SQLiteStatement, entity: T)

    /**
     * Inserts the entity into the database.
     *
     * @param entity The entity to insert
     */
    fun insert(connection: SQLiteConnection, entity: T) {
        connection.prepare(createQuery()).use { stmt ->
            bind(stmt, entity)
            stmt.step()
        }
    }

    /**
     * Inserts the given entities into the database.
     *
     * @param entities Entities to insert
     */
    fun insert(
        connection: SQLiteConnection,
        entities: Array<out T>
    ) {
        connection.prepare(createQuery()).use { stmt ->
            entities.forEach { entity ->
                bind(stmt, entity)
                stmt.step()
                stmt.reset()
            }
        }
    }

    /**
     * Inserts the given entities into the database.
     *
     * @param entities Entities to insert
     */
    fun insert(
        connection: SQLiteConnection,
        entities: Iterable<T>
    ) {
        connection.prepare(createQuery()).use { stmt ->
            entities.forEach { entity ->
                bind(stmt, entity)
                stmt.step()
                stmt.reset()
            }
        }
    }

    /**
     * Inserts the given entity into the database and returns the row id.
     *
     * @param entity The entity to insert
     * @return The SQLite row id or -1 if no row is inserted
     */
    fun insertAndReturnId(
        connection: SQLiteConnection,
        entity: T
    ): Long {
        connection.prepare(createQuery()).use { stmt ->
            bind(stmt, entity)
            stmt.step()
        }
        return getLastInsertedRowId(connection)
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids, for entities that are not inserted the row id returned will be -1
     */
    fun insertAndReturnIdsArray(
        connection: SQLiteConnection,
        entities: Collection<T>
    ): LongArray {
        return connection.prepare(createQuery()).use { stmt ->
            LongArray(entities.size) { index ->
                bind(stmt, entities.elementAt(index))
                stmt.step()
                stmt.reset()
                getLastInsertedRowId(connection)
            }
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids, for entities that are not inserted the row id returned will be -1
     */
    fun insertAndReturnIdsArray(
        connection: SQLiteConnection,
        entities: Array<out T>
    ): LongArray {
        return connection.prepare(createQuery()).use { stmt ->
            LongArray(entities.size) { index ->
                bind(stmt, entities.elementAt(index))
                stmt.step()
                stmt.reset()
                getLastInsertedRowId(connection)
            }
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids, for entities that are not inserted the row id returned will be -1
     */
    fun insertAndReturnIdsArrayBox(
        connection: SQLiteConnection,
        entities: Collection<T>
    ): Array<out Long> {
        return connection.prepare(createQuery()).use { stmt ->
            Array(entities.size) { index ->
                bind(stmt, entities.elementAt(index))
                stmt.step()
                stmt.reset()
                getLastInsertedRowId(connection)
            }
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids, for entities that are not inserted the row id returned will be -1
     */
    fun insertAndReturnIdsArrayBox(
        connection: SQLiteConnection,
        entities: Array<out T>
    ): Array<out Long> {
        return connection.prepare(createQuery()).use { stmt ->
            Array(entities.size) { index ->
                bind(stmt, entities.elementAt(index))
                stmt.step()
                stmt.reset()
                getLastInsertedRowId(connection)
            }
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids, for entities that are not inserted the row id returned will be -1
     */
    fun insertAndReturnIdsList(
        connection: SQLiteConnection,
        entities: Array<out T>
    ): List<Long> {
        return buildList {
            connection.prepare(createQuery()).use { stmt ->
                entities.forEach { entity ->
                    bind(stmt, entity)
                    stmt.step()
                    stmt.reset()
                    add(getLastInsertedRowId(connection))
                }
            }
        }
    }

    /**
     * Inserts the given entities into the database and returns the row ids.
     *
     * @param entities Entities to insert
     * @return The SQLite row ids, for entities that are not inserted the row id returned will be -1
     */
    fun insertAndReturnIdsList(
        connection: SQLiteConnection,
        entities: Collection<T>
    ): List<Long> {
        return buildList {
            connection.prepare(createQuery()).use { stmt ->
                entities.forEach { entity ->
                    bind(stmt, entity)
                    stmt.step()
                    stmt.reset()
                    add(getLastInsertedRowId(connection))
                }
            }
        }
    }
}
