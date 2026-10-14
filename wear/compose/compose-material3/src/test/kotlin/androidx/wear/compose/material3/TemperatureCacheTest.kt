/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.material3

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TemperatureCacheTest {

    @Test
    fun testComplement() {
        val blueComplement = TemperatureCache(Hct.fromInt(0xff0000ff.toInt())).complement.toInt()
        assertSameColor(0xff9d0002.toInt(), blueComplement)

        val redComplement = TemperatureCache(Hct.fromInt(0xffff0000.toInt())).complement.toInt()
        assertSameColor(0xff007bfc.toInt(), redComplement)

        val greenComplement = TemperatureCache(Hct.fromInt(0xff00ff00.toInt())).complement.toInt()
        assertSameColor(0xffffd2c9.toInt(), greenComplement)

        val whiteComplement = TemperatureCache(Hct.fromInt(0xffffffff.toInt())).complement.toInt()
        assertSameColor(0xffffffff.toInt(), whiteComplement)

        val blackComplement = TemperatureCache(Hct.fromInt(0xff000000.toInt())).complement.toInt()
        assertSameColor(0xff000000.toInt(), blackComplement)

        val lightBeigeComplement =
            TemperatureCache(Hct.fromInt(0xffeee1c5.toInt())).complement.toInt()
        assertSameColor(0xffe4e0f2.toInt(), lightBeigeComplement)

        val lightGrayComplement =
            TemperatureCache(Hct.fromInt(0xffdee3e6.toInt())).complement.toInt()
        assertSameColor(0xfff1dfd8.toInt(), lightGrayComplement)
    }

    /** Compares colors as hex strings so that failures are readable. */
    private fun assertSameColor(expected: Int, actual: Int) {
        assertEquals(expected.toHexString(), actual.toHexString())
    }

    private fun Int.toHexString(): String = "#%08X".format(this)
}
