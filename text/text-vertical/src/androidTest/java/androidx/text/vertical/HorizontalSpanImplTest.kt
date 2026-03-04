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

package androidx.text.vertical

import android.graphics.Canvas
import android.graphics.Paint
import android.text.SpannedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@SmallTest
@RunWith(AndroidJUnit4::class)
class HorizontalSpanImplTest {

    private lateinit var paint: Paint
    private lateinit var mockLayout: HorizontalSpanLayout
    private lateinit var impl: HorizontalSpanImpl
    private var buildCount = 0

    @Before
    fun setup() {
        paint = Paint()
        mockLayout = mock()
        buildCount = 0
        impl =
            HorizontalSpanImpl(
                key = { _, text, start, end -> LayoutKey(start, end, text) },
                build = { _, _, _, _ ->
                    buildCount++
                    mockLayout
                },
            )
    }

    @Test
    fun getSize_returns_zero_for_null_text() {
        assertThat(impl.getSize(paint, null, 0, 0, null)).isEqualTo(0)
    }

    @Test
    fun getSize_calculates_width() {
        val text = SpannedString("Test")
        val expectedWidth = 100
        val layout = mock<HorizontalSpanLayout> { on { spanWidth }.thenReturn(expectedWidth) }
        val localImpl =
            HorizontalSpanImpl(
                key = { _, text, start, end -> LayoutKey(start, end, text) },
                build = { _, _, _, _ -> layout },
            )

        val size = localImpl.getSize(paint, text, 0, text.length, null)

        assertThat(size).isEqualTo(expectedWidth)
    }

    @Test
    fun getSize_populates_FontMetrics_when_provided() {
        val text = SpannedString("Test")
        val fm = Paint.FontMetricsInt()

        impl.getSize(paint, text, 0, text.length, fm)

        verify(mockLayout).fillFontMetrics(fm)
    }

    @Test
    fun getSize_uses_cached_layout_on_key_match() {
        val text = SpannedString("Test")

        // First call
        impl.getSize(paint, text, 0, text.length, null)
        assertThat(buildCount).isEqualTo(1)

        // Second call with same key parameters
        impl.getSize(paint, text, 0, text.length, null)
        assertThat(buildCount).isEqualTo(1)
    }

    @Test
    fun getSize_rebuilds_layout_on_key_mismatch() {
        val text = SpannedString("Test")

        // First call: range 0-1 -> key "0-1"
        impl.getSize(paint, text, 0, 1, null)
        assertThat(buildCount).isEqualTo(1)

        // Second call: range 0-2 -> key "0-2"
        impl.getSize(paint, text, 0, 2, null)
        assertThat(buildCount).isEqualTo(2)
    }

    @Test
    fun draw_delegates_to_layout() {
        val text = SpannedString("Test")
        val canvas = mock<Canvas>()
        val x = 10f
        val y = 20

        impl.draw(canvas, text, 0, text.length, x, 0, y, 0, paint)

        verify(mockLayout).draw(eq(canvas), eq(x), eq(y.toFloat()), eq(paint))
    }

    @Test
    fun Shared_cache_between_getSize_and_draw() {
        val text = SpannedString("Test")
        val canvas = mock<Canvas>()

        // Call getSize first
        impl.getSize(paint, text, 0, text.length, null)
        assertThat(buildCount).isEqualTo(1)

        // Call draw with same parameters (same key)
        impl.draw(canvas, text, 0, text.length, 0f, 0, 0, 0, paint)
        assertThat(buildCount).isEqualTo(1)
    }
}
