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
 * Marks a function in a [Dao] annotated class as a delete function.
 *
 * The implementation of the function will delete its parameters from the database.
 *
 * All the parameters of the delete function must either be classes annotated with [Entity] or
 * collections / array of it.
 *
 * Example:
 * ```
 * @Dao
 * interface MusicDao {
 *     @Delete
 *     suspend fun deleteSongs(vararg songs: Song)
 *
 *     @Delete
 *     suspend fun deleteAlbumAndSongs(album: Album, songs: List<Song>)
 * }
 * ```
 *
 * If a target entity is specified via [entity] value then the parameters can be of arbitrary data
 * object types that will be interpreted as partial entities. For example:
 * ```
 * @Entity
 * data class Playlist (
 *     @PrimaryKey
 *     val playlistId: Long,
 *     val ownerId: Long,
 *     val name: String,
 *     @ColumnInfo(defaultValue = "normal")
 *     val category: String
 * )
 *
 * data class OwnerIdAndCategory (
 *     val ownerId: Long,
 *     val category: String
 * )
 *
 * @Dao
 * interface PlaylistDao {
 *     @Delete(entity = Playlist::class)
 *     suspend fun deleteByOwnerIdAndCategory(varargs idCategory: OwnerIdAndCategory)
 * }
 * ```
 *
 * @see Insert
 * @see Update
 * @see Upsert
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Delete(

    /**
     * The target entity of the delete function.
     *
     * When this is declared, the delete function parameters are interpreted as partial entities
     * when the type of the parameter differs from the target. The data object class that represents
     * the entity must contain a subset of the properties of the target entity. The properties value
     * will be used to find matching entities to delete.
     *
     * By default, the target entity is interpreted by the function parameters.
     *
     * @return the target entity of the delete function or none if the function should use the
     *   parameter type entities.
     */
    val entity: KClass<*> = Any::class
)
