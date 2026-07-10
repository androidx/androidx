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

/**
 * Marks a function in a [Dao] annotated class as a query function.
 *
 * The [value] of the annotation is the SQL query that will be run when this function is called.
 * This query is **verified at compile time** by Room to ensure that it executes fine against the
 * database schema.
 *
 * **Query Parameters**
 *
 * The arguments of the function will be bound to the parameters of the SQL statement. See
 * [SQLite's binding documentation](https://www.sqlite.org/c3ref/bind_blob.html) for details of
 * binding parameters in SQLite.
 *
 * Room only supports named parameters (e.g.`:name`) to avoid any confusion between the function
 * parameters and the SQL parameters.
 *
 * Room will automatically bind the parameters of the function as arguments into the SQL statement.
 * This is done by matching the name of the parameters to the name of the SQL parameters.
 *
 * ```
 * @Query("SELECT * FROM song WHERE release_year = :year")
 * suspend fun findSongsByReleaseYear(year: Int): List<Song>
 * ```
 *
 * As an extension over SQL parameters, Room supports binding a parameter of collection type to the
 * query. At runtime, Room will transform the SQL query to have matching number of arguments
 * depending on the number of items in the function parameter.
 *
 * ```
 * @Query("SELECT * FROM song WHERE id IN (:songIds)")
 * suspend fun findByIds(songIds: Array<Long>): List<Song>
 * ```
 *
 * For the example above, if the `songIds` is an array of 3 elements, Room will run the query as:
 * `SELECT * FROM song WHERE id IN (?, ?, ?)` and bind each item in the `songIds` array argument
 * into the statement. One caveat of this type of binding is the max amount of items that can be
 * bound are based on the [androidx.sqlite.SQLiteDriver] implementation provided to Room. For
 * example for a driver that uses Android's SQLite binding the limit is 999 items. See also
 * [Section 9 of SQLite Limits](https://www.sqlite.org/limits.html)
 *
 * **Supported Statements**
 *
 * There are 4 type of statements supported in `@Query` functions: `SELECT`, `INSERT`, `UPDATE`, and
 * `DELETE`.
 *
 * For `SELECT` statements, Room will infer the result contents from the function's return type and
 * generate the code that will automatically convert the query result into the function's return
 * type. For single row result queries, the return type can be any data object (also known as data
 * classes) and for single column result queries, the return type can be any of the built-in types,
 * primitives, [String], [ByteArray] and enums. For queries that return multiple values, you can use
 * [List], [Array] or [Map].
 *
 * `INSERT` queries can return `void` / [Unit] or [Long]. If it is a `Long`, the value is the SQLite
 * 'rowid' of the inserted row (i.e. `SELECT last_insert_rowid()`). Note that queries which insert
 * multiple rows cannot return more than one 'rowid', so avoid such statements if returning `Long`.
 *
 * `UPDATE` and `DELETE` queries can return `void` / [Unit] or [Int]. If it is an `Int`, the value
 * is the number of rows affected by this query (i.e. `SELECT changes()`).
 *
 * **Flow Observable Queries**
 *
 * `@Query` functions with `SELECT` statement can also return a [kotlinx.coroutines.flow.Flow]. This
 * creates a `Flow<T>` object that emits the results of the query and re-dispatches the query every
 * time the data in the queried table changes where `T` is one of the supported query types for
 * `SELECT` statements.
 *
 * Note that a non-collection type argument on the return type of `Flow<T>` will always return the
 * first row in the result set, rather than emitting all the rows in sequence. To observe changes
 * over multiple rows in a table, use a collection type argument, i.e. use the return type
 * `Flow<List<T>>` instead.
 *
 * Keep nullability in mind when choosing a return type, as it affects how the query function
 * handles empty tables:
 * * When the return type is `Flow<T>`, querying an empty table throws a null pointer exception.
 * * When the return type is `Flow<T?>`, querying an empty table emits a null value.
 * * When the return type is `Flow<List<T>>`, querying an empty table emits an empty list.
 *
 * Example:
 * ```
 * @Query("SELECT * FROM Song")
 * suspend fun getSongs(): Flow<List<Song>>
 * ```
 *
 * **DAO Function Return Types**
 *
 * You can return arbitrary data classes from your query functions as long as the properties of the
 * data class match the column names in the query result.
 *
 * For example, if you have class:
 * ```
 * data class SongDuration (
 *     val name: String,
 *     @ColumnInfo(name = "duration")
 *     val length: String
 * )
 * ```
 *
 * You can write a query like this:
 * ```
 * @Query("SELECT name, duration FROM song WHERE id = :songId LIMIT 1")
 * suspend fun findSongDuration(songId: Int): SongDuration
 * ```
 *
 * And Room will create the correct implementation to convert the query result into a `SongDuration`
 * object. If there is a mismatch between the query result columns and the properties of the data
 * class, and as long as there is at least 1 column match, Room prints a
 * [RoomWarnings.QUERY_MISMATCH] warning and sets as many properties as it can. Data classes may
 * additional have embedded properties via [Embedded] declarations and / or relations via [Relation]
 * declarations. See the documentation on those annotation for more information.
 *
 * For multiple values, use one of the supported collection return types:
 * ```
 * /** Gets all songs */
 * @Query("SELECT * FROM Song")
 * suspend fun getSongs(): List<Song>
 *
 * /** Gets a map of all albums and their associated songs */
 * @Query("SELECT * FROM Album LEFT JOIN Song ON Album.albumId = Song.albumId")
 * suspend fun getSongsByAlbum(): Map<Album, List<Song>>
 *
 * /** Gets a map of categories and the amount of songs in each category */
 * @Query(
 *     """
 *     SELECT Category.*, COUNT(Song.id) AS songCount
 *     FROM Category LEFT JOIN Song ON Category.cid = Song.cid
 *     GROUP BY Category.cid, Category.name
 *     """
 * )
 * suspend fun getCategoriesCount(): Map<Category, @MapColumn("songCount") Int>
 * ```
 *
 * Additional return types can be supported via [DaoReturnTypeConverter] functions.
 *
 * @see DaoReturnTypeConverter
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
public annotation class Query(
    /**
     * The SQL statement to be run.
     *
     * @return The query to be run.
     */
    val value: String
)
