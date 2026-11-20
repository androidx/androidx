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

package androidx.room3

import androidx.annotation.RestrictTo
import androidx.room3.util.SQLiteVersion
import androidx.room3.util.getSQLiteVersion
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteException
import androidx.sqlite.async.prepare
import androidx.sqlite.async.step
import kotlin.concurrent.Volatile

/**
 * This class knows how to insert an entity. When the insertion fails due to a unique constraint
 * conflict (i.e. primary key conflict), it will perform an update.
 *
 * @param T the type param of the entity to be upserted
 * @constructor Creates an [EntityUpsertAdapter] that upserts entities using native SQLite `INSERT
 *   ... ON CONFLICT` syntax via [insertOrUpdateQuery] and [insertOrNothingQuery] on SQLite 3.24.0+,
 *   or falls back to [entityInsertAdapter] and [updateAdapter] when queries are `null` or SQLite is
 *   older than 3.24.0.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
public class EntityUpsertAdapter<T>(
    private val entityInsertAdapter: EntityInsertAdapter<T>,
    private val updateAdapter: EntityDeleteOrUpdateAdapter<T>,
    private val insertOrUpdateQuery: String?,
    private val insertOrNothingQuery: String?,
) {
    /**
     * Creates an EntityUpsertAdapter that can upsert an entity of type T into the database using
     * [entityInsertAdapter] to perform insertion and [updateAdapter] to perform an update when the
     * insertion fails.
     */
    public constructor(
        entityInsertAdapter: EntityInsertAdapter<T>,
        updateAdapter: EntityDeleteOrUpdateAdapter<T>,
    ) : this(entityInsertAdapter, updateAdapter, null, null)

    @Volatile private var supportsNativeUpsertCached: Boolean? = null

    /**
     * Inserts the entity into the database. If a constraint exception is thrown i.e. a primary key
     * conflict, update the existing entity.
     *
     * @param entity The entity to insert
     */
    public suspend fun upsert(connection: SQLiteConnection, entity: T?) {
        if (entity == null) return
        if (insertOrUpdateQuery != null && isNativeUpsertSupported(connection)) {
            entityInsertAdapter.insert(connection, insertOrUpdateQuery, entity)
            return
        }
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
    public suspend fun upsert(connection: SQLiteConnection, entities: Array<out T?>?) {
        if (entities == null) return
        if (insertOrUpdateQuery != null && isNativeUpsertSupported(connection)) {
            entityInsertAdapter.insert(connection, insertOrUpdateQuery, entities)
            return
        }
        entities.forEach { entity ->
            try {
                entityInsertAdapter.insert(connection, entity)
            } catch (ex: SQLiteException) {
                checkUniquenessException(ex)
                updateAdapter.handle(connection, entity)
            }
        }
    }

    public suspend fun upsert(connection: SQLiteConnection, entities: Iterable<T?>?) {
        if (entities == null) return
        if (insertOrUpdateQuery != null && isNativeUpsertSupported(connection)) {
            entityInsertAdapter.insert(connection, insertOrUpdateQuery, entities)
            return
        }
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
    public suspend fun upsertAndReturnId(connection: SQLiteConnection, entity: T?): Long {
        if (entity == null) return -1L
        if (insertOrNothingQuery != null && isNativeUpsertSupported(connection)) {
            return upsertAndReturnIdsNative(connection, insertOrNothingQuery, listOf(entity))[0]
        }
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
    public suspend fun upsertAndReturnIdsArray(
        connection: SQLiteConnection,
        entities: Array<out T?>?,
    ): LongArray {
        if (entities.isNullOrEmpty()) return longArrayOf()
        if (insertOrNothingQuery != null && isNativeUpsertSupported(connection)) {
            return upsertAndReturnIdsNative(connection, insertOrNothingQuery, entities.asList())
        }
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

    public suspend fun upsertAndReturnIdsArray(
        connection: SQLiteConnection,
        entities: Collection<T?>?,
    ): LongArray {
        if (entities.isNullOrEmpty()) return longArrayOf()
        if (insertOrNothingQuery != null && isNativeUpsertSupported(connection)) {
            return upsertAndReturnIdsNative(connection, insertOrNothingQuery, entities)
        }
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

    public suspend fun upsertAndReturnIdsList(
        connection: SQLiteConnection,
        entities: Array<out T?>?,
    ): List<Long> {
        if (entities.isNullOrEmpty()) return emptyList()
        if (insertOrNothingQuery != null && isNativeUpsertSupported(connection)) {
            return upsertAndReturnIdsNative(connection, insertOrNothingQuery, entities.asList())
                .toList()
        }
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

    public suspend fun upsertAndReturnIdsList(
        connection: SQLiteConnection,
        entities: Collection<T?>?,
    ): List<Long> {
        if (entities.isNullOrEmpty()) return emptyList()
        if (insertOrNothingQuery != null && isNativeUpsertSupported(connection)) {
            return upsertAndReturnIdsNative(connection, insertOrNothingQuery, entities).toList()
        }
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

    public suspend fun upsertAndReturnIdsArrayBox(
        connection: SQLiteConnection,
        entities: Array<out T?>?,
    ): Array<out Long> {
        if (entities.isNullOrEmpty()) return emptyArray()
        if (insertOrNothingQuery != null && isNativeUpsertSupported(connection)) {
            return upsertAndReturnIdsNative(connection, insertOrNothingQuery, entities.asList())
                .toTypedArray()
        }
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

    public suspend fun upsertAndReturnIdsArrayBox(
        connection: SQLiteConnection,
        entities: Collection<T?>?,
    ): Array<out Long> {
        if (entities.isNullOrEmpty()) return emptyArray()
        if (insertOrNothingQuery != null && isNativeUpsertSupported(connection)) {
            return upsertAndReturnIdsNative(connection, insertOrNothingQuery, entities)
                .toTypedArray()
        }
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
     * Upserts [entities] on SQLite 3.24+ and returns inserted row IDs, or `-1L` on update.
     *
     * Uses [insertOrNothingQuery] (`INSERT ... ON CONFLICT DO NOTHING`) rather than
     * [insertOrUpdateQuery] (`ON CONFLICT DO UPDATE`) because SQLite reports `changes() == 1` for
     * both inserts and updates and leaves `last_insert_rowid()` unchanged on updates, which cannot
     * reliably distinguish them. With `DO NOTHING`, `changes()` is `1` on insert (return
     * `last_insert_rowid()`) and `0` on a primary key conflict (run [updateAdapter] and return
     * `-1L`).
     */
    // TODO: Consider creating overloads to avoid array / list conversions.
    private suspend fun upsertAndReturnIdsNative(
        connection: SQLiteConnection,
        insertOrNothingQuery: String,
        entities: Collection<T?>,
    ): LongArray {
        // For entities with only primary key columns, codegen passes the same `DO NOTHING`
        // query for `insertOrUpdateQuery` and `insertOrNothingQuery`, so no `UPDATE` is needed on
        // conflict.
        val hasUpdateColumns = insertOrUpdateQuery != insertOrNothingQuery
        return connection.prepare(insertOrNothingQuery).use { insertStmt ->
            connection.prepare(SELECT_CHANGES_AND_ROWID_SQL).use { statusStmt ->
                val iterator = entities.iterator()
                LongArray(entities.size) {
                    val entity = iterator.next() ?: return@LongArray -1L
                    entityInsertAdapter.bindInternal(insertStmt, entity)
                    insertStmt.step()
                    insertStmt.reset()
                    statusStmt.step()
                    val changed = statusStmt.getLong(0)
                    val rowId = statusStmt.getLong(1)
                    statusStmt.reset()
                    return@LongArray if (changed != 0L) {
                        // There was a row changed (i.e. inserted)
                        rowId
                    } else {
                        // Conflict on insert since no rows changed, therefore update
                        if (hasUpdateColumns) {
                            updateAdapter.handle(connection, entity)
                        }
                        -1L
                    }
                }
            }
        }
    }

    private suspend fun isNativeUpsertSupported(connection: SQLiteConnection): Boolean =
        supportsNativeUpsertCached
            ?: (getSQLiteVersion(connection) >= MIN_NATIVE_UPSERT_VERSION).also {
                supportsNativeUpsertCached = it
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

    public companion object {
        // `ON CONFLICT DO UPDATE / DO NOTHING` requires SQLite 3.24.0.
        private val MIN_NATIVE_UPSERT_VERSION = SQLiteVersion(3, 24, 0)
        private const val SELECT_CHANGES_AND_ROWID_SQL = "SELECT changes(), last_insert_rowid()"
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
