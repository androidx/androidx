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

package androidx.compose.remote.core.operations.layout.managers

import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.measure.MeasurePass
import androidx.compose.remote.core.operations.layout.measure.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class CoreTextAutosizeTest {

    @Test
    fun testAutosizeDoesNotMutateBaseFontSizeAndPreventsInfiniteRelayout() {
        val textId = 1
        val mockPaintContext: PaintContext = mock { on { density } doReturn 1.0f }
        val mockRemoteContext: RemoteContext = mock {
            on { paintContext } doReturn mockPaintContext
            on { getText(textId) } doReturn "Hello Autosize Layout"
        }

        val baseFontSize = 14f
        val coreText =
            CoreText(
                null, // parent
                100, // componentId
                -1, // animationId
                0f,
                0f,
                200f,
                200f, // x, y, width, height
                textId,
                0xFF000000.toInt(), // color
                -1, // colorId
                baseFontSize,
                8f, // minFontSize
                100f, // maxFontSize
                0, // fontStyle
                TextStyle.DEFAULT_FONT_WEIGHT,
                -1, // fontFamilyId
                1, // textAlign
                1, // overflow
                Int.MAX_VALUE, // maxLines
                0f,
                0f,
                1f, // letterSpacing, lineHeightAdd, lineHeightMultiplier
                0,
                0,
                0, // lineBreakStrategy, hyphenationFrequency, justificationMode
                false,
                false, // underline, strikethrough
                null,
                null, // fontAxis, fontAxisValues
                true, // autosize enabled
                0, // flags
                -1, // textStyleId
            )

        // 1. Initial variable update
        coreText.updateVariables(mockRemoteContext)

        val fontSizeValueField = CoreText::class.java.getDeclaredField("mFontSizeValue")
        fontSizeValueField.isAccessible = true
        assertThat(fontSizeValueField.get(coreText)).isEqualTo(baseFontSize)

        // 2. Measure pass with autosize
        val measurePass = MeasurePass()
        val outSize = Size(0f, 0f)

        coreText.computeWrapSize(
            mockPaintContext,
            0f,
            200f,
            0f,
            200f,
            true,
            true,
            measurePass,
            outSize,
        )

        val measureFontSizeField = CoreText::class.java.getDeclaredField("mMeasureFontSize")
        measureFontSizeField.isAccessible = true
        val computedMeasureFontSize = measureFontSizeField.get(coreText) as Float

        // mFontSizeValue must remain the original base font size (not mutated to current)
        assertThat(fontSizeValueField.get(coreText)).isEqualTo(baseFontSize)
        // mMeasureFontSize should be updated to fitted size (greater than base size for 200x200
        // box)
        assertThat(computedMeasureFontSize).isGreaterThan(baseFontSize)

        // Reset mNeedsMeasure (simulating layout pass completion) before testing subsequent tick
        val needsMeasureField = Component::class.java.getDeclaredField("mNeedsMeasure")
        needsMeasureField.isAccessible = true
        needsMeasureField.set(coreText, false)

        // 3. Subsequent variable update should NOT trigger invalidateMeasure (no infinite relayout)
        coreText.updateVariables(mockRemoteContext)

        val needsMeasure = needsMeasureField.get(coreText) as Boolean
        assertThat(needsMeasure).isFalse()
    }

    @Test
    fun testAutosizeMeasuresLayoutWithComputedFontSize() {
        val textId = 1
        val mockPaintContext: PaintContext = mock { on { density } doReturn 1.0f }
        val mockRemoteContext: RemoteContext = mock {
            on { paintContext } doReturn mockPaintContext
            on { getText(textId) } doReturn "Autosize Paint Test"
        }

        val baseFontSize = 14f
        val coreText =
            CoreText(
                null,
                101,
                -1,
                0f,
                0f,
                200f,
                200f,
                textId,
                0xFF000000.toInt(),
                -1,
                baseFontSize,
                8f,
                100f,
                0,
                TextStyle.DEFAULT_FONT_WEIGHT,
                -1,
                1,
                1,
                Int.MAX_VALUE,
                0f,
                0f,
                1f,
                0,
                0,
                0,
                false,
                false,
                null,
                null,
                true,
                0,
                -1,
            )

        coreText.updateVariables(mockRemoteContext)

        val measurePass = MeasurePass()
        val outSize = Size(0f, 0f)

        coreText.computeWrapSize(
            mockPaintContext,
            0f,
            200f,
            0f,
            200f,
            true,
            true,
            measurePass,
            outSize,
        )

        val measureFontSizeField = CoreText::class.java.getDeclaredField("mMeasureFontSize")
        measureFontSizeField.isAccessible = true
        val computedMeasureFontSize = measureFontSizeField.get(coreText) as Float

        assertThat(computedMeasureFontSize).isGreaterThan(baseFontSize)
    }
}
