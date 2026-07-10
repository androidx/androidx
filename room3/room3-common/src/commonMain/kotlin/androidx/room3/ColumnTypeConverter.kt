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
 * Marks a function as a column type converter.
 *
 * Converter functions help Room support types beyond the built-in primitives ([String],
 * [ByteArray]) and enums. `@ColumnTypeConverter` functions convert column values when reading from
 * a query result or binding parameters into a statement.
 *
 * Each converter function must receive one parameter and have a non-[Unit] return type. A class can
 * have as many `@ColumnTypeConverter` functions as needed.
 *
 * ```
 * object Converters {
 *     @ColumnTypeConverter
 *     fun fromTimestamp(value: Long?): Date? {
 *         return value?.let { Date(it) }
 *     }
 *
 *     @ColumnTypeConverter
 *     fun dateToTimestamp(date: Date?): Long? {
 *         return date?.getTime()
 *     }
 * }
 * ```
 *
 * @see [ColumnTypeConverters]
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class ColumnTypeConverter
