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

package androidx.core.os

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@SdkSuppress(minSdkVersion = 21)
@SmallTest
class PersistableBundleTest {
    @Test fun persistableBundleOfValid() {
        val bundle = persistableBundleOf(
            "null" to null,

            "double" to 1.0,
            "int" to 1,
            "long" to 1L,

            "string" to "hey",

            "doubleArray" to doubleArrayOf(1.0, 2.0, 3.0),
            "intArray" to intArrayOf(1, 2, 3),
            "longArray" to longArrayOf(1L, 2L, 3L),

            "stringArray" to arrayOf("hey")
        )

        assertEquals(9, bundle.size())

        assertNull(bundle["null"])

        assertEquals(1.0, bundle["double"])
        assertEquals(1, bundle["int"])
        assertEquals(1L, bundle["long"])

        assertEquals("hey", bundle["string"])

        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), bundle["doubleArray"] as DoubleArray, 0.0)
        assertArrayEquals(intArrayOf(1, 2, 3), bundle["intArray"] as IntArray)
        assertArrayEquals(longArrayOf(1L, 2L, 3L), bundle["longArray"] as LongArray)

        assertThat(bundle["stringArray"] as Array<*>).asList().containsExactly("hey")
    }

    @SdkSuppress(minSdkVersion = 22)
    @Test fun persistableBundleOfValidApi22() {
        val bundle = persistableBundleOf(
            "boolean" to true,
            "booleanArray" to booleanArrayOf()
        )

        assertEquals(true, bundle["boolean"])
        assertArrayEquals(booleanArrayOf(), bundle["booleanArray"] as BooleanArray)
    }

    @Test fun persistableBundleOfInvalid() {
        assertThrows<IllegalArgumentException> {
            persistableBundleOf(
                "nope" to View(ApplicationProvider.getApplicationContext() as Context)
            )
        }.hasMessageThat().isEqualTo("Illegal value type android.view.View for key \"nope\"")

        assertThrows<IllegalArgumentException> {
            persistableBundleOf(
                "nopes" to arrayOf(View(ApplicationProvider.getApplicationContext() as Context))
            )
        }.hasMessageThat().isEqualTo("Illegal value array type android.view.View for key \"nopes\"")
    }

    @Test fun mapToPersistableBundle() {
        val map = mapOf(
            "null" to null,

            "double" to 1.0,
            "int" to 1,
            "long" to 1L,

            "string" to "hey",

            "doubleArray" to doubleArrayOf(1.0, 2.0, 3.0),
            "intArray" to intArrayOf(1, 2, 3),
            "longArray" to longArrayOf(1L, 2L, 3L),

            "stringArray" to arrayOf("hey")
        )
        val bundle = map.toPersistableBundle()

        assertEquals(9, bundle.size())

        assertNull(bundle["null"])

        assertEquals(1.0, bundle["double"])
        assertEquals(1, bundle["int"])
        assertEquals(1L, bundle["long"])

        assertEquals("hey", bundle["string"])

        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), bundle["doubleArray"] as DoubleArray, 0.0)
        assertArrayEquals(intArrayOf(1, 2, 3), bundle["intArray"] as IntArray)
        assertArrayEquals(longArrayOf(1L, 2L, 3L), bundle["longArray"] as LongArray)

        assertThat(bundle["stringArray"] as Array<*>).asList().containsExactly("hey")
    }
}
