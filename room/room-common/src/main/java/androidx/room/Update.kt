/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * Marks a method in a [Dao] annotated class as an update method.
 *
 * The implementation of the method will update its parameters in the database if they already
 * exists (checked by primary keys). If they don't already exists, this option will not change the
 * database.
 *
 * All of the parameters of the Update method must either be classes annotated with [Entity]
 * or collections/array of it.
 *
 * Example:
 *
 * ```
 * @Dao
 * public interface MusicDao {
 *     @Update
 *     fun updateSong(song: Song)
 *
 *     @Update
 *     fun updateSongs(songs: List<Song>): Int
 * }
 * ```
 *
 * If the target entity is specified via [entity] then the parameters can be of arbitrary
 * POJO types that will be interpreted as partial entities. For example:
 *
 * ```
 * @Entity
 * data class Playlist (
 *     @PrimaryKey(autoGenerate = true)
 *     val playlistId: Long,
 *     val name: String,
 *     @ColumnInfo(defaultValue = "")
 *     val description: String,
 *     @ColumnInfo(defaultValue = "normal")
 *     val category: String,
 *     @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
 *     val createdTime: String,
 *     @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
 *     val lastModifiedTime: String
 * )
 *
 * data class PlaylistCategory (
 *   val playlistId: Long,
 *   val category: String,
 *   val lastModifiedTime: String
 * )
 *
 * @Dao
 * public interface PlaylistDao {
 *   @Update(entity = Playlist::class)
 *   fun updateCategory(varargs category: Category)
 * }
 * ```
 *
 * @see [Insert]
 * @see [Delete]
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Update(

    /**
     * The target entity of the update method.
     *
     * When this is declared, the update method parameters are interpreted as partial entities when
     * the type of the parameter differs from the target. The POJO class that represents the entity
     * must contain a subset of the fields of the target entity along with its primary keys.
     *
     * Only the columns represented by the partial entity fields will be updated if an entity with
     * equal primary key is found.
     *
     * By default the target entity is interpreted by the method parameters.
     *
     * @return the target entity of the update method or none if the method should use the
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
