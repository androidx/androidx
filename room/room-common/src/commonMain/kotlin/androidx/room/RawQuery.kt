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

import kotlin.reflect.KClass

/**
 * Marks a method in a [Dao] annotated class as a raw query method where you can pass the query as a
 * [androidx.room.RoomRawQuery] or [androidx.sqlite.db.SupportSQLiteQuery].
 *
 * ```
 * @Dao
 * interface RawDao {
 *     @RawQuery
 *     fun getSongViaQuery(query: RoomRawQuery): Song
 * }
 *
 * // Usage of RawDao
 * val query = RoomRawQuery(
 *     sql = "SELECT * FROM Song WHERE id = ? LIMIT 1",
 *     onBindStatement = { it.bindLong(1, songId) }
 * )
 * val song = rawDao.getSongViaQuery(query)
 * ```
 *
 * Room will generate the code based on the return type of the function and failure to pass a proper
 * query will result in a runtime failure or an undefined result.
 *
 * If you know the query at compile time, you should always prefer [Query] since it validates the
 * query at compile time and also generates more efficient code, since Room can compute the query
 * result at compile time (e.g. it does not need to account for possibly missing columns in the
 * result).
 *
 * On the other hand, `@RawQuery` serves as an escape hatch where you can build your own SQL query
 * at runtime but still use Room to convert it into objects.
 *
 * `@RawQuery` methods must return a non-void type. If you want to execute a raw query that does not
 * return any value, use [androidx.room.RoomDatabase.query] methods.
 *
 * `@RawQuery` methods can only be used for read queries. For write queries, use
 * [androidx.room.RoomDatabase.openHelper].
 *
 * **Observable Queries:**
 *
 * `@RawQuery` methods can return observable types but you need to specify which tables are accessed
 * in the query using the [observedEntities] field in the annotation.
 *
 * ```
 * @Dao
 * interface RawDao {
 *     @RawQuery(observedEntities = Song::class)
 *     fun getSongs(query: RoomRawQuery): Flow<List<Song>>
 * }
 *
 * // Usage of RawDao
 * val liveSongs = rawDao.getSongs(
 *     RoomRawQuery("SELECT * FROM song ORDER BY name DESC")
 * )
 * ```
 *
 * **Returning POJOs:**
 *
 * `@RawQuery` can also return plain old java objects, similar to [Query] methods.
 *
 * ```
 * data class NameAndReleaseYear (
 *     val name: String,
 *     @ColumnInfo(name = "release_year")
 *     val year: Int
 * )
 *
 * @Dao
 * interface RawDao {
 *     @RawQuery
 *     fun getNameAndReleaseYear(query: RoomRawQuery): NameAndReleaseYear
 * }
 *
 * // Usage of RawDao
 * val result: NameAndReleaseYear = rawDao.getNameAndReleaseYear(
 *     RoomRawQuery(
 *         sql = "SELECT * FROM song WHERE id = ?",
 *         onBindStatement = { it.bindLong(1, songId) }
 *     )
 * )
 * ```
 *
 * **POJOs with Embedded Fields:**
 *
 * `@RawQuery` methods can return POJOs that include [Embedded] fields as well.
 *
 * ```
 * data class SongAndArtist (
 *     @Embedded
 *     val song: Song,
 *     @Embedded
 *     val artist: Artist
 * )
 *
 * @Dao
 * interface RawDao {
 *     @RawQuery
 *     fun getSongAndArtist(query: RoomRawQuery): SongAndArtist
 * }
 *
 * // Usage of RawDao
 * val result: = rawDao.getSongAndArtist(
 *     RoomRawQuery("SELECT * FROM Song, Artist WHERE Song.artistId = Artist.id LIMIT 1")
 * )
 * ```
 *
 * **Relations:**
 *
 * `@RawQuery` return types can also be objects with [Relation].
 *
 * ```
 * data class AlbumAndSongs {
 *     @Embedded
 *     val album: Album,
 *     @Relation(parentColumn = "id", entityColumn = "albumId")
 *     val songs: List<Song>
 * }
 *
 * @Dao
 * interface RawDao {
 *     @RawQuery
 *     fun getAlbumAndSongs(query: RoomRawQuery): List<AlbumAndSongs>
 * }
 *
 * // Usage of RawDao
 * val result = rawDao.getAlbumAndSongs(
 *     RoomRawQuery("SELECT * FROM album")
 * )
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class RawQuery(
    /**
     * Denotes the list of entities which are accessed in the provided query and should be observed
     * for invalidation if the query is observable.
     *
     * The listed classes should either be annotated with [Entity] or they should reference to at
     * least 1 [Entity] (via [Embedded] or [Relation]).
     *
     * Providing this field in a non-observable query has no impact.
     *
     * ```
     * @Dao
     * interface RawDao {
     *   @RawQuery(observedEntities = Song::class)
     *   fun getSongs(query: RoomRawQuery): Flow<List<Song>>
     * }
     * val liveSongs: = rawDao.getSongs(
     *     RoomRawQuery("SELECT * FROM song ORDER BY name DESC")
     * )
     * ```
     *
     * @return List of entities that should invalidate the query if changed.
     */
    val observedEntities: Array<KClass<*>> = []
)
