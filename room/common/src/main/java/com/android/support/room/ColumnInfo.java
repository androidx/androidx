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

package com.android.support.room;

import android.support.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets the column name of an entity field.
 * <p>
 * By default, Room uses the field name as the column name in the database. You can override this
 * behavior by using this annotation on an Entity field.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface ColumnInfo {
    /**
     * Name of the column in the database. Defaults to the field name if not set.
     *
     * @return Name of the column in the database.
     */
    String name() default INHERIT_FIELD_NAME;

    /**
     * The type affinity for the column, which will be used when constructing the database.
     * <p>
     * If it is not specified, Room resolves it based on the field's type and available
     * TypeConverters.
     *
     * @return The type affinity of the column.
     */
    @SQLiteTypeAffinity int affinity() default UNDEFINED;

    /**
     * Default value for name. If used, Room will use the field name as the column name.
     */
    String INHERIT_FIELD_NAME = "[field-name]";

    /**
     * Undefined type affinity. Will be resolved based on the type.
     */
    int UNDEFINED = 1;

    /**
     * Column affinity constant for strings.
     */
    int TEXT = 2;
    /**
     * Column affinity constant for integers or booleans.
     */
    int INTEGER = 3;
    /**
     * Column affinity constant for floats or doubles.
     */
    int REAL = 4;
    /**
     * Column affinity constant for binary data.
     */
    int BLOB = 5;

    /**
     * The SQLite column type for this field.
     */
    @IntDef({UNDEFINED, TEXT, INTEGER, REAL, BLOB})
    @interface SQLiteTypeAffinity {
    }
}
