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

/**
 * Declares which column(s) are used to build a map or multimap return value in a {@link Dao}
 * query method.
 *
 * This annotation is required when the key or value of the Map is a single column of one of the
 * built in types (primitives, boxed primitives, enum, String, byte[], ByteBuffer) or a type with a
 * converter (e.g. Date, UUID, etc).
 *
 * The use of this annotation provides clarity on which column should be used in retrieving
 * information required by the return type.
 *
 * Example:
 *
 * ```
 *   @MapInfo(keyColumn = "artistName", valueColumn = "songName")
 *   @Query("SELECT * FROM Artist JOIN Song ON Artist.artistName = Song.artist")
 *   fun getArtistNameToSongNames(): Map<String, List<String>>
 *
 *   @MapInfo(valueColumn = "songCount")
 *   @Query("SELECT *, COUNT(mSongId) as songCount FROM Artist JOIN Song ON
 *   Artist.artistName = Song.artist GROUP BY artistName")
 *   fun getArtistAndSongCounts(): Map<Artist, Integer>
 * ```
 *
 * To use the @MapInfo annotation, you must provide either the key column name, value column
 * name, or both, based on the [Dao]'s method return type. Column(s) specified in the
 * provided @MapInfo annotation must be present in the query result.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class MapInfo(
    /**
     * The name of the column to be used for the map's keys.
     *
     * @return The key column name.
     */
    val keyColumn: String = "",

    /**
     * The name of the column to be used for the map's values.
     *
     * @return The value column name.
     */
    val valueColumn: String = ""
)
