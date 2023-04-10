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
 * Marks a method in a [Dao] annotated class as a delete method.
 *
 * The implementation of the method will delete its parameters from the database.
 *
 * All of the parameters of the Delete method must either be classes annotated with [Entity]
 * or collections/array of it.
 *
 * Example:
 * ```
 * @Dao
 * public interface MusicDao {
 *     @Delete
 *     public fun deleteSongs(vararg songs: Song)
 *
 *     @Delete
 *     public fun deleteAlbumAndSongs(val album: Album, val songs: List<Song>)
 * }
 * ```
 *
 * If the target entity is specified via [entity] then the parameters can be of arbitrary
 * POJO types that will be interpreted as partial entities. For example:
 *
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
 * public interface PlaylistDao {
 *     @Delete(entity = Playlist::class)
 *     fun deleteByOwnerIdAndCategory(varargs idCategory: OwnerIdAndCategory)
 * }
 * ```
 *
 * @see Insert
 * @see Update
 */

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Delete(

    /**
     * The target entity of the delete method.
     *
     * When this is declared, the delete method parameters are interpreted as partial entities when
     * the type of the parameter differs from the target. The POJO class that represents the entity
     * must contain a subset of the fields of the target entity. The fields value will be used to
     * find matching entities to delete.
     *
     * By default the target entity is interpreted by the method parameters.
     *
     * @return the target entity of the delete method or none if the method should use the
     *         parameter type entities.
     */
    val entity: KClass<*> = Any::class
)
