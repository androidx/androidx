/*
 * Copyright 2021 The Android Open Source Project
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

import kotlin.reflect.KClass

/**
 * Declares an automatic migration on a Database.
 *
 * An automatic migration is a [androidx.room.migration.Migration] that is generated
 * via the use of database schema files at two versions of a [androidx.room.RoomDatabase].
 * Room automatically detects changes on the database between these two schemas,
 * and constructs a [androidx.room.migration.Migration] to migrate between the
 * two versions. In case of ambiguous scenarios (e.g. column/table rename/deletes), additional
 * information is required, and can be provided via the
 * [androidx.room.migration.AutoMigrationSpec] property.
 *
 * An auto migration must define the 'from' and 'to' versions of the schema for which a migration
 * implementation will be generated. A class that implements AutoMigrationSpec can be declared in
 * the [androidx.room.migration.AutoMigrationSpec] property to either
 * provide more information for ambiguous scenarios or execute callbacks during the migration.
 *
 * If there are any column/table renames/deletes between the two versions of the database
 * provided then it is said that there are ambiguous scenarios in the migration. In
 * such scenarios then an [androidx.room.migration.AutoMigrationSpec] is
 * required and the class provided must be annotated with the relevant change annotation(s):
 * [RenameColumn], [RenameTable], [DeleteColumn] or [DeleteTable]. When
 * no ambiguous scenario is present, then the [androidx.room.migration.AutoMigrationSpec]
 * property is optional.
 *
 * If an auto migration is defined for a database, then [androidx.room.Database.exportSchema]
 * must be set to true.
 *
 * Example:
 *
 * ```
 * @Database(
 *    version = MusicDatabase.LATEST_VERSION,
 *    entities = [
 *        Song.class,
 *        Artist.class
 *    ],
 *    autoMigrations = [
 *        AutoMigration (
 *            from = 1,
 *            to = 2
 *        ),
 *        AutoMigration (
 *            from = 2,
 *            to = 3,
 *            spec = MusicDatabase.MyExampleAutoMigration::class
 *        )
 *    ],
 *    exportSchema = true
 * )
 * abstract class MusicDatabase  : RoomDatabase() {
 *    const val LATEST_VERSION = 3
 *
 *    @DeleteTable(deletedTableName = "Album")
 *    @RenameTable(fromTableName = "Singer", toTableName = "Artist")
 *    @RenameColumn(
 *        tableName = "Song",
 *        fromColumnName = "songName",
 *        toColumnName = "songTitle"
 *     )
 *    @DeleteColumn(fromTableName = "Song", deletedColumnName = "genre")
 *    class MyExampleAutoMigration : AutoMigrationSpec {
 *        @Override
 *        override fun onPostMigrate(db: SupportSQLiteDatabase) {
 *            // Invoked once auto migration is done
 *        }
 *     }
 * }
 * ```
 *
 * @see [androidx.room.RoomDatabase]
 * @see [androidx.room.migration.AutoMigrationSpec]
 */

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)

public annotation class AutoMigration(
    /**
     * Version of the database schema to migrate from.
     *
     * @return Version number of the database to migrate from.
     */
    val from: Int,

    /**
     * Version of the database schema to migrate to.
     *
     * @return Version number of the database to migrate to.
     */
    val to: Int,

    /**
     * User implemented custom auto migration spec.
     *
     * @return The auto migration specification or none if the user has not implemented a spec
     */
    val spec: KClass<*> = Any::class
)
