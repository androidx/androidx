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

package androidx.pdf.signature

import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import androidx.pdf.signature.model.Signature
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SignatureUtilsTest {

    @Test
    fun scalePathToFit_scalesAndCentersPathCorrectly() {
        val width = 100f
        val height = 100f

        // Create a 10x10 square path
        val path = Path().apply { addRect(0f, 0f, 10f, 10f, Path.Direction.CW) }

        val transformedPath = SignatureUtils.scalePathToFit(path, width, height)

        val bounds = RectF()
        transformedPath.computeBounds(bounds, true)

        // 100 * 0.9 = 90 scale. Center offset is (100 - 90) / 2 = 5.
        assertThat(bounds.left).isWithin(0.01f).of(5f)
        assertThat(bounds.top).isWithin(0.01f).of(5f)
        assertThat(bounds.right).isWithin(0.01f).of(95f)
        assertThat(bounds.bottom).isWithin(0.01f).of(95f)
    }

    @Test
    fun scalePathToFit_withEmptyPath_returnsEmptyPath() {
        val scaledPath = SignatureUtils.scalePathToFit(Path(), 100f, 100f)
        assertThat(scaledPath.isEmpty).isTrue()
    }

    @Test
    fun scaleTypedSignature_setsTextSizeAndCentersCorrectly() {
        val paint = TextPaint()
        val width = 200f
        val height = 100f
        val text = "Test"

        val (point, updatedPaint) =
            SignatureUtils.scaleTypedSignature(
                text,
                width,
                height,
                paint,
                Signature.TypedSignature.FONT_SERIF,
            )

        val expectedAvailableHeight = height * 0.9f // CONTENT_FIT_SCALE

        val tempPaint =
            TextPaint().apply {
                typeface = Typeface.SERIF
                textSize = expectedAvailableHeight
            }
        val metrics = tempPaint.fontMetrics
        val totalFontHeight = metrics.descent - metrics.ascent
        val expectedTextSize = expectedAvailableHeight * (expectedAvailableHeight / totalFontHeight)

        assertThat(updatedPaint.textSize).isEqualTo(expectedTextSize)
        assertThat(updatedPaint.typeface).isEqualTo(Typeface.SERIF)
        assertThat(updatedPaint.textScaleX).isEqualTo(1f)
        assertThat(point).isNotNull()
    }

    @Test
    fun scaleTypedSignature_withLongText_compressesTextScaleX() {
        val paint = spy(TextPaint())
        val width = 100f
        val height = 50f

        val text = "This is a really long signature name that needs scaling"
        doReturn(500f).whenever(paint).measureText(text)

        val (_, updatedPaint) =
            SignatureUtils.scaleTypedSignature(
                text,
                width,
                height,
                paint,
                Signature.TypedSignature.FONT_MONOSPACE,
            )

        assertThat(updatedPaint.textScaleX).isLessThan(1f)
    }

    @Test
    fun scaleTypedSignature_withEmptyText_returnsZeroedPoint() {
        val (point, _) = SignatureUtils.scaleTypedSignature("", 100f, 100f, TextPaint(), 0)
        assertThat(point.x).isEqualTo(0f)
        assertThat(point.y).isEqualTo(0f)
    }

    @Test
    fun scaleUploadedSignature_calculatesTargetRectCorrectly() {
        val width = 200f
        val height = 100f

        val destRect = SignatureUtils.scaleUploadedSignature(width, height)

        val expectedTargetWidth = 180f
        val expectedTargetHeight = 90f

        val expectedOffX = 10f
        val expectedOffY = 5f

        val expectedDestRect =
            RectF(
                expectedOffX,
                expectedOffY,
                expectedOffX + expectedTargetWidth,
                expectedOffY + expectedTargetHeight,
            )

        assertThat(destRect).isEqualTo(expectedDestRect)
    }

    @Test
    fun scaleUploadedSignature_withZeroDimensions_returnsZeroedRect() {
        val rect = SignatureUtils.scaleUploadedSignature(0f, 100f)
        assertThat(rect).isEqualTo(RectF(0f, 0f, 0f, 100f))
    }
}
