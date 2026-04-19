/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.util

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.SparseArray
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class CompatUtilsTest {

    @Test
    fun compatContentEquals_sameArrays_returnsTrue() {
        val array1 =
            SparseArray<String>().apply {
                put(1, "one")
                put(2, "two")
            }
        val array2 =
            SparseArray<String>().apply {
                put(1, "one")
                put(2, "two")
            }
        assertThat(array1.compatContentEquals(array2)).isTrue()
    }

    @Test
    fun compatContentEquals_differentSizes_returnsFalse() {
        val array1 = SparseArray<String>().apply { put(1, "one") }
        val array2 =
            SparseArray<String>().apply {
                put(1, "one")
                put(2, "two")
            }
        assertThat(array1.compatContentEquals(array2)).isFalse()
    }

    @Test
    fun compatContentEquals_differentKeys_returnsFalse() {
        val array1 = SparseArray<String>().apply { put(1, "one") }
        val array2 = SparseArray<String>().apply { put(2, "one") }
        assertThat(array1.compatContentEquals(array2)).isFalse()
    }

    @Test
    fun compatContentEquals_differentValues_returnsFalse() {
        val array1 = SparseArray<String>().apply { put(1, "one") }
        val array2 = SparseArray<String>().apply { put(1, "two") }
        assertThat(array1.compatContentEquals(array2)).isFalse()
    }

    @Test
    fun compatContentEquals_emptyArrays_returnsTrue() {
        val array1 = SparseArray<String>()
        val array2 = SparseArray<String>()
        assertThat(array1.compatContentEquals(array2)).isTrue()
    }

    @Test
    fun compatContentEquals_unsortedInsertion_returnsTrue() {
        // SparseArray automatically sorts keys. Insertion order shouldn't matter.
        val array1 =
            SparseArray<String>().apply {
                put(10, "ten")
                put(1, "one")
            }
        val array2 =
            SparseArray<String>().apply {
                put(1, "one")
                put(10, "ten")
            }
        assertThat(array1.compatContentEquals(array2)).isTrue()
    }

    @Test
    fun compatContentEquals_withNullValues_returnsTrue() {
        val array1 = SparseArray<String?>().apply { put(1, null) }
        val array2 = SparseArray<String?>().apply { put(1, null) }
        assertThat(array1.compatContentEquals(array2)).isTrue()
    }

    @Test
    fun compatContentEquals_complexObjects_checksStructuralEquality() {
        val array1 = SparseArray<Point>().apply { put(1, Point(10, 20)) }
        val array2 =
            SparseArray<Point>().apply {
                put(1, Point(10, 20)) // Different instance, same values
            }
        assertThat(array1.compatContentEquals(array2)).isTrue()
    }

    @Test
    fun compatContentEquals_afterRemovingElements_returnsCorrectResult() {
        val array1 =
            SparseArray<String>().apply {
                put(1, "one")
                put(2, "two")
                remove(2)
            }
        val array2 = SparseArray<String>().apply { put(1, "one") }
        assertThat(array1.compatContentEquals(array2)).isTrue()
    }

    private val context =
        androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q]) // Test legacy branch (< API 30)
    fun getDisplaySize_legacySdk_returnsCorrectDimensions() {
        val point = getDisplaySize(context)

        // Default Robolectric screen is usually 480x800 or similar
        assertThat(point.x).isGreaterThan(0)
        assertThat(point.y).isGreaterThan(0)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q], qualifiers = "w768dp-h1024dp-mdpi") // Pre-R, mdpi
    fun getDisplaySize_sdkQ_mdpi_returnsCorrectPixelSize() {
        val displaySize = getDisplaySize(context)
        assertThat(displaySize.x).isEqualTo(768)
        assertThat(displaySize.y).isEqualTo(1024)
    }

    @Test
    @Config(
        sdk = [Build.VERSION_CODES.Q],
        qualifiers = "w320dp-h480dp-xxhdpi",
    ) // Pre-R, xxhdpi (3x density)
    fun getDisplaySize_sdkQ_xxhdpi_returnsCorrectPixelSize() {
        val displaySize = getDisplaySize(context)
        assertThat(displaySize.x).isEqualTo(960) // 320 * 3
        assertThat(displaySize.y).isEqualTo(1440) // 480 * 3
    }
}
