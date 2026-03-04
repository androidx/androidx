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
 * Marks a function as a type converter. A class can have as many `@TypeConverter` functions as it
 * needs.
 *
 * Converter functions are useful for helping Room support types beyond the built-in ones,
 * primitives [String], [ByteArray] and enums. `@TypeConverter` functions are for converting a
 * column values may it be when reading from a query result or binding parameters into a statement.
 *
 * Each converter function should receive 1 parameter and have non-void / non-[Unit] return type.
 *
 * ```
 * // Example converter for Date
 * object Converters {
 *     @TypeConverter
 *     fun fromTimestamp(value: Long?): Date? {
 *         return value == null ? null : Date(value)
 *     }
 *
 *     @TypeConverter
 *     fun dateToTimestamp(date: Date?): Long? {
 *         if (date == null) {
 *             return null
 *         } else {
 *             return date.getTime()
 *         }
 *     }
 * }
 * ```
 *
 * @see [TypeConverters]
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class TypeConverter
