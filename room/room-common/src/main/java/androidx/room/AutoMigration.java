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

package androidx.room;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Declares an automatic migration on a Database.
 * <p>
 * An automatic migration is a {@link androidx.room.migration.Migration Migration} that is generated
 * via the use of database schema files at two versions of a {@link androidx.room.RoomDatabase
 * RoomDatabase}. Room automatically detects changes on the database between these two schemas,
 * and constructs a {@link androidx.room.migration.Migration Migration} to migrate between the
 * two versions. In case of ambiguous scenarios (e.g. column/table rename/deletes), additional
 * information is required, and can be provided via the
 * {@link androidx.room.migration.AutoMigrationSpec AutoMigrationSpec} property.
 * <p>
 * An auto migration must define the 'from' and 'to' versions of the schema for which a migration
 * implementation will be generated. A class that implements AutoMigrationSpec can be declared in
 * the {@link androidx.room.migration.AutoMigrationSpec AutoMigrationSpec} property to either
 * provide more information for ambiguous scenarios or execute callbacks during the migration.
 * <p>
 * If there are any column/table renames/deletes between the two versions of the database
 * provided then it is said that there are ambiguous scenarios in the migration. In
 * such scenarios then an {@link androidx.room.migration.AutoMigrationSpec AutoMigrationSpec} is
 * required and the class provided must be annotated with the relevant change annotation(s):
 * {@link RenameColumn}, {@link RenameTable}, {@link DeleteColumn} or {@link DeleteTable}. When
 * no ambiguous scenario is present, then the {@link androidx.room.migration.AutoMigrationSpec
 * AutoMigrationSpec} property is optional.
 * <p>
 * If an auto migration is defined for a database, then {@link androidx.room.Database#exportSchema}
 * must be set to true.
 * <p>
 * Example:
 * <pre>
 * {@literal @}Database(
 *      version = MusicDatabase.LATEST_VERSION,
 *      entities = {
 *          Song.class,
 *          Artist.class
 *      },
 *      autoMigrations = {
 *          {@literal @}AutoMigration (
 *              from = 1,
 *              to = 2
 *          ),
 *         {@literal @}AutoMigration (
 *              from = 2,
 *              to = 3,
 *              spec = MusicDatabase.MyExampleAutoMigration.class
 *          )
 *      },
 *      exportSchema = true
 * )
 * public abstract class MusicDatabase extends RoomDatabase {
 *     static final int LATEST_VERSION = 3;
 *
 *    {@literal @}DeleteTable(deletedTableName = "Album")
 *    {@literal @}RenameTable(fromTableName = "Singer", toTableName = "Artist")
 *    {@literal @}RenameColumn(
 *          tableName = "Song",
 *          fromColumnName = "songName",
 *          toColumnName = "songTitle"
 *     )
 *    {@literal @}DeleteColumn(fromTableName = "Song", deletedColumnName = "genre")
 *     static class MyExampleAutoMigration implements AutoMigrationSpec {
 *         {@literal @}Override
 *          default void onPostMigrate({@literal @}NonNull SupportSQLiteDatabase db) {
 *              // Invoked once auto migration is done
 *          }
 *     }
 * }
 * </pre>
 *
 * @see androidx.room.RoomDatabase RoomDatabase
 * @see androidx.room.migration.AutoMigrationSpec AutoMigrationSpec
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface AutoMigration {
    /**
     * Version of the database schema to migrate from.
     *
     * @return Version number of the database to migrate from.
     */
    int from();

    /**
     * Version of the database schema to migrate to.
     *
     * @return Version number of the database to migrate to.
     */
    int to();

    /**
     * User implemented custom auto migration spec.
     *
     * @return The auto migration specification or none if the user has not implemented a spec
     */
    Class<?> spec() default Object.class;
}
