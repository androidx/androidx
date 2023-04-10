/*
 * Copyright 2019 The Android Open Source Project
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
 * Declares a junction to be used for joining a relationship.
 *
 * If a [Relation] should use an associative table (also know as junction table or join
 * table) then you can use this annotation to reference such table. This is useful for fetching
 * many-to-many relations.
 *
 * ```
 * @Entity(primaryKeys = {"pId", "sId"})
 * public class PlaylistSongXRef {
 *     val pId: Int,
 *     val sId: Int
 * }
 * public class PlaylistWithSongs {
 *     @Embedded
 *     val playlist: Playlist
 *     @Relation(
 *             parentColumn = "playlistId",
 *             entity = Song::class,
 *             entityColumn = "songId",
 *             associateBy = Junction(
 *                     value = PlaylistSongXRef::class,
 *                     parentColumn = "pId",
 *                     entityColumn = "sId")
 *     )
 *     val songs: List<String>
 * }
 *
 * @Dao
 * public interface MusicDao {
 *     @Query("SELECT * FROM Playlist")
 *     val getAllPlaylistsWithSongs(): List<PlaylistWithSongs>
 * }
 * ```
 *
 * In the above example the many-to-many relationship between a `Song` and a `Playlist` has
 * an associative table defined by the entity `PlaylistSongXRef`.
 *
 * @see [Relation]
 */
@Target(allowedTargets = []) // Complex annotation target
@Retention(AnnotationRetention.BINARY)
public annotation class Junction(
    /**
     * An entity or database view to be used as a junction table when fetching the
     * relating entities.
     *
     * @return The entity or database view to be used as a junction table.
     */
    val value: KClass<*>,

    /**
     * The junction column that will be used to match against the [Relation.parentColumn].
     *
     * If not specified it defaults to [Relation.parentColumn].
     */
    val parentColumn: String = "",

    /**
     * The junction column that will be used to match against the [Relation.entityColumn].
     *
     * If not specified it defaults to [Relation.entityColumn].
     */
    val entityColumn: String = ""
)
