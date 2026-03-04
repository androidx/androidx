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

package androidx.room3

/**
 * Declares which column is used to build a map or multimap return value in a [Dao] query function.
 *
 * This annotation is required when the key or value of a `Map` (or nested map) is a single column
 * of one of the built-in types (primitives, enums, `String`, `ByteArray`) or a type with a
 * [TypeConverter] (e.g. `Date`, UUID, etc).
 *
 * The use of this annotation provides clarity on which column should be used in retrieving
 * information required by the DAO function return type.
 *
 * Example:
 * ```
 * @Query("SELECT * FROM Artist JOIN Song ON Artist.artistName = Song.artist")
 * suspend fun getArtistNameToSongNames():
 *     Map<@MapColumn("artistName") String, @MapColumn("songName") List<String>>
 *
 * @Query(
 *     """
 *     SELECT *, COUNT(mSongId) as songCount
 *     FROM Artist JOIN Song ON  Artist.artistName = Song.artist GROUP BY artistName
 *     """
 * )
 * suspend fun getArtistAndSongCounts():
 *     Map<Artist, @MapColumn(columnName = "songCount") Integer>
 * ```
 *
 * Column(s) specified in this `@MapColumn`'s annotation values must be present in the query result.
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
public annotation class MapColumn(
    /**
     * The name of the column to be used for the map's key or value.
     *
     * @return The column name.
     */
    val columnName: String,

    /**
     * The name of the table or alias to be used for the map's column.
     *
     * Providing this value is optional. Useful for disambiguating between duplicate column names.
     * For example, consider the following query: `SELECT * FROM Artist AS a JOIN Song AS s ON a.id
     * == s.artistId`, then the `@MapColumn` usage for a return type `Map<String, List<Song>>` would
     * be `Map<@MapColumn(columnName = "id", tableName = "a") String, List<Song>>`.
     *
     * @return The column table name.
     */
    val tableName: String = "",
)
