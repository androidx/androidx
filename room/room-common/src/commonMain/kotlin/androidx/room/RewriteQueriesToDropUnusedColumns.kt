/*
 * Copyright 2020 The Android Open Source Project
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
 * When present, [RewriteQueriesToDropUnusedColumns] annotation will cause Room to rewrite your
 * [Query] methods such that only the columns that are used in the response are queried from the
 * database.
 *
 * This annotation is useful if you don't need all columns returned in a query but also don't want
 * to spell out their names in the query projection.
 *
 * For example, if you have a `User` class with 10 fields and want to return only the `name` and
 * `lastName` fields in a POJO, you could write the query like this:
 * ```
 * @Dao
 * interface MyDao {
 *     @Query("SELECT * FROM User")
 *     public fun getAll(): List<NameAndLastName>
 * }
 *
 * data class NameAndLastName (
 *     val name: String,
 *     val lastName: String
 * )
 * ```
 *
 * Normally, Room would print a [RoomWarnings.QUERY_MISMATCH] warning since the query result has
 * additional columns that are not used in the response. You can annotate the method with
 * [RewriteQueriesToDropUnusedColumns] to inform Room to rewrite your query at compile time to avoid
 * fetching extra columns.
 *
 * ```
 * @Dao
 * interface MyDao {
 *     @RewriteQueriesToDropUnusedColumns
 *     @Query("SELECT * FROM User")
 *     fun getAll(): List<NameAndLastName>
 * }
 * ```
 *
 * At compile time, Room will convert this query to `SELECT name, lastName FROM (SELECT * FROM
 * User)` which gets flattened by **Sqlite** to `SELECT name, lastName FROM User`.
 *
 * When the annotation is used on a [Dao] method annotated with [Query], it will only affect that
 * query. You can put the annotation on the [Dao] annotated class/interface or the [Database]
 * annotated class where it will impact all methods in the dao / database respectively.
 *
 * Note that Room will not rewrite the query if it has multiple columns that have the same name as
 * it does not yet have a way to distinguish which one is necessary.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class RewriteQueriesToDropUnusedColumns
