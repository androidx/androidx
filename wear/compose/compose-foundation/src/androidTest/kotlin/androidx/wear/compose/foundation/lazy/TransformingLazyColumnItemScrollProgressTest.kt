/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation.lazy

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TransformingLazyColumnScrollProgressTest {
    @Test
    fun negativeValuesPackedCorrectly() {
        val progress =
            TransformingLazyColumnItemScrollProgress(
                topOffsetFraction = -0.5f,
                bottomOffsetFraction = 0.2f
            )
        assertEquals(progress.topOffsetFraction, -0.5f, 1e-2f)
        assertEquals(progress.bottomOffsetFraction, 0.2f, 1e-2f)
    }

    @Test
    fun zeroValuesPackedCorrectly() {
        val progress =
            TransformingLazyColumnItemScrollProgress(
                topOffsetFraction = 0f,
                bottomOffsetFraction = 0f
            )
        assertEquals(progress.topOffsetFraction, 0f, 1e-2f)
        assertEquals(progress.bottomOffsetFraction, 0f, 1e-2f)
    }

    @Test
    fun normalValuesPackedCorrectly() {
        val progress =
            TransformingLazyColumnItemScrollProgress(
                topOffsetFraction = 0.3f,
                bottomOffsetFraction = 0.7f
            )
        assertEquals(progress.topOffsetFraction, 0.3f, 1e-2f)
        assertEquals(progress.bottomOffsetFraction, 0.7f, 1e-2f)
    }

    @Test
    fun nanValuesPackedCorrectly() {
        val progress =
            TransformingLazyColumnItemScrollProgress(
                topOffsetFraction = Float.NaN,
                bottomOffsetFraction = Float.NaN
            )
        assertEquals(progress.topOffsetFraction, Float.NaN)
        assertEquals(progress.bottomOffsetFraction, Float.NaN)
    }

    @Test
    fun infValuesPackedCorrectly() {
        val progress =
            TransformingLazyColumnItemScrollProgress(
                topOffsetFraction = Float.NEGATIVE_INFINITY,
                bottomOffsetFraction = Float.POSITIVE_INFINITY
            )
        assertEquals(progress.topOffsetFraction, Float.NEGATIVE_INFINITY)
        assertEquals(progress.bottomOffsetFraction, Float.POSITIVE_INFINITY)
    }
}
