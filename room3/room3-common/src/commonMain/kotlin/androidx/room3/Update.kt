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
package androidx.room3

import kotlin.reflect.KClass

/**
 * Marks a function in a [Dao] annotated class as an update function.
 *
 * The implementation of the function will update its parameters in the database if they already
 * exists (checked by primary keys). If they don't already exist, this option will not change the
 * database.
 *
 * All the parameters of the update function must either be classes annotated with [Entity] or
 * collections / array of it.
 *
 * Example:
 * ```
 * @Dao
 * interface MusicDao {
 *     @Update
 *     suspend fun updateSong(song: Song)
 *
 *     @Update
 *     suspend fun updateSongs(songs: List<Song>): Int
 * }
 * ```
 *
 * If a target entity is specified via [entity] value then the parameters can be of arbitrary data
 * object types that will be interpreted as partial entities. For example:
 * ```
 * @Entity
 * data class Playlist(
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
 * data class PlaylistCategory(
 *     val playlistId: Long,
 *     val category: String,
 *     val lastModifiedTime: String
 * )
 *
 * @Dao
 * interface PlaylistDao {
 *     @Update(entity = Playlist::class)
 *     suspend fun updateCategory(varargs category: Category)
 * }
 * ```
 *
 * @see [Delete]
 * @see [Insert]
 * @see [Upsert]
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Update(

    /**
     * The target entity of the update function.
     *
     * When this is declared, the update function parameters are interpreted as partial entities
     * when the type of the parameter differs from the target. The data object class that represents
     * the entity must contain a subset of the properties of the target entity along with its
     * primary keys.
     *
     * Only the columns represented by the partial entity properties will be updated if an entity
     * with equal primary key is found.
     *
     * By default, the target entity is interpreted by the function parameters.
     *
     * @return the target entity of the update function or none if the function should use the
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
