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

package androidx.core.database

import android.database.Cursor
import android.database.MatrixCursor
import androidx.test.filters.SmallTest
import org.junit.Assert.assertNull
import org.junit.Test

@SmallTest
class CursorTest {
    @Test
    fun blobOrNullByIndex() {
        val cursor = scalarCursor(null)
        val blob = cursor.getBlobOrNull(0)
        assertNull(blob)
    }

    @Test
    fun doubleOrNullByIndex() {
        val cursor = scalarCursor(null)
        val double = cursor.getDoubleOrNull(0)
        assertNull(double)
    }

    @Test
    fun floatOrNullByIndex() {
        val cursor = scalarCursor(null)
        val float = cursor.getFloatOrNull(0)
        assertNull(float)
    }

    @Test
    fun intOrNullByIndex() {
        val cursor = scalarCursor(null)
        val int = cursor.getIntOrNull(0)
        assertNull(int)
    }

    @Test
    fun longOrNullByIndex() {
        val cursor = scalarCursor(null)
        val long = cursor.getLongOrNull(0)
        assertNull(long)
    }

    @Test
    fun shortOrNullByIndex() {
        val cursor = scalarCursor(null)
        val short = cursor.getShortOrNull(0)
        assertNull(short)
    }

    @Test
    fun stringOrNullByIndex() {
        val cursor = scalarCursor(null)
        val string = cursor.getStringOrNull(0)
        assertNull(string)
    }

    private fun scalarCursor(item: Any?): Cursor =
        MatrixCursor(arrayOf("data")).apply {
            addRow(arrayOf(item))
            moveToFirst() // Prepare for consumers to read.
        }
}
