/*
 * Copyright 2018 The Android Open Source Project
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
 * Marks a class as an SQLite view.
 *
 * The value of the annotation is a SELECT query used when the view is created.
 *
 * The class will behave like normal POJO when it is used in a [Dao]. You can SELECT FROM a
 * [DatabaseView] similar to an [Entity], but you can not INSERT, DELETE or UPDATE
 * into a [DatabaseView].
 *
 * Similar to an [Entity], you can use [ColumnInfo] and [Embedded] inside to
 * customize the data class.
 *
 * Example:
 *
 * ```
 * @DatabaseView(
 *   "SELECT id, name, release_year FROM Song " +
 *   "WHERE release_year >= 1990 AND release_year <= 1999"
 * )
 * data class SongFrom90s (
 *   val id: Long,
 *   val name: String,
 *   @ColumnInfo(name = "release_year")
 *   val releaseYear: Int
 * )
 * ```
 *
 * Views have to be registered to a RoomDatabase via [Database.views].
 *
 * @see [Dao]
 * @see [Database]
 * @see [ColumnInfo]
 * @see [Embedded]
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class DatabaseView(

    /**
     * The SELECT query.
     *
     * @return The SELECT query.
     */
    val value: String = "",

    /**
     * The view name in the SQLite database. If not set, it defaults to the class name.
     *
     * @return The SQLite view name.
     */
    val viewName: String = ""
)
