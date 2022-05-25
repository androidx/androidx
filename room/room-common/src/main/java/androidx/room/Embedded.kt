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

/**
 * Marks a field of an [Entity] or POJO to allow nested fields (i.e. fields of the annotated
 * field's class) to be referenced directly in the SQL queries.
 *
 * If the container is an [Entity], these sub fields will be columns in the [Entity]'s
 * database table.
 *
 * For example, if you have 2 classes:
 *
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
 * Room will consider `latitude` and `longitude` as if they are fields of the
 * `Address` class when mapping an SQLite row to `Address`.
 *
 * So if you have a query that returns `street, latitude, longitude`, Room will properly
 * construct an `Address` class.
 *
 * If the `Address` class is annotated with [Entity], its database table will have 3
 * columns: `street`, `latitude` and `longitude`.
 *
 * If there is a name conflict with the fields of the sub object and the owner object, you can
 * specify a [prefix] for the fields of the sub object. Note that prefix is always applied
 * to sub fields even if they have a [ColumnInfo] with a specific `name`.
 *
 * If sub fields of an embedded field has [PrimaryKey] annotation, they **will not** be
 * considered as primary keys in the owner [Entity].
 *
 * When an embedded field is read, if all fields of the embedded field (and its sub fields) are
 * `null` in the [android.database.Cursor], it is set to `null`. Otherwise,
 * it is constructed.
 *
 * Note that even if you have [TypeConverter]s that convert a `null` column into a
 * `non-null` value, if all columns of the embedded field in the
 * [android.database.Cursor] are null, the [TypeConverter] will never be called
 * and the embedded field will not be constructed.
 *
 * You can override this behavior by annotating the embedded field with
 * [androidx.annotation.NonNull].
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Embedded(
    /**
     * Specifies a prefix to prepend the column names of the fields in the embedded fields.
     *
     * For the example above, if we've written:
     *
     * ```
     * @Embedded(prefix = "loc_")
     * val coordinates: Coordinates
     * ```
     *
     * The column names for `latitude` and `longitude` will be `loc_latitude` and
     * `loc_longitude` respectively.
     *
     * By default, prefix is the empty string.
     *
     * @return The prefix to be used for the fields of the embedded item.
     */
    val prefix: String = ""
)
