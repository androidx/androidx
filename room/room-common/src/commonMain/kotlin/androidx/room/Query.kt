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

/**
 * Marks a method in a `Dao` annotated class as a query method.
 *
 * The value of the annotation includes the query that will be run when this method is called. This
 * query is **verified at compile time** by Room to ensure that it compiles fine against the
 * database.
 *
 * The arguments of the method will be bound to the bind arguments in the SQL statement. See
 * [SQLite's binding documentation](https://www.sqlite.org/c3ref/bind_blob.html) for details of bind
 * arguments in SQLite.
 *
 * Room only supports named bind parameter `:name` to avoid any confusion between the method
 * parameters and the query bind parameters.
 *
 * Room will automatically bind the parameters of the method into the bind arguments. This is done
 * by matching the name of the parameters to the name of the bind arguments.
 *
 * ```
 *   @Query("SELECT * FROM song WHERE release_year = :year")
 *   public abstract fun findSongsByReleaseYear(year: Int): List<Song>
 * ```
 *
 * As an extension over SQLite bind arguments, Room supports binding a list of parameters to the
 * query. At runtime, Room will build the correct query to have matching number of bind arguments
 * depending on the number of items in the method parameter.
 *
 * ```
 *   @Query("SELECT * FROM song WHERE id IN(:songIds)")
 *   public abstract fun findByIds(songIds: Array<Long>): List<Song>
 * ```
 *
 * For the example above, if the `songIds` is an array of 3 elements, Room will run the query as:
 * `SELECT * FROM song WHERE id IN(?, ?, ?)` and bind each item in the `songIds` array into the
 * statement. One caveat of this type of binding is that only 999 items can be bound to the query,
 * this is a limitation of SQLite
 * [see Section 9 of SQLite Limits](https://www.sqlite.org/limits.html)
 *
 * There are 4 type of statements supported in `Query` methods: SELECT, INSERT, UPDATE, and DELETE.
 *
 * For SELECT queries, Room will infer the result contents from the method's return type and
 * generate the code that will automatically convert the query result into the method's return type.
 * For single result queries, the return type can be any data object (also known as POJOs). For
 * queries that return multiple values, you can use [java.util.List] or `Array`. In addition to
 * these, any query may return [android.database.Cursor] or any query result can be wrapped in a
 * [androidx.lifecycle.LiveData].
 *
 * INSERT queries can return `void` or `Long`. If it is a `Long`, the value is the SQLite rowid of
 * the row inserted by this query. Note that queries which insert multiple rows cannot return more
 * than one rowid, so avoid such statements if returning `Long`.
 *
 * UPDATE or DELETE queries can return `void` or `Int`. If it is an `Int`, the value is the number
 * of rows affected by this query.
 *
 * **Flow**
 *
 * If you are using Kotlin, you can also return `Flow<T>` from query methods. This creates a
 * `Flow<T>` object that emits the results of the query and re-dispatches the query every time the
 * data in the queried table changes.
 *
 * Note that querying a table with a return type of `Flow<T>` always returns the first row in the
 * result set, rather than emitting all of the rows in sequence. To observe changes over multiple
 * rows in a table, use a return type of `Flow<List<T>>` instead.
 *
 * Keep nullability in mind when choosing a return type, as it affects how the query method handles
 * empty tables:
 * * When the return type is `Flow<T>`, querying an empty table throws a null pointer exception.
 * * When the return type is `Flow<T?>`, querying an empty table emits a null value.
 * * When the return type is `Flow<List<T>>`, querying an empty table emits an empty list.
 *
 * **RxJava2**
 *
 * If you are using RxJava2, you can also return `Flowable<T>` or `Publisher<T>` from query methods.
 * Since Reactive Streams does not allow `null`, if the query returns a nullable type, it will not
 * dispatch anything if the value is `null` (like fetching an [Entity] row that does not exist). You
 * can return [Flowable<T[]>] or [Flowable<List<T>>] to workaround this limitation.
 *
 * Both `Flowable<T>` and `Publisher<T>` will observe the database for changes and re-dispatch if
 * data changes. If you want to query the database without observing changes, you can use `Maybe<T>`
 * or `Single<T>`. If a `Single<T>` query returns `null`, Room will throw
 * [androidx.room.EmptyResultSetException].
 *
 * Additionally if the statement is an INSERT, UPDATE or DELETE then the return types, `Single<T>`,
 * `Maybe<T>` and `Completable` are supported.
 *
 * You can return arbitrary POJOs from your query methods as long as the fields of the POJO match
 * the column names in the query result.
 *
 * For example, if you have class:
 * ```
 * data class SongDuration (
 *   val name: String,
 *   @ColumnInfo(name = "duration")
 *   val length: String
 * )
 * ```
 *
 * You can write a query like this:
 * ```
 *   @Query("SELECT name, duration FROM song WHERE id = :songId LIMIT 1")
 *   public abstract fun findSongDuration(songId: Int): SongDuration
 * ```
 *
 * And Room will create the correct implementation to convert the query result into a `SongDuration`
 * object. If there is a mismatch between the query result and the fields of the POJO, and as long
 * as there is at least 1 field match, Room prints a [RoomWarnings.QUERY_MISMATCH] warning and sets
 * as many fields as it can.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
public annotation class Query(
    /**
     * The SQLite query to be run.
     *
     * @return The query to be run.
     */
    val value: String
)
