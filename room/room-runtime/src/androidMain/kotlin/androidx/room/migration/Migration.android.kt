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
@file:JvmName("MigrationKt")

package androidx.room.migration

import androidx.room.driver.SupportSQLiteConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base class for a database migration.
 *
 * Each migration can move between 2 versions that are defined by [startVersion] and
 * [endVersion].
 *
 * A migration can handle more than 1 version (e.g. if you have a faster path to choose when
 * going version 3 to 5 without going to version 4). If Room opens a database at version
 * 3 and latest version is 5, Room will use the migration object that can migrate from
 * 3 to 5 instead of 3 to 4 and 4 to 5.
 *
 * If there are not enough migrations provided to move from the current version to the latest
 * version, Room will might clear the database and recreate if destructive migrations are enabled.
 *
 * @constructor Creates a new migration between [startVersion] and [endVersion] inclusive.
 */
actual abstract class Migration(
    @JvmField
    actual val startVersion: Int,
    @JvmField
    actual val endVersion: Int
) {
    /**
     * Should run the necessary migrations.
     *
     * The Migration class cannot access any generated Dao in this method.
     *
     * This method is already called inside a transaction and that transaction might actually be a
     * composite transaction of all necessary `Migration`s.
     *
     * @param db The database instance
     */
    abstract fun migrate(db: SupportSQLiteDatabase)

    /**
     * Should run the necessary migrations.
     *
     * This function is already called inside a transaction and that transaction might actually be a
     * composite transaction of all necessary `Migration`s.
     *
     * @param connection The database connection
     */
    actual open fun migrate(connection: SQLiteConnection) {
        // TODO(b/314338741): Signal users this non-abstract overload should be implemented
        if (connection is SupportSQLiteConnection) {
            migrate(connection.db)
        } else {
            TODO("Not yet migrated to use SQLiteDriver")
        }
    }
}

/**
 * Creates [Migration] from [startVersion] to [endVersion] that runs [migrate] to perform
 * the necessary migrations.
 *
 * A migration can handle more than 1 version (e.g. if you have a faster path to choose when
 * going version 3 to 5 without going to version 4). If Room opens a database at version
 * 3 and latest version is < 5, Room will use the migration object that can migrate from
 * 3 to 5 instead of 3 to 4 and 4 to 5.
 *
 * If there are not enough migrations provided to move from the current version to the latest
 * version, Room will clear the database and recreate so even if you have no changes between 2
 * versions, you should still provide a Migration object to the builder.
 *
 * [migrate] cannot access any generated Dao in this method.
 *
 * [migrate] is already called inside a transaction and that transaction
 * might actually be a composite transaction of all necessary `Migration`s.
 */
public fun Migration(
    startVersion: Int,
    endVersion: Int,
    migrate: (SupportSQLiteDatabase) -> Unit
): Migration = MigrationImpl(startVersion, endVersion, migrate)

private class MigrationImpl(
    startVersion: Int,
    endVersion: Int,
    val migrateCallback: (SupportSQLiteDatabase) -> Unit
) : Migration(startVersion, endVersion) {
    override fun migrate(db: SupportSQLiteDatabase) = migrateCallback(db)
}
