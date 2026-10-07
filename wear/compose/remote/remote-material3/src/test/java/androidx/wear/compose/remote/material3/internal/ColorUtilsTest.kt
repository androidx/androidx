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

package androidx.wear.compose.remote.material3.internal

import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.asConstantColor
import androidx.compose.remote.creation.compose.state.createNamedRemoteColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.luminance
import androidx.wear.compose.remote.material3.DocumentPainterRule
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import com.google.common.truth.Truth.assertThat
import kotlin.math.cbrt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ColorUtilsTest {
    @get:Rule val painterRule = DocumentPainterRule()

    @Test
    fun setLuminance_white_returnsGrayOfTone() {
        val color = Color.White.rc.setLuminance(80f).asConstantColor()

        assertColor(color, ToneEightyGray)
    }

    @Test
    fun setLuminance_keepsAlpha() {
        val color = Color.White.copy(alpha = 0.5f).rc.setLuminance(80f).asConstantColor()

        assertThat(color.alpha).isWithin(Tolerance).of(0.5f)
        assertColor(color.copy(alpha = 1f), ToneEightyGray)
    }

    @Test
    fun setLuminance_chromaticColor_setsToneAndKeepsChromaticity() {
        val source = Color(red = 103, green = 80, blue = 164)

        val color = source.rc.setLuminance(30f).asConstantColor()

        assertThat(color.lStar()).isWithin(0.5f).of(30f)
        val linearSource = source.convert(ColorSpaces.LinearSrgb)
        val linearColor = color.convert(ColorSpaces.LinearSrgb)
        assertThat(linearColor.red / linearColor.blue)
            .isWithin(0.02f)
            .of(linearSource.red / linearSource.blue)
        assertThat(linearColor.green / linearColor.blue)
            .isWithin(0.02f)
            .of(linearSource.green / linearSource.blue)
    }

    @Test
    fun setLuminance_black_returnsGrayOfTone() {
        val color = Color.Black.rc.setLuminance(80f).asConstantColor()

        assertColor(color, ToneEightyGray)
    }

    @Test
    fun setLuminance_outOfRange_isCorrected() {
        assertColor(Color.Gray.rc.setLuminance(150f).asConstantColor(), Color.White)
        assertColor(Color.Gray.rc.setLuminance(-10f).asConstantColor(), Color.Black)
    }

    @Test
    fun setLuminance_namedColor_isEvaluatedByPlayer() {
        val named = createNamedRemoteColor("test.color", Color.White)
        assertThat(named.setLuminance(80f).constantValueOrNull).isNull()

        val painter = painterRule.paintDocument {
            RemoteBox(RemoteModifier.fillMaxSize().background(named.setLuminance(80f)))
        }

        assertColor(painter.colorAt(painter.width / 2, painter.height / 2), ToneEightyGray)
    }

    @Test
    fun setLuminance_themeOnBackground_isEvaluatedByPlayer() {
        val painter = painterRule.paintDocument {
            val color = RemoteMaterialTheme.colorScheme.onBackground.setLuminance(80f)
            RemoteBox(RemoteModifier.fillMaxSize().background(color))
        }

        // The default onBackground is white.
        assertColor(painter.colorAt(painter.width / 2, painter.height / 2), ToneEightyGray)
    }

    private fun assertColor(actual: Color, expected: Color) {
        assertThat(actual.red).isWithin(Tolerance).of(expected.red)
        assertThat(actual.green).isWithin(Tolerance).of(expected.green)
        assertThat(actual.blue).isWithin(Tolerance).of(expected.blue)
        assertThat(actual.alpha).isWithin(Tolerance).of(expected.alpha)
    }

    /** CIE L* of this color. */
    private fun Color.lStar(): Float {
        val y = luminance()
        return if (y > 216f / 24389f) 116f * cbrt(y) - 16f else y * 24389f / 27f
    }

    private companion object {
        // One 8-bit channel step.
        const val Tolerance = 1f / 255f

        // HCT tone 80 gray, as returned by Color.White.setLuminance(80f) in Wear Compose.
        val ToneEightyGray = Color(red = 198, green = 198, blue = 198)
    }
}
