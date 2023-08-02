/*
 * Copyright (C) 2017 The Android Open Source Project
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
import androidx.room.util.useCursor
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper

/**
 * An open helper that holds a reference to the configuration until the database is opened.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
open class RoomOpenHelper(
    configuration: DatabaseConfiguration,
    delegate: Delegate,
    identityHash: String,
    legacyHash: String
) : SupportSQLiteOpenHelper.Callback(delegate.version) {
    private var configuration: DatabaseConfiguration?
    private val delegate: Delegate
    private val identityHash: String

    /**
     * Room v1 had a bug where the hash was not consistent if fields are reordered.
     * The new has fixes it but we still need to accept the legacy hash.
     */
    // b/64290754
    private val legacyHash: String

    init {
        this.configuration = configuration
        this.delegate = delegate
        this.identityHash = identityHash
        this.legacyHash = legacyHash
    }

    constructor(
        configuration: DatabaseConfiguration,
        delegate: Delegate,
        legacyHash: String
    ) : this(configuration, delegate, "", legacyHash)

    override fun onConfigure(db: SupportSQLiteDatabase) {
        super.onConfigure(db)
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        val isEmptyDatabase = hasEmptySchema(db)
        delegate.createAllTables(db)
        if (!isEmptyDatabase) {
            // A 0 version pre-populated database goes through the create path because the
            // framework's SQLiteOpenHelper thinks the database was just created from scratch. If we
            // find the database not to be empty, then it is a pre-populated, we must validate it to
            // see if its suitable for usage.
            val result = delegate.onValidateSchema(db)
            if (!result.isValid) {
                throw IllegalStateException(
                    "Pre-packaged database has an invalid schema: ${result.expectedFoundMsg}"
                )
            }
        }
        updateIdentity(db)
        delegate.onCreate(db)
    }

    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        var migrated = false
        configuration?.let { config ->
            val migrations = config.migrationContainer.findMigrationPath(
                oldVersion, newVersion
            )
            if (migrations != null) {
                delegate.onPreMigrate(db)
                migrations.forEach { it.migrate(db) }
                val result = delegate.onValidateSchema(db)
                if (!result.isValid) {
                    throw IllegalStateException(
                        ("Migration didn't properly handle: " +
                            result.expectedFoundMsg)
                    )
                }
                delegate.onPostMigrate(db)
                updateIdentity(db)
                migrated = true
            }
        }
        if (!migrated) {
            val config = this.configuration
            if (config != null && !config.isMigrationRequired(oldVersion, newVersion)) {
                delegate.dropAllTables(db)
                delegate.createAllTables(db)
            } else {
                throw IllegalStateException(
                    "A migration from $oldVersion to $newVersion was required but not found. " +
                        "Please provide the " +
                        "necessary Migration path via " +
                        "RoomDatabase.Builder.addMigration(Migration ...) or allow for " +
                        "destructive migrations via one of the " +
                        "RoomDatabase.Builder.fallbackToDestructiveMigration* methods."
                )
            }
        }
    }

    override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        checkIdentity(db)
        delegate.onOpen(db)
        // there might be too many configurations etc, just clear it.
        configuration = null
    }

    private fun checkIdentity(db: SupportSQLiteDatabase) {
        if (hasRoomMasterTable(db)) {
            val identityHash: String? = db.query(
                SimpleSQLiteQuery(RoomMasterTable.READ_QUERY)
            ).useCursor { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            }

            if (this.identityHash != identityHash && this.legacyHash != identityHash) {
                throw IllegalStateException(
                    "Room cannot verify the data integrity. Looks like" +
                        " you've changed schema but forgot to update the version number. You can" +
                        " simply fix this by increasing the version number. Expected identity" +
                        " hash: ${ this.identityHash }, found: $identityHash"
                )
            }
        } else {
            // No room_master_table, this might an a pre-populated DB, we must validate to see if
            // its suitable for usage.
            val result = delegate.onValidateSchema(db)
            if (!result.isValid) {
                throw IllegalStateException(
                    "Pre-packaged database has an invalid schema: ${result.expectedFoundMsg}"
                )
            }
            delegate.onPostMigrate(db)
            updateIdentity(db)
        }
    }

    private fun updateIdentity(db: SupportSQLiteDatabase) {
        createMasterTableIfNotExists(db)
        db.execSQL(RoomMasterTable.createInsertQuery(identityHash))
    }

    private fun createMasterTableIfNotExists(db: SupportSQLiteDatabase) {
        db.execSQL(RoomMasterTable.CREATE_QUERY)
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    abstract class Delegate(@JvmField val version: Int) {
        abstract fun dropAllTables(db: SupportSQLiteDatabase)
        abstract fun createAllTables(db: SupportSQLiteDatabase)
        abstract fun onOpen(db: SupportSQLiteDatabase)
        abstract fun onCreate(db: SupportSQLiteDatabase)

        /**
         * Called after a migration run to validate database integrity.
         *
         * @param db The SQLite database.
         */
        @Deprecated("Use [onValidateSchema(SupportSQLiteDatabase)]")
        protected open fun validateMigration(db: SupportSQLiteDatabase) {
            throw UnsupportedOperationException("validateMigration is deprecated")
        }

        /**
         * Called after a migration run or pre-package database copy to validate database integrity.
         *
         * @param db The SQLite database.
         */
        @Suppress("DEPRECATION")
        open fun onValidateSchema(db: SupportSQLiteDatabase): ValidationResult {
            validateMigration(db)
            return ValidationResult(true, null)
        }

        /**
         * Called before migrations execute to perform preliminary work.
         * @param database The SQLite database.
         */
        open fun onPreMigrate(db: SupportSQLiteDatabase) {}

        /**
         * Called after migrations execute to perform additional work.
         * @param database The SQLite database.
         */
        open fun onPostMigrate(db: SupportSQLiteDatabase) {}
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    open class ValidationResult(
        @JvmField val isValid: Boolean,
        @JvmField val expectedFoundMsg: String?
    )
    companion object {
        internal fun hasRoomMasterTable(db: SupportSQLiteDatabase): Boolean {
            db.query(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND " +
                    "name='${ RoomMasterTable.TABLE_NAME }'"
            ).useCursor { cursor ->
                return cursor.moveToFirst() && cursor.getInt(0) != 0
            }
        }

        internal fun hasEmptySchema(db: SupportSQLiteDatabase): Boolean {
            db.query(
                "SELECT count(*) FROM sqlite_master WHERE name != 'android_metadata'"
            ).useCursor { cursor ->
                return cursor.moveToFirst() && cursor.getInt(0) == 0
            }
        }
    }
}
