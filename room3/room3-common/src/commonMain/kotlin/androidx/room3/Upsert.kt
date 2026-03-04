/*
 * Copyright (C) 2022 The Android Open Source Project
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
 * Marks a function in a [Dao] annotated class as an upsert (insert or update) function.
 *
 * The implementation of the function will insert its parameters into the database if it does not
 * already exist (checked by primary key). If it already exists, it will update its parameters in
 * the database.
 *
 * All the parameters of the upsert function must either be classes annotated with [Entity] or
 * collections/array of it.
 *
 * Example:
 * ```
 * @Dao
 * interface MusicDao {
 *     @Upsert
 *     suspend fun upsertSongs(varargs songs: Song)
 *
 *     @Upsert
 *     suspend fun upsertBoth(song1: Song, song2: Song)
 *
 *     @Upsert
 *     suspend fun upsertAlbumWithSongs(album: Album, songs: List<Song>)
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
 *     @Upsert(entity = Playlist::class)
 *     suspend fun upsertNewPlaylist(nameDescription: NameAndDescription)
 * }
 * ```
 *
 * @see [Delete]
 * @see [Insert]
 * @see [Update]
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Upsert(

    /**
     * The target entity of the upsert function.
     *
     * When this is declared, the upsert function parameters are interpreted as partial entities
     * when the type of the parameter differs from the target. The data object class that represents
     * the entity must contain all the non-null properties without default values of the target
     * entity.
     *
     * If the target entity contains a [PrimaryKey] that is auto generated, then the data object
     * class doesn't need an equal primary key field, otherwise primary keys must also be present in
     * the data object. If the primary key already exists, only the columns represented by the
     * partial entity properties will be updated
     *
     * By default, the target entity is interpreted by the function parameters.
     *
     * @return the target entity of the upsert function or none if the function should use the
     *   parameter type entities.
     */
    val entity: KClass<*> = Any::class
)
