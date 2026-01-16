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

package androidx.pdf.ink.util

import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.ink.brush.StockBrushes
import androidx.pdf.FakeEditablePdfDocument
import androidx.pdf.ink.state.AnnotationDrawingMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class DrawingModeUtilsTest {

    @Test
    fun toBrush_withPenMode_returnsCorrectPenBrush() {
        val penMode = AnnotationDrawingMode.PenMode(color = Color.YELLOW, size = 10f)

        val brush = penMode.toInkBrush()

        assertThat(brush).isNotNull()
        assertThat(brush.family).isEqualTo(StockBrushes.pressurePen())
        assertThat(brush.colorIntArgb).isEqualTo(Color.YELLOW)
        assertThat(brush.size).isEqualTo(10f)
    }

    @Test
    fun toBrush_withHighlighterMode_returnsCorrectHighlighterBrush() {
        val highlighterMode =
            AnnotationDrawingMode.HighlighterMode(
                color = Color.RED,
                size = 20f,
                document = FakeEditablePdfDocument(),
            )

        val brush = highlighterMode.toInkBrush()

        assertThat(brush).isNotNull()
        assertThat(brush.family).isEqualTo(StockBrushes.highlighter())
        assertThat(brush.colorIntArgb).isEqualTo(Color.RED)
        assertThat(brush.size).isEqualTo(20f)
    }

    @Test
    fun toHighlighterConfig_withHighlighterMode_returnsCorrectConfig() {
        val fakeDocument = FakeEditablePdfDocument()
        val highlighterMode =
            AnnotationDrawingMode.HighlighterMode(
                color = Color.BLUE,
                size = 15f,
                document = fakeDocument,
            )

        val config = highlighterMode.toHighlighterConfig()

        assertThat(config.color).isEqualTo(Color.BLUE)
        assertThat(config.pdfDocument).isEqualTo(fakeDocument)
    }
}
