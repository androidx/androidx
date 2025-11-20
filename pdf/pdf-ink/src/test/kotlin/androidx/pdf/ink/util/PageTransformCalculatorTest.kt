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

package androidx.pdf.ink.util

import android.graphics.Matrix
import android.graphics.RectF
import android.util.SparseArray
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PageTransformCalculatorTest {

    private val calculator = PageTransformCalculator()

    @Test
    fun calculate_returnsCorrectMatrices() {
        val pageLocations =
            SparseArray<RectF>().apply {
                put(0, RectF(0f, 0f, 100f, 200f))
                put(1, RectF(0f, 210f, 100f, 410f))
            }
        val zoom = 1.5f

        val matrices =
            calculator.calculate(
                firstVisiblePage = 0,
                visiblePagesCount = 2,
                pageLocations = pageLocations,
                zoomLevel = zoom,
            )

        assertThat(matrices.size).isEqualTo(2)

        // Verify matrix for page 0
        val matrix0 = matrices.get(0)
        val expectedMatrix0 =
            Matrix().apply {
                postScale(zoom, zoom)
                postTranslate(0f, 0f)
            }
        assertThat(matrix0).isEqualTo(expectedMatrix0)

        // Verify matrix for page 1
        val matrix1 = matrices.get(1)
        val expectedMatrix1 =
            Matrix().apply {
                postScale(zoom, zoom)
                postTranslate(0f, 210f)
            }
        assertThat(matrix1).isEqualTo(expectedMatrix1)
    }
}
