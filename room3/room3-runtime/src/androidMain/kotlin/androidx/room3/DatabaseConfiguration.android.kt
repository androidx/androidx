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
package androidx.room3

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo
import androidx.room3.autoclose.AutoCloserConfig
import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.prepackage.PrePackagedCopyConfig
import androidx.room3.util.isMigrationRequired as isMigrationRequiredExt
import androidx.sqlite.SQLiteDriver
import kotlin.coroutines.CoroutineContext

/** Configuration class for a [RoomDatabase]. */
@Suppress("UNUSED_PARAMETER", "KmpModifierMismatch") // expect is not open
public actual open class DatabaseConfiguration
@SuppressLint("LambdaLast")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
constructor(
    /* The context to use while connecting to the database. */
    @JvmField public val context: Context,

    /* The name of the database file or null if it is an in-memory database. */
    @JvmField public actual val name: String?,

    /* Collection of available migrations. */
    @JvmField public actual val migrationContainer: RoomDatabase.MigrationContainer,

    /* Database callbacks. */
    @JvmField public actual val callbacks: List<RoomDatabase.Callback>?,

    /* Whether Room should throw an exception for queries run on the main thread. */
    @JvmField public val allowMainThreadQueries: Boolean,

    /* The journal mode for this database. */
    @JvmField public actual val journalMode: RoomDatabase.JournalMode,

    /**
     * Intent that should be bound to acquire the invalidation service or `null` if not used.
     *
     * @see [multiInstanceInvalidation]
     */
    @field:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) // used in generated code
    @JvmField
    public val multiInstanceInvalidationServiceIntent: Intent?,

    /* Whether Room should throw an exception for missing migrations. */
    @JvmField public actual val requireMigration: Boolean,

    /* Whether Room will fallback to destructive migrations on downgrades only .*/
    @JvmField public actual val allowDestructiveMigrationOnDowngrade: Boolean,
    internal actual val migrationNotRequiredFrom: Set<Int>?,

    /* Callback when Room uses a pre-packaged database. */
    @JvmField public val prepackagedDatabaseCallback: RoomDatabase.PrepackagedDatabaseCallback?,

    /* List of provided type converters. */
    @JvmField public actual val typeConverters: List<Any>,

    /* List of provided auto migration specs. */
    @JvmField public actual val autoMigrationSpecs: List<AutoMigrationSpec>,

    /* Whether Room will delete all tables or only known tables during destructive migrations. */
    @JvmField public actual val allowDestructiveMigrationForAllTables: Boolean,

    /* The SQLite Driver for the database. */
    @JvmField public actual val sqliteDriver: SQLiteDriver,

    /* The Coroutine context for the database. */
    @JvmField public actual val queryCoroutineContext: CoroutineContext,
) {
    /**
     * If true, table invalidation in an instance of [RoomDatabase] is broadcast and synchronized
     * with other instances of the same [RoomDatabase] file, including those in a separate process.
     */
    @JvmField
    public val multiInstanceInvalidation: Boolean = multiInstanceInvalidationServiceIntent != null

    /* Whether the invalidation tracker will use temp or real tables for invalidation tracking. */
    internal var useTempTrackingTable = true

    /* Config for pre-package database or null if not used. */
    internal var copyFromConfig: PrePackagedCopyConfig? = null

    /* Config for auto-close or null if not used. */
    internal var autoCloseConfig: AutoCloserConfig? = null

    /* Size of the prepared statement cache. 25 to match Android Framework cache size. 0 if cache is to be unused. */
    internal var preparedStatementCacheSize = 25

    /**
     * Returns whether a migration is required between two versions.
     *
     * @param fromVersion The old schema version.
     * @param toVersion The new schema version.
     * @return True if a valid migration is required, false otherwise.
     */
    public open fun isMigrationRequired(fromVersion: Int, toVersion: Int): Boolean {
        return isMigrationRequiredExt(fromVersion, toVersion)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun copy(
        context: Context = this.context,
        name: String? = this.name,
        migrationContainer: RoomDatabase.MigrationContainer = this.migrationContainer,
        callbacks: List<RoomDatabase.Callback>? = this.callbacks,
        allowMainThreadQueries: Boolean = this.allowMainThreadQueries,
        journalMode: RoomDatabase.JournalMode = this.journalMode,
        multiInstanceInvalidationServiceIntent: Intent? =
            this.multiInstanceInvalidationServiceIntent,
        requireMigration: Boolean = this.requireMigration,
        allowDestructiveMigrationOnDowngrade: Boolean = this.allowDestructiveMigrationOnDowngrade,
        migrationNotRequiredFrom: Set<Int>? = this.migrationNotRequiredFrom,
        prepackagedDatabaseCallback: RoomDatabase.PrepackagedDatabaseCallback? =
            this.prepackagedDatabaseCallback,
        typeConverters: List<Any> = this.typeConverters,
        autoMigrationSpecs: List<AutoMigrationSpec> = this.autoMigrationSpecs,
        allowDestructiveMigrationForAllTables: Boolean = this.allowDestructiveMigrationForAllTables,
        sqliteDriver: SQLiteDriver = this.sqliteDriver,
        queryCoroutineContext: CoroutineContext = this.queryCoroutineContext,
    ): DatabaseConfiguration =
        DatabaseConfiguration(
                context,
                name,
                migrationContainer,
                callbacks,
                allowMainThreadQueries,
                journalMode,
                multiInstanceInvalidationServiceIntent,
                requireMigration,
                allowDestructiveMigrationOnDowngrade,
                migrationNotRequiredFrom,
                prepackagedDatabaseCallback,
                typeConverters,
                autoMigrationSpecs,
                allowDestructiveMigrationForAllTables,
                sqliteDriver,
                queryCoroutineContext,
            )
            .also {
                it.useTempTrackingTable = this.useTempTrackingTable
                it.copyFromConfig = this.copyFromConfig
                it.autoCloseConfig = this.autoCloseConfig
                it.preparedStatementCacheSize = this.preparedStatementCacheSize
            }
}
