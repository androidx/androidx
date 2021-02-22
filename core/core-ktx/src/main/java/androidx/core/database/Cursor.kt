/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE") // Aliases to other public API.

package androidx.core.database

import android.database.Cursor

/**
 * Returns the value of the requested column as a nullable byte array.
 *
 * The result and whether this method throws an exception when the column type is not a blob type is
 * implementation-defined.
 *
 * @see Cursor.isNull
 * @see Cursor.getBlob
 */
public inline fun Cursor.getBlobOrNull(
    index: Int
): ByteArray? = if (isNull(index)) null else getBlob(index)

/**
 * Returns the value of the requested column as a nullable double.
 *
 * The result and whether this method throws an exception when the column type is not a
 * floating-point type is implementation-defined.
 *
 * @see Cursor.isNull
 * @see Cursor.getDouble
 */
public inline fun Cursor.getDoubleOrNull(
    index: Int
): Double? = if (isNull(index)) null else getDouble(index)

/**
 * Returns the value of the requested column as a nullable float.
 *
 * The result and whether this method throws an exception when the column type is not a
 * floating-point type is implementation-defined.
 *
 * @see Cursor.isNull
 * @see Cursor.getFloat
 */
public inline fun Cursor.getFloatOrNull(
    index: Int
): Float? = if (isNull(index)) null else getFloat(index)

/**
 * Returns the value of the requested column as a nullable integer.
 *
 * The result and whether this method throws an exception when the column type is not an integral
 * type is implementation-defined.
 *
 * @see Cursor.isNull
 * @see Cursor.getInt
 */
public inline fun Cursor.getIntOrNull(
    index: Int
): Int? = if (isNull(index)) null else getInt(index)

/**
 * Returns the value of the requested column as a nullable long.
 *
 * The result and whether this method throws an exception when the column type is not an integral
 * type is implementation-defined.
 *
 * @see Cursor.isNull
 * @see Cursor.getLong
 */
public inline fun Cursor.getLongOrNull(
    index: Int
): Long? = if (isNull(index)) null else getLong(index)

/**
 * Returns the value of the requested column as a nullable short.
 *
 * The result and whether this method throws an exception when the column type is not an integral
 * type is implementation-defined.
 *
 * @see Cursor.isNull
 * @see Cursor.getShort
 */
public inline fun Cursor.getShortOrNull(
    index: Int
): Short? = if (isNull(index)) null else getShort(index)

/**
 * Returns the value of the requested column as a nullable string.
 *
 * The result and whether this method throws an exception when the column type is not a string type
 * is implementation-defined.
 *
 * @see Cursor.isNull
 * @see Cursor.getString
 */
public inline fun Cursor.getStringOrNull(
    index: Int
): String? = if (isNull(index)) null else getString(index)
