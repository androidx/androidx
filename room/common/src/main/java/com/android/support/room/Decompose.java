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

package com.android.support.room;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be used as an annotation on a field of an Entity or Pojo to signal that nested fields
 * (i.e. fields of the annotated field's class) can be referenced directly in the SQL queries.
 * <p>
 * If the container is an {@link Entity}, these sub fields will be columns in the Entity's database
 * table.
 * <p>
 * For example, if you have 2 classes:
 * <pre>
 *   public class Coordinates {
 *       double latitude;
 *       double longitude;
 *   }
 *   public class Address {
 *       String street;
 *       {@literal @}Decompose
 *       Coordinates coordinates;
 *   }
 * </pre>
 * Room will consider {@code latitude} and {@code longitude} as if they are fields of the
 * {@code Address} class when mapping an SQLite row to {@code Address}.
 * <p>
 * So if you have a query that returns {@code street, latitude, longitude}, Room will properly
 * construct an {@code Address} class.
 * <p>
 * Or if the {@code Address} class is annotated with {@link Entity}, it's database table will have 3
 * columns: {@code street, latitude, longitude}
 * <p>
 * If there is a name conflict with the fields of the sub object and the owner object, you can
 * specify a {@link #prefix()} for the items of the sub object. Note that prefix is always applied
 * to sub fields even if they have a {@link ColumnInfo} with a specific {@code name}.
 * <p>
 * If sub fields of a decomposed field has {@link PrimaryKey} annotation, they <b>will not</b> be
 * considered as primary keys in the owner Entity.
 * <p>
 * When a decomposed field is read, if all fields of the decomposed field (and its sub fields) are
 * {@code null} in the cursor, it is set to {@code null}. Otherwise, it is constructed.
 * <p>
 * Note that even if you have {@link TypeConverter}s that convert a {@code null} column into a
 * {@code non-null} value, if all columns of the decomposed field in the {@code Cursor} are null,
 * the {@link TypeConverter} will never be called and the decomposed field will not be constructed.
 * <p>
 * You can override this behavior by annotating the decomposed field with
 * {@link android.support.annotation.NonNull}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Decompose {
    /**
     * Specifies a prefix to prepend the names of the fields in the decomposed fields.
     * <p>
     * For the example, above, if we've written:
     * <pre>
     *   {@literal @}Decomposed(prefix = "foo_")
     *   Coordinates coordinates;
     * </pre>
     * The column names for {@code latitude} and {@code longitude} will be {@code foo_latitude} and
     * {@code foo_longitude} respectively.
     * <p>
     * By default, prefix is the empty string.
     *
     * @return The prefix to be used for the fields of the decomposed item.
     */
    String prefix() default  "";
}
