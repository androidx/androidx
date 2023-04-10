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
 * A convenience annotation which can be used in a POJO to automatically fetch relation entities.
 * When the POJO is returned from a query, all of its relations are also fetched by Room.
 *
 * ```
 * @Entity
 * data class Song (
 *     @PrimaryKey
 *     val songId: Int,
 *     val albumId: Int,
 *     val name: String
 *     // other fields
 * )
 *
 * data class AlbumNameAndAllSongs (
 *     val id: Int,
 *     val name: String,
 *     @Relation(parentColumn = "id", entityColumn = "albumId")
 *     val songs: List<Song>
 * )
 *
 * @Dao
 * public interface MusicDao {
 *     @Query("SELECT id, name FROM Album")
 *     fun loadAlbumAndSongs(): List<AlbumNameAndAllSongs>
 * }
 * ```
 *
 * For a one-to-many or many-to-many relationship, the type of the field annotated with
 * `Relation` must be a [java.util.List] or [java.util.Set].
 *
 * By default, the [Entity] type is inferred from the return type.
 * If you would like to return a different object, you can specify the [entity] property
 * in the annotation.
 *
 * ```
 * data class Album (
 *     val id: Int
 *     // other fields
 * )
 *
 * data class SongNameAndId (
 *     val songId: Int,
 *     val name: String
 * )
 *
 * data class AlbumAllSongs (
 *     @Embedded
 *     val album: Album,
 *     @Relation(parentColumn = "id", entityColumn = "albumId", entity = Song.class)
 *     val songs: List<SongNameAndId>
 * )
 *
 * @Dao
 * public interface MusicDao {
 *     @Query("SELECT * from Album")
 *     val loadAlbumAndSongs(): List<AlbumAllSongs>
 * }
 * ```
 *
 * In the example above, `SongNameAndId` is a regular POJO but all of fields are fetched
 * from the `entity` defined in the `@Relation` annotation _Song_.
 * `SongNameAndId` could also define its own relations all of which would also be fetched
 * automatically.
 *
 * If you would like to specify which columns are fetched from the child [Entity], you can
 * use [projection] property in the `Relation` annotation.
 *
 * ```
 * data class AlbumAndAllSongs (
 *     @Embedded
 *     val album: Album,
 *     @Relation(
 *         parentColumn = "id",
 *         entityColumn = "albumId",
 *         entity = Song.class,
 *         projection = {"name"})
 *     val songNames: List<String>
 * )
 * ```
 *
 * If the relationship is defined by an associative table (also know as junction table) then you can
 * use [associateBy] to specify it. This is useful for fetching many-to-many relations.
 *
 * Note that `@Relation` annotation can be used only in POJO classes, an [Entity] class
 * cannot have relations. This is a design decision to avoid common pitfalls in [Entity]
 * setups. You can read more about it in the main
 * [Room documentation](https://developer.android.com/training/data-storage/room/referencing-data#understand-no-object-references).
 * When loading data, you can simply work around this limitation by creating
 * POJO classes that extend the [Entity].
 *
 * @see [Junction]
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Relation(
    /**
     * The entity or view to fetch the item from. You don't need to set this if the entity or view
     * matches the type argument in the return type.
     *
     * @return The entity or view to fetch from. By default, inherited from the return type.
     */
    val entity: KClass<*> = Any::class,

    /**
     * Reference column in the parent POJO.
     *
     * In a one-to-one or one-to-many relation, this value will be matched against the column
     * defined in [entityColumn]. In a many-to-many using [associateBy] then
     * this value will be matched against the [Junction.parentColumn]
     *
     * @return The column reference in the parent object.
     */
    val parentColumn: String,

    /**
     * The column to match in the [entity].
     *
     * In a one-to-one or one-to-many relation, this value will be matched against the column
     * defined in [parentColumn]. In a many-to-many using [associateBy] then
     * this value will be matched against the [Junction.entityColumn].
     */
    val entityColumn: String,

    /**
     * The entity or view to be used as a associative table (also known as a junction table) when
     * fetching the relating entities.
     *
     * @return The junction describing the associative table. By default, no junction is specified
     * and none will be used.
     *
     * @see Junction
     */
    val associateBy: Junction = Junction(Any::class),

    /**
     * If sub columns should be fetched from the entity, you can specify them using this field.
     *
     * By default, inferred from the the return type.
     *
     * @return The list of columns to be selected from the [entity].
     */
    val projection: Array<String> = []
)
