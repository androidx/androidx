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
 * Marks a method in a [Dao] annotated class as an insert method.
 *
 * The implementation of the method will insert its parameters into the database.
 *
 * All of the parameters of the Insert method must either be classes annotated with [Entity]
 * or collections/array of it.
 *
 * Example:
 *
 * ```
 * @Dao
 * public interface MusicDao {
 *   @Insert(onConflict = OnConflictStrategy.REPLACE)
 *   public fun insertSongs(varargs songs: Song)
 *
 *   @Insert
 *   public fun insertBoth(song1: Song, song2: Song)
 *
 *   @Insert
 *   public fun insertAlbumWithSongs(album: Album, songs: List<Song>);
 * }
 * ```
 *
 * If the target entity is specified via [entity] then the parameters can be of arbitrary
 * POJO types that will be interpreted as partial entities. For example:
 *
 * ```
 * @Entity
 * data class Playlist (
 *   @PrimaryKey(autoGenerate = true)
 *   val playlistId: Long,
 *   val name: String,
 *   val description: String?,
 *
 *   @ColumnInfo(defaultValue = "normal")
 *   val category: String,
 *
 *   @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
 *   val createdTime: String,
 *
 *   @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
 *   val lastModifiedTime: String
 * )
 *
 * data class NameAndDescription (
 *   val name: String,
 *   val description: String
 * )
 *
 * @Dao
 * public interface PlaylistDao {
 *   @Insert(entity = Playlist::class)
 *   public fun insertNewPlaylist(nameDescription: NameAndDescription);
 * }
 * ```
 *
 * @see [Update]
 * @see [Delete]
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Insert(

    /**
     * The target entity of the insert method.
     *
     * When this is declared, the insert method parameters are interpreted as partial entities when
     * the type of the parameter differs from the target. The POJO class that represents the entity
     * must contain all of the non-null fields without default values of the target entity.
     *
     * If the target entity contains a [PrimaryKey] that is auto generated, then the POJO
     * class doesn't need an equal primary key field, otherwise primary keys must also be present
     * in the POJO.
     *
     * By default the target entity is interpreted by the method parameters.
     *
     * @return the target entity of the insert method or none if the method should use the
     *         parameter type entities.
     */
    val entity: KClass<*> = Any::class,

    /**
     * What to do if a conflict happens.
     *
     * Use [OnConflictStrategy.ABORT] to roll back the transaction on conflict.
     * Use [OnConflictStrategy.REPLACE] to replace the existing rows with the new rows.
     * Use [OnConflictStrategy.IGNORE] to keep the existing rows.
     *
     * @return How to handle conflicts. Defaults to [OnConflictStrategy.NONE].
     */
    @get:OnConflictStrategy
    val onConflict: Int = OnConflictStrategy.NONE
)
