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

import kotlin.reflect.KClass

/**
 * Marks a function in a [Dao] annotated class as an insert function.
 *
 * The implementation of the function will insert its parameters into the database.
 *
 * All the parameters of the insert function must either be classes annotated with [Entity] or
 * collections / array of it.
 *
 * Example:
 * ```
 * @Dao
 * interface MusicDao {
 *     @Insert(onConflict = OnConflictStrategy.REPLACE)
 *     suspend fun insertSongs(varargs songs: Song)
 *
 *     @Insert
 *     suspend fun insertBoth(song1: Song, song2: Song)
 *
 *     @Insert
 *     suspend fun insertAlbumWithSongs(album: Album, songs: List<Song>)
 * }
 * ```
 *
 * If a target entity is specified via [entity] value then the parameters can be of arbitrary data
 * object types that will be interpreted as partial entities. For example:
 * ```
 * @Entity
 * data class Playlist (
 *     @PrimaryKey(autoGenerate = true)
 *     val playlistId: Long,
 *     val name: String,
 *     val description: String?,
 *     @ColumnInfo(defaultValue = "normal")
 *     val category: String,
 *     @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
 *     val createdTime: String,
 *     @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
 *     val lastModifiedTime: String
 * )
 *
 * data class NameAndDescription (
 *     val name: String,
 *     val description: String
 * )
 *
 * @Dao
 * interface PlaylistDao {
 *     @Insert(entity = Playlist::class)
 *     suspend fun insertNewPlaylist(nameDescription: NameAndDescription)
 * }
 * ```
 *
 * Note that in the example above the partial entity `NameAndDescription` contains the required
 * properties to fulfill the insert, specifically `name` and `description` while other properties
 * can be left out, such as `category`, `createdTime` and `lastModifiedTime` because they have a
 * [ColumnInfo.defaultValue] in the target entity. Similarly `playlistId` can be left out because it
 * has [PrimaryKey.autoGenerate] set to `true`.
 *
 * @see [Delete]
 * @see [Update]
 * @see [Upsert]
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Insert(

    /**
     * The target entity of the insert function.
     *
     * When this is declared, the insert function parameters are interpreted as partial entities
     * when the type of the parameter differs from the target. The data object class that represents
     * the entity must contain all the non-null properties without default values of the target
     * entity.
     *
     * If the target entity contains a [PrimaryKey] that is auto generated, then the data object
     * class doesn't need an equal primary key property, otherwise primary keys must also be present
     * in the data object class.
     *
     * By default, the target entity is interpreted by the function parameters.
     *
     * @return the target entity of the insert function or none if the function should use the
     *   parameter type entities.
     */
    val entity: KClass<*> = Any::class,

    /**
     * What to do if a conflict happens.
     *
     * Use [OnConflictStrategy.ABORT] (default) to roll back the transaction on conflict. Use
     * [OnConflictStrategy.REPLACE] to replace the existing rows with the new rows. Use
     * [OnConflictStrategy.IGNORE] to keep the existing rows.
     *
     * @return How to handle conflicts. Defaults to [OnConflictStrategy.ABORT].
     */
    @get:OnConflictStrategy val onConflict: Int = OnConflictStrategy.ABORT,
)
