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

import androidx.annotation.IntDef
import androidx.annotation.RequiresApi

/**
 * Allows specific customization about the column associated with this property.
 *
 * For example, you can specify a column name for the property or change the column's type affinity.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class ColumnInfo(
    /**
     * Name of the column in the database. Defaults to the property name if not set.
     *
     * @return Name of the column in the database.
     */
    val name: String = INHERIT_FIELD_NAME,

    /**
     * The type affinity for the column, which will be used when constructing the database.
     *
     * If it is not specified, the value defaults to [UNDEFINED] and Room resolves it based on the
     * property's type and available TypeConverters.
     *
     * See [SQLite types documentation](https://www.sqlite.org/datatype3.html) for details.
     *
     * @return The type affinity of the column. This is either [UNDEFINED], [TEXT], [INTEGER],
     *   [REAL], or [BLOB].
     */
    @Suppress("unused") @get:SQLiteTypeAffinity val typeAffinity: Int = UNDEFINED,

    /**
     * Convenience method to index the property.
     *
     * If you would like to create a composite index instead, see: [Index].
     *
     * @return True if this property should be indexed, false otherwise. Defaults to false.
     */
    val index: Boolean = false,

    /**
     * The collation sequence for the column, which will be used when constructing the database.
     *
     * The default value is [UNSPECIFIED]. In that case, Room does not add any collation sequence to
     * the column, and SQLite treats it like [BINARY].
     *
     * @return The collation sequence of the column. This is either [UNSPECIFIED], [BINARY],
     *   [NOCASE], [RTRIM], [LOCALIZED] or [UNICODE].
     */
    @get:Collate val collate: Int = UNSPECIFIED,

    /**
     * The default value for this column.
     *
     * ```
     * @ColumnInfo(defaultValue = "No name")
     * public name: String
     *
     * @ColumnInfo(defaultValue = "0")
     * public flag: Int
     * ```
     *
     * Note that the default value you specify here will _NOT_ be used if you simply insert the
     * [Entity] with [Insert]. In that case, any value assigned in Java/Kotlin will be used. Use
     * [Query] with an `INSERT` statement and skip this column there in order to use this default
     * value.
     *
     * NULL, CURRENT_TIMESTAMP and other SQLite constant values are interpreted as such. If you want
     * to use them as strings for some reason, surround them with single-quotes.
     *
     * ```
     * @ColumnInfo(defaultValue = "NULL")
     * public description: String?
     *
     * @ColumnInfo(defaultValue = "'NULL'")
     * public name: String
     * ```
     *
     * You can also use constant expressions by surrounding them with parentheses.
     *
     * ```
     * @ColumnInfo(defaultValue = "('Created at' || CURRENT_TIMESTAMP)")
     * public notice: String
     * ```
     *
     * @return The default value for this column.
     * @see [VALUE_UNSPECIFIED]
     */
    val defaultValue: String = VALUE_UNSPECIFIED,
) {
    /** The SQLite column type constants that can be used in [typeAffinity()] */
    @IntDef(UNDEFINED, TEXT, INTEGER, REAL, BLOB)
    @Retention(AnnotationRetention.BINARY)
    public annotation class SQLiteTypeAffinity

    @RequiresApi(21)
    @IntDef(UNSPECIFIED, BINARY, NOCASE, RTRIM, LOCALIZED, UNICODE)
    @Retention(AnnotationRetention.BINARY)
    public annotation class Collate

    public companion object {
        /**
         * Constant to let Room inherit the property name as the column name. If used, Room will use
         * the property name as the column name.
         */
        public const val INHERIT_FIELD_NAME: String = "[field-name]"

        /**
         * Undefined type affinity. Will be resolved based on the type.
         *
         * @see typeAffinity()
         */
        public const val UNDEFINED: Int = 1

        /**
         * Column affinity constant for strings.
         *
         * @see typeAffinity()
         */
        public const val TEXT: Int = 2

        /**
         * Column affinity constant for integers or booleans.
         *
         * @see typeAffinity()
         */
        public const val INTEGER: Int = 3

        /**
         * Column affinity constant for floats or doubles.
         *
         * @see typeAffinity()
         */
        public const val REAL: Int = 4

        /**
         * Column affinity constant for binary data.
         *
         * @see typeAffinity()
         */
        public const val BLOB: Int = 5

        /**
         * Collation sequence is not specified. The match will behave like [BINARY].
         *
         * @see collate()
         */
        public const val UNSPECIFIED: Int = 1

        /**
         * Collation sequence for case-sensitive match.
         *
         * @see collate()
         */
        public const val BINARY: Int = 2

        /**
         * Collation sequence for case-insensitive match.
         *
         * @see collate()
         */
        public const val NOCASE: Int = 3

        /**
         * Collation sequence for case-sensitive match except that trailing space characters are
         * ignored.
         *
         * @see collate()
         */
        public const val RTRIM: Int = 4

        /**
         * Collation sequence that uses system's current locale.
         *
         * @see collate()
         */
        @RequiresApi(21) public const val LOCALIZED: Int = 5

        /**
         * Collation sequence that uses Unicode Collation Algorithm.
         *
         * @see collate()
         */
        @RequiresApi(21) public const val UNICODE: Int = 6

        /** A constant for [defaultValue()] that makes the column to have no default value. */
        public const val VALUE_UNSPECIFIED: String = "[value-unspecified]"
    }
}
