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

/**
 * Marks a property of an [Entity] or data object to allow nested properties (i.e. properties of the
 * annotated property's class) to be referenced directly in the SQL queries.
 *
 * If the container is an [Entity], these sub properties will be columns in the [Entity]'s database
 * table.
 *
 * For example, if you have 2 classes:
 * ```
 * data class Coordinates (
 *   val latitude: Double,
 *   val longitude: Double
 * )
 *
 * data class Address (
 *   val street: String,
 *   @Embedded
 *   val coordinates: Coordinates
 * )
 * ```
 *
 * Room will consider `latitude` and `longitude` as if they are properties of the `Address` class
 * when mapping an SQLite row to `Address`.
 *
 * So if you have a query that returns `street, latitude, longitude`, Room will properly construct
 * an `Address` object.
 *
 * If the `Address` class is annotated with [Entity], its database table will have 3 columns:
 * `street`, `latitude` and `longitude`.
 *
 * If there is a name conflict with the properties of the sub data object and the owner object, you
 * can specify a [prefix] for the properties of the sub object. Note that the prefix is always
 * applied to sub properties even if they have a [ColumnInfo] with a specific `name`.
 *
 * If sub properties of an embedded property has [PrimaryKey] annotation, they **will not** be
 * considered as primary keys in the owner [Entity].
 *
 * **Nullable Embedded Properties**
 *
 * When constructing an embedded property that is nullable, if all columns in the row for the
 * properties of the embedded (and its sub properties) are `null` then the embedded property will be
 * set to `null`. Otherwise, it is constructed.
 *
 * Note that even if you have [TypeConverter]s that converts a `null` column into a `non-null`value,
 * if all columns of the embedded property are null, the [TypeConverter] will never be used and the
 * embedded property will not be constructed.
 *
 * This behavior only applies to nullable embedded properties and can be overridden by making the
 * property non-null.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Embedded(
    /**
     * Specifies a prefix to prepend the column names of the properties in the embedded properties.
     *
     * For the example above, if we've written:
     * ```
     * @Embedded(prefix = "loc_")
     * val coordinates: Coordinates
     * ```
     *
     * The column names for `latitude` and `longitude` will be `loc_latitude` and `loc_longitude`
     * respectively.
     *
     * By default, prefix is the empty string.
     *
     * @return The prefix to be used for the properties of the embedded item.
     */
    val prefix: String = ""
)
