/*
 * Copyright 2023 The Android Open Source Project
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

@file:JvmName("MigrationUtil")
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.room.util

import androidx.annotation.RestrictTo
import androidx.room.DatabaseConfiguration
import androidx.room.RoomDatabase.MigrationContainer
import androidx.room.migration.Migration
import kotlin.jvm.JvmName

/**
 * Returns whether a migration is required between two versions.
 *
 * @param fromVersion The old schema version.
 * @param toVersion The new schema version.
 * @return True if a valid migration is required, false otherwise.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // On purpose and only in Android source set.
internal fun DatabaseConfiguration.isMigrationRequired(fromVersion: Int, toVersion: Int): Boolean {
    // Migrations are not required if it is a downgrade AND destructive migration during downgrade
    // has been allowed.
    val isDowngrade = fromVersion > toVersion
    return if (isDowngrade && this.allowDestructiveMigrationOnDowngrade) {
        false
    } else {
        // Migrations are required between the two versions if we generally require migrations
        // AND EITHER there are no exceptions OR the supplied fromVersion is not one of the
        // exceptions.
        this.requireMigration &&
            (this.migrationNotRequiredFrom == null ||
                !this.migrationNotRequiredFrom.contains(fromVersion))
    }
}

/**
 * Indicates if the given migration is contained within the [MigrationContainer] based on its
 * start-end versions.
 *
 * @param startVersion Start version of the migration.
 * @param endVersion End version of the migration
 * @return True if it contains a migration with the same start-end version, false otherwise.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // On purpose in non-common platforms source sets.
internal fun MigrationContainer.contains(startVersion: Int, endVersion: Int): Boolean {
    val migrations = getMigrations()
    if (migrations.containsKey(startVersion)) {
        val startVersionMatches = migrations[startVersion] ?: emptyMap()
        return startVersionMatches.containsKey(endVersion)
    }
    return false
}

/**
 * Finds the list of migrations that should be run to move from `start` version to `end` version.
 *
 * @param start The current database version
 * @param end The target database version
 * @return An ordered list of [Migration] objects that should be run to migrate between the given
 *   versions. If a migration path cannot be found, `null` is returned.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // On purpose and only in Android source set.
internal fun MigrationContainer.findMigrationPath(start: Int, end: Int): List<Migration>? {
    if (start == end) {
        return emptyList()
    }
    val migrateUp = end > start
    val result = mutableListOf<Migration>()
    return findUpMigrationPath(result, migrateUp, start, end)
}

private fun MigrationContainer.findUpMigrationPath(
    result: MutableList<Migration>,
    upgrade: Boolean,
    start: Int,
    end: Int
): List<Migration>? {
    var migrationStart = start
    while (if (upgrade) migrationStart < end else migrationStart > end) {
        // Use ordered keys and start searching from one end of them.
        val (targetNodes, keySet) =
            if (upgrade) {
                getSortedDescendingNodes(migrationStart)
            } else {
                getSortedNodes(migrationStart)
            } ?: return null
        var found = false
        for (targetVersion in keySet) {
            val shouldAddToPath =
                if (upgrade) {
                    targetVersion in (migrationStart + 1)..end
                } else {
                    targetVersion in end until migrationStart
                }
            if (shouldAddToPath) {
                // We are iterating over the key set of targetNodes, so we can assume it
                // won't return a null value.
                result.add(targetNodes[targetVersion]!!)
                migrationStart = targetVersion
                found = true
                break
            }
        }
        if (!found) {
            return null
        }
    }
    return result
}
