/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.unit

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Any tests for SP need to run in Android because only the Android project relies on core:core with
 * the FontScaleConverterFactory.
 */
class AndroidDensityTest {
    private val density = Density(2f, 3f)

    @Test
    fun testDpToSp() = with(density) {
        assertEquals(2.sp, 6.dp.toSp())
    }

    @Test
    fun testTextUnitToPx() = with(density) {
        assertEquals(6f, 1.sp.toPx(), 0.001f)
    }

    @Test(expected = IllegalStateException::class)
    fun testTextUnitToPxFail() {
        with(density) {
            1.em.toPx()
        }
    }

    @Test
    fun testTextRoundUnitToPx() = with(density) {
        assertEquals(6, 1.sp.roundToPx())
        assertEquals(6, 1.05.sp.roundToPx())
        assertEquals(6, .95.sp.roundToPx())
    }

    @Test(expected = IllegalStateException::class)
    fun testTextUnitRoundToPxFail() {
        with(density) {
            1.em.roundToPx()
        }
    }

    @Test
    fun testTextUnitToDp() = with(density) {
        assertEquals(3.dp, 1.sp.toDp())
    }

    @Test
    fun testIntToSp() = with(density) {
        assertEquals(1.sp, 6.toSp())
    }

    @Test
    fun testFloatToSp() = with(density) {
        assertEquals(1.sp, 6f.toSp())
    }
}
