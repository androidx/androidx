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

/**
 * Marks a class as an entity. This class will have a mapping SQLite table in the database.
 *
 * Each entity must have at least 1 field annotated with [PrimaryKey].
 * You can also use [primaryKeys] attribute to define the primary key.
 *
 * Each entity must either have a no-arg constructor or a constructor whose parameters match
 * fields (based on type and name). Constructor does not have to receive all fields as parameters
 * but if a field is not passed into the constructor, it should either be public or have a public
 * setter. If a matching constructor is available, Room will always use it. If you don't want it
 * to use a constructor, you can annotate it with [Ignore].
 *
 * When a class is marked as an [Entity], all of its fields are persisted. If you would like to
 * exclude some of its fields, you can mark them with [Ignore].
 *
 * If a field is `transient`, it is automatically ignored **unless** it is annotated with
 * `ColumnInfo`, `Embedded` or `Relation`.
 *
 * Example:
 * ```
 * @Entity
 * data class Song (
 *     @PrimaryKey
 *     val id: Long,
 *     val name: String,
 *     @ColumnInfo(name = "release_year")
 *     val releaseYear: Int
 * )
 * ```
 *
 * @see Dao
 * @see Database
 * @see PrimaryKey
 * @see ColumnInfo
 * @see Index
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class Entity(
    /**
     * The table name in the SQLite database. If not set, defaults to the class name.
     *
     * @return The SQLite tableName of the Entity.
     */
    val tableName: String = "",

    /**
     * List of indices on the table.
     *
     * @return The list of indices on the table.
     */
    val indices: Array<Index> = [],

    /**
     * If set to `true`, any Index defined in parent classes of this class will be carried
     * over to the current `Entity`. Note that if you set this to `true`, even if the
     * `Entity` has a parent which sets this value to `false`, the `Entity` will
     * still inherit indices from it and its parents.
     *
     * When the `Entity` inherits an index from the parent, it is **always** renamed with
     * the default naming schema since SQLite **does not** allow using the same index name in
     * multiple tables. See [Index] for the details of the default name.
     *
     * By default, indices defined in parent classes are dropped to avoid unexpected indices.
     * When this happens, you will receive a [RoomWarnings.INDEX_FROM_PARENT_FIELD_IS_DROPPED]
     * or [RoomWarnings.INDEX_FROM_PARENT_IS_DROPPED] warning during compilation.
     *
     * @return True if indices from parent classes should be automatically inherited by this Entity,
     *         false otherwise. Defaults to false.
     */
    val inheritSuperIndices: Boolean = false,

    /**
     * The list of Primary Key column names.
     *
     * If you would like to define an auto generated primary key, you can use [PrimaryKey]
     * annotation on the field with [PrimaryKey.autoGenerate] set to `true`.
     *
     * @return The primary key of this Entity. Can be empty if the class has a field annotated
     * with [PrimaryKey].
     */
    val primaryKeys: Array<String> = [],

    /**
     * List of [ForeignKey] constraints on this entity.
     *
     * @return The list of [ForeignKey] constraints on this entity.
     */
    val foreignKeys: Array<ForeignKey> = [],

    /**
     * The list of column names that should be ignored by Room.
     *
     * Normally, you can use [Ignore], but this is useful for ignoring fields inherited from
     * parents.
     *
     * Columns that are part of an [Embedded] field can not be individually ignored. To ignore
     * columns from an inherited [Embedded] field, use the name of the field.
     *
     * @return The list of field names.
     */
    val ignoredColumns: Array<String> = []
)
