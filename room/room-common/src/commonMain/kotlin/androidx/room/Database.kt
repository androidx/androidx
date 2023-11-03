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

import kotlin.reflect.KClass

/**
 * Marks a class as a RoomDatabase.
 *
 * The class should be an abstract class and extend [androidx.room.RoomDatabase].
 *
 * You can receive an implementation of the class via
 * [androidx.room.Room.databaseBuilder] or
 * [androidx.room.Room.inMemoryDatabaseBuilder].
 *
 * ```
 * // Song and Album are classes annotated with @Entity.
 * @Database(version = 1, entities = [Song::class, Album::class])
 * abstract class MusicDatabase : RoomDatabase {
 *   // SongDao is a class annotated with @Dao.
 *   abstract fun getSongDao(): SongDao
 *
 *   // AlbumDao is a class annotated with @Dao.
 *   abstract fun getArtistDao(): ArtistDao
 *
 *   // SongAlbumDao is a class annotated with @Dao.
 *   abstract fun getSongAlbumDao(): SongAlbumDao
 * }
 * ```
 *
 * The example above defines a class that has 2 tables and 3 DAO classes that are used to access it.
 * There is no limit on the number of [Entity] or [Dao] classes but they must be unique
 * within the Database.
 *
 * Instead of running queries on the database directly, you are highly recommended to create
 * [Dao] classes. Using Dao classes will allow you to abstract the database communication in
 * a more logical layer which will be much easier to mock in tests (compared to running direct
 * SQL queries). It also automatically does the conversion from `Cursor` to your application
 * data classes so you don't need to deal with lower level database APIs for most of your data
 * access.
 *
 * Room also verifies all of your queries in [Dao] classes while the application is being
 * compiled so that if there is a problem in one of the queries, you will be notified instantly.
 *
 * To automatically generate a migration between two versions of the database, assuming you have
 * the relevant schema files, you are recommended to use [AutoMigration] annotations. Note
 * that if an autoMigration is defined in a database, `exportSchema` must be `true`.
 *
 * @see [Dao]
 * @see [Entity]
 * @see [AutoMigration]
 * @see [androidx.room.RoomDatabase]
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class Database(
    /**
     * The list of entities included in the database. Each entity turns into a table in the
     * database.
     *
     * @return The list of entities in the database.
     */
    val entities: Array<KClass<*>> = [],

    /**
     * The list of database views included in the database. Each class turns into a view in the
     * database.
     *
     * @return The list of database views.
     */
    val views: Array<KClass<*>> = [],

    /**
     * The database version.
     *
     * @return The database version.
     */
    val version: Int,

    /**
     * You can set the annotation processor argument (`room.schemaLocation`) to tell Room to
     * export the database schema into a folder. Even though it is not mandatory, it is a good
     * practice to have version history of your schema in your codebase and you should commit the
     * schema files into your version control system (but don't ship them with your app!).
     *
     * When `room.schemaLocation` is set, Room will check this variable and if it is set to
     * `true`, the database schema will be exported into the given folder.
     *
     * Value of `exportSchema` is `true` by default but you can disable it for databases when
     * you don't want to keep history of versions (like an in-memory only database).
     *
     * @return Whether the schema should be exported to the given folder when the
     * `room.schemaLocation` argument is set. Defaults to `true`.
     */
    val exportSchema: Boolean = true,

    /**
     * List of AutoMigrations that can be performed on this Database.
     *
     * See [AutoMigration] for example code usage.
     *
     * For more complicated cases not covered by [AutoMigration], runtime defined
     * [androidx.room.migration.Migration] added with
     * [androidx.room.RoomDatabase.Builder.addMigrations] can still be used.
     *
     * @return List of [AutoMigration] annotations.
     */
    val autoMigrations: Array<AutoMigration> = []
)
