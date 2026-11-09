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

package androidx.compose.material3

import androidx.compose.material3.internal.colorUtil.Cam
import androidx.compose.material3.internal.colorUtil.Frame
import androidx.compose.material3.internal.colorUtil.SRGB_TO_XYZ
import androidx.compose.material3.internal.colorUtil.linearized
import androidx.compose.material3.internal.colorUtil.yFromLstar
import androidx.compose.material3.tokens.ColorLightTokens
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.cbrt
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ColorUtilTest {
    @Test
    fun setLuminance0_returnsBlack() {
        assertThat(Color.Red.setLuminance(0f)).isEqualTo(Color.Black)
        assertThat(Color.Blue.setLuminance(0f)).isEqualTo(Color.Black)
        assertThat(Color.Green.setLuminance(0f)).isEqualTo(Color.Black)
        assertThat(Color.Magenta.setLuminance(0f)).isEqualTo(Color.Black)
        assertThat(Color.Cyan.setLuminance(0f)).isEqualTo(Color.Black)
        assertThat(Color.White.setLuminance(0f)).isEqualTo(Color.Black)
        assertThat(Color.Black.setLuminance(0f)).isEqualTo(Color.Black)
    }

    @Test
    fun setLuminance100_returnsWhite() {
        assertThat(Color.Red.setLuminance(100f)).isEqualTo(Color.White)
        assertThat(Color.Blue.setLuminance(100f)).isEqualTo(Color.White)
        assertThat(Color.Green.setLuminance(100f)).isEqualTo(Color.White)
        assertThat(Color.Magenta.setLuminance(100f)).isEqualTo(Color.White)
        assertThat(Color.Cyan.setLuminance(100f)).isEqualTo(Color.White)
        assertThat(Color.White.setLuminance(100f)).isEqualTo(Color.White)
        assertThat(Color.Black.setLuminance(100f)).isEqualTo(Color.White)
    }

    @Test
    fun setLuminance_fromBlack() {
        assertColorWithinTolerance(Color(0xff000000), Color.Black.setLuminance(0f), 1f)
        assertColorWithinTolerance(Color(0xff010101), Color.Black.setLuminance(.25f), 1f)
        assertColorWithinTolerance(Color(0xff020202), Color.Black.setLuminance(.5f), 1f)
        assertColorWithinTolerance(Color(0xff030303), Color.Black.setLuminance(.75f), 1f)
        assertColorWithinTolerance(Color(0xff040404), Color.Black.setLuminance(1f), 1f)
        assertColorWithinTolerance(Color(0xff070707), Color.Black.setLuminance(2f), 1f)
        assertColorWithinTolerance(Color(0xff0b0b0b), Color.Black.setLuminance(3f), 1f)
        assertColorWithinTolerance(Color(0xff0e0e0e), Color.Black.setLuminance(4f), 1f)
        assertColorWithinTolerance(Color(0xff111111), Color.Black.setLuminance(5f), 1f)
        assertColorWithinTolerance(Color(0xff131313), Color.Black.setLuminance(6f), 1f)
        assertColorWithinTolerance(Color(0xff151515), Color.Black.setLuminance(7f), 1f)
        assertColorWithinTolerance(Color(0xff181818), Color.Black.setLuminance(8f), 1f)
        assertColorWithinTolerance(Color(0xff191919), Color.Black.setLuminance(9f), 1f)
        assertColorWithinTolerance(Color(0xff1b1b1b), Color.Black.setLuminance(10f), 1f)
        assertColorWithinTolerance(Color(0xff303030), Color.Black.setLuminance(20f), 1f)
        assertColorWithinTolerance(Color(0xff474747), Color.Black.setLuminance(30f), 1f)
        assertColorWithinTolerance(Color(0xff5e5e5e), Color.Black.setLuminance(40f), 1f)
        assertColorWithinTolerance(Color(0xff777777), Color.Black.setLuminance(50f), 1f)
        assertColorWithinTolerance(Color(0xff919191), Color.Black.setLuminance(60f), 1f)
        assertColorWithinTolerance(Color(0xffababab), Color.Black.setLuminance(70f), 1f)
        assertColorWithinTolerance(Color(0xffc6c6c6), Color.Black.setLuminance(80f), 1f)
        assertColorWithinTolerance(Color(0xffe2e2e2), Color.Black.setLuminance(90f), 1f)
        assertColorWithinTolerance(Color(0xFFF9F9F9), Color.Black.setLuminance(98f), 1f)
        assertColorWithinTolerance(Color(0xffffffff), Color.Black.setLuminance(99f), 1f)
        assertColorWithinTolerance(Color(0xffffffff), Color.Black.setLuminance(100f), 1f)
    }

    @Test
    fun setLuminance_fromRed() {
        assertColorWithinTolerance(Color(0xffff0000), Color.Red.setLuminance(53f), 1f)
        assertColorWithinTolerance(Color(0xFF5B0000), Color.Red.setLuminance(0f), 1f)
        assertColorWithinTolerance(Color(0xFF5C0000), Color.Red.setLuminance(.25f), 1f)
        assertColorWithinTolerance(Color(0xFF5C0000), Color.Red.setLuminance(.5f), 1f)
        assertColorWithinTolerance(Color(0xFF5D0000), Color.Red.setLuminance(.75f), 1f)
        assertColorWithinTolerance(Color(0xFF5D0000), Color.Red.setLuminance(1f), 1f)
        assertColorWithinTolerance(Color(0xFF5F0000), Color.Red.setLuminance(2f), 1f)
        assertColorWithinTolerance(Color(0xFF620000), Color.Red.setLuminance(3f), 1f)
        assertColorWithinTolerance(Color(0xFF640000), Color.Red.setLuminance(4f), 1f)
        assertColorWithinTolerance(Color(0xFF670000), Color.Red.setLuminance(5f), 1f)
        assertColorWithinTolerance(Color(0xFF690000), Color.Red.setLuminance(6f), 1f)
        assertColorWithinTolerance(Color(0xFF6C0000), Color.Red.setLuminance(7f), 1f)
        assertColorWithinTolerance(Color(0xFF6E0000), Color.Red.setLuminance(8f), 1f)
        assertColorWithinTolerance(Color(0xFF710000), Color.Red.setLuminance(9f), 1f)
        assertColorWithinTolerance(Color(0xFF740000), Color.Red.setLuminance(10f), 1f)
        assertColorWithinTolerance(Color(0xFF920000), Color.Red.setLuminance(20f), 1f)
        assertColorWithinTolerance(Color(0xFFB10000), Color.Red.setLuminance(30f), 1f)
        assertColorWithinTolerance(Color(0xFFD20000), Color.Red.setLuminance(40f), 1f)
        assertColorWithinTolerance(Color(0xFFF40000), Color.Red.setLuminance(50f), 1f)
        assertColorWithinTolerance(Color(0xFFFF3017), Color.Red.setLuminance(60f), 1f)
        assertColorWithinTolerance(Color(0xFFFF5632), Color.Red.setLuminance(70f), 1f)
        assertColorWithinTolerance(Color(0xFFFF764C), Color.Red.setLuminance(80f), 1f)
        assertColorWithinTolerance(Color(0xFFFF9566), Color.Red.setLuminance(90f), 1f)
        assertColorWithinTolerance(Color(0xFFFFAD7B), Color.Red.setLuminance(98f), 1f)
        assertColorWithinTolerance(Color(0xFFFFB07D), Color.Red.setLuminance(99f), 1f)
        assertColorWithinTolerance(Color(0xFFFFB380), Color.Red.setLuminance(100f), 1f)
    }

    @Test
    fun setLuminance_fromGreen() {
        assertColorWithinTolerance(Color(0xFF00FC00), Color.Green.setLuminance(86f), 1f)
        assertColorWithinTolerance(Color(0xFF002400), Color.Green.setLuminance(0f), 1f)
        assertColorWithinTolerance(Color(0xFF002500), Color.Green.setLuminance(.25f), 1f)
        assertColorWithinTolerance(Color(0xFF002500), Color.Green.setLuminance(.5f), 1f)
        assertColorWithinTolerance(Color(0xFF002500), Color.Green.setLuminance(.75f), 1f)
        assertColorWithinTolerance(Color(0xFF002500), Color.Green.setLuminance(1f), 1f)
        assertColorWithinTolerance(Color(0xFF002700), Color.Green.setLuminance(2f), 1f)
        assertColorWithinTolerance(Color(0xFF002800), Color.Green.setLuminance(3f), 1f)
        assertColorWithinTolerance(Color(0xFF002900), Color.Green.setLuminance(4f), 1f)
        assertColorWithinTolerance(Color(0xFF002A00), Color.Green.setLuminance(5f), 1f)
        assertColorWithinTolerance(Color(0xFF002B00), Color.Green.setLuminance(6f), 1f)
        assertColorWithinTolerance(Color(0xFF002C00), Color.Green.setLuminance(7f), 1f)
        assertColorWithinTolerance(Color(0xFF002D00), Color.Green.setLuminance(8f), 1f)
        assertColorWithinTolerance(Color(0xFF002E00), Color.Green.setLuminance(9f), 1f)
        assertColorWithinTolerance(Color(0xFF003000), Color.Green.setLuminance(10f), 1f)
        assertColorWithinTolerance(Color(0xFF004200), Color.Green.setLuminance(20f), 1f)
        assertColorWithinTolerance(Color(0xFF005B00), Color.Green.setLuminance(30f), 1f)
        assertColorWithinTolerance(Color(0xFF007600), Color.Green.setLuminance(40f), 1f)
        assertColorWithinTolerance(Color(0xFF009200), Color.Green.setLuminance(50f), 1f)
        assertColorWithinTolerance(Color(0xFF00AE00), Color.Green.setLuminance(60f), 1f)
        assertColorWithinTolerance(Color(0xFF00CB00), Color.Green.setLuminance(70f), 1f)
        assertColorWithinTolerance(Color(0xFF00E800), Color.Green.setLuminance(80f), 1f)
        assertColorWithinTolerance(Color(0xFF29FF1C), Color.Green.setLuminance(90f), 1f)
        assertColorWithinTolerance(Color(0xFF54FF3E), Color.Green.setLuminance(98f), 1f)
        assertColorWithinTolerance(Color(0xFF58FF42), Color.Green.setLuminance(99f), 1f)
        assertColorWithinTolerance(Color(0xFF5DFF45), Color.Green.setLuminance(100f), 1f)
    }

    @Test
    fun setLuminance_fromBlue() {
        assertColorWithinTolerance(Color(0xFF0000FF), Color.Blue.setLuminance(32f), 1f)
        assertColorWithinTolerance(Color(0xFF0000A2), Color.Blue.setLuminance(0f), 1f)
        assertColorWithinTolerance(Color(0xFF0000A2), Color.Blue.setLuminance(.25f), 1f)
        assertColorWithinTolerance(Color(0xFF0000A3), Color.Blue.setLuminance(.5f), 1f)
        assertColorWithinTolerance(Color(0xFF0000A4), Color.Blue.setLuminance(.75f), 1f)
        assertColorWithinTolerance(Color(0xFF0000A4), Color.Blue.setLuminance(1f), 1f)
        assertColorWithinTolerance(Color(0xFF0000A7), Color.Blue.setLuminance(2f), 1f)
        assertColorWithinTolerance(Color(0xFF0000AA), Color.Blue.setLuminance(3f), 1f)
        assertColorWithinTolerance(Color(0xFF0000AD), Color.Blue.setLuminance(4f), 1f)
        assertColorWithinTolerance(Color(0xFF0000AF), Color.Blue.setLuminance(5f), 1f)
        assertColorWithinTolerance(Color(0xFF0000B2), Color.Blue.setLuminance(6f), 1f)
        assertColorWithinTolerance(Color(0xFF0000B5), Color.Blue.setLuminance(7f), 1f)
        assertColorWithinTolerance(Color(0xFF0000B8), Color.Blue.setLuminance(8f), 1f)
        assertColorWithinTolerance(Color(0xFF0000BB), Color.Blue.setLuminance(9f), 1f)
        assertColorWithinTolerance(Color(0xFF0000BE), Color.Blue.setLuminance(10f), 1f)
        assertColorWithinTolerance(Color(0xFF0000DB), Color.Blue.setLuminance(20f), 1f)
        assertColorWithinTolerance(Color(0xFF0000F8), Color.Blue.setLuminance(30f), 1f)
        assertColorWithinTolerance(Color(0xFF4523FF), Color.Blue.setLuminance(40f), 1f)
        assertColorWithinTolerance(Color(0xFF7041FF), Color.Blue.setLuminance(50f), 1f)
        assertColorWithinTolerance(Color(0xFF955DFF), Color.Blue.setLuminance(60f), 1f)
        assertColorWithinTolerance(Color(0xFFB878FF), Color.Blue.setLuminance(70f), 1f)
        assertColorWithinTolerance(Color(0xFFD994FF), Color.Blue.setLuminance(80f), 1f)
        assertColorWithinTolerance(Color(0xFFFAB0FF), Color.Blue.setLuminance(90f), 1f)
        assertColorWithinTolerance(Color(0xFFFFC7FF), Color.Blue.setLuminance(98f), 1f)
        assertColorWithinTolerance(Color(0xFFFFCAFF), Color.Blue.setLuminance(99f), 1f)
        assertColorWithinTolerance(Color(0xFFFFCDFF), Color.Blue.setLuminance(100f), 1f)
    }

    @Test
    fun yFromMidgray() {
        assertEquals(18.418, yFromLstar(50.0), 0.001)
    }

    @Test
    fun yFromBlack() {
        assertEquals(0.0, yFromLstar(0.0), 0.001)
    }

    @Test
    fun yFromWhite() {
        assertEquals(100.0, yFromLstar(100.0), 0.001)
    }

    @Test
    fun camFromRed() {
        val cam: Cam = Cam.fromInt(RED)
        assertEquals(113.357f, cam.chroma, 0.001f)
        assertEquals(27.408f, cam.hue, 0.001f)
    }

    @Test
    fun camFromGreen() {
        val cam: Cam = Cam.fromInt(GREEN)
        assertEquals(108.410f, cam.chroma, 0.001f)
        assertEquals(142.139f, cam.hue, 0.001f)
    }

    @Test
    fun camFromBlue() {
        val cam: Cam = Cam.fromInt(BLUE)
        assertEquals(87.230f, cam.chroma, 0.001f)
        assertEquals(282.788f, cam.hue, 0.001f)
    }

    @Test
    fun camFromBlack() {
        val cam: Cam = Cam.fromInt(BLACK)
        assertEquals(0.0f, cam.chroma, 0.001f)
        assertEquals(0.0f, cam.hue, 0.001f)
    }

    @Test
    fun camFromWhite() {
        val cam: Cam = Cam.fromInt(WHITE)
        assertEquals(2.869f, cam.chroma, 0.001f)
        assertEquals(209.492f, cam.hue, 0.001f)
    }

    @Test
    fun redFromGamutMap() {
        val cam: Cam = Cam.fromInt(RED)
        val color: Int = Cam.getInt(cam.hue, cam.chroma, lstarFromInt(RED))
        assertEquals(RED, color)
    }

    @Test
    fun greenFromGamutMap() {
        val cam: Cam = Cam.fromInt(GREEN)
        val color: Int = Cam.getInt(cam.hue, cam.chroma, lstarFromInt(GREEN))
        assertEquals(GREEN, color)
    }

    @Test
    fun blueFromGamutMap() {
        val cam: Cam = Cam.fromInt(BLUE)
        val color: Int = Cam.getInt(cam.hue, cam.chroma, lstarFromInt(BLUE))
        assertEquals(BLUE, color)
    }

    @Test
    fun whiteFromGamutMap() {
        val cam: Cam = Cam.fromInt(WHITE)
        val color: Int = Cam.getInt(cam.hue, cam.chroma, lstarFromInt(WHITE))
        assertEquals(WHITE, color)
    }

    @Test
    fun blackFromGamutMap() {
        val cam: Cam = Cam.fromInt(BLACK)
        val color: Int = Cam.getInt(cam.hue, cam.chroma, lstarFromInt(BLACK))
        assertEquals(BLACK, color)
    }

    @Test
    fun midgrayFromGamutMap() {
        val cam: Cam = Cam.fromInt(MIDGRAY)
        val color: Int = Cam.getInt(cam.hue, cam.chroma, lstarFromInt(MIDGRAY))
        assertEquals(MIDGRAY, color)
    }

    @Test
    fun randomGreenFromGamutMap() {
        val colorToTest = -0xff6e00
        val cam: Cam = Cam.fromInt(colorToTest)
        val color: Int = Cam.getInt(cam.hue, cam.chroma, lstarFromInt(colorToTest))
        assertEquals(colorToTest.toLong(), color.toLong())
    }

    @Test
    fun gamutMapArbitraryHCL() {
        val color: Int = Cam.getInt(309.0f, 40.0f, 70.0f)
        val cam: Cam = Cam.fromInt(color)

        assertEquals(308.759f, cam.hue, 0.001f)
        assertEquals(40.148f, cam.chroma, 0.001f)
        assertEquals(70.029f, lstarFromInt(color), 0.001f)
    }

    @Test
    fun viewingConditions_default() {
        val vc = Frame.Default

        assertEquals(0.184f, vc.n, 0.001f)
        assertEquals(29.981f, vc.aw, 0.001f)
        assertEquals(1.016f, vc.nbb, 0.001f)
        assertEquals(1.021f, vc.rgbD[0], 0.001f)
        assertEquals(0.986f, vc.rgbD[1], 0.001f)
        assertEquals(0.933f, vc.rgbD[2], 0.001f)
    }

    @Test
    fun generatedTones_matchGoldenValues() {
        // Tones generated from key colors the way the SDK 31-33 dynamic color schemes generate the
        // neutral variant tones missing from the system palette, computed with the original port of
        // the color math. Changes to the color math must not change them.
        val tones = floatArrayOf(98f, 96f, 94f, 92f, 87f, 24f, 22f, 17f, 12f, 6f, 4f)
        val expectedTones =
            mapOf(
                // Low chroma key colors, like the system neutral variant tone 40.
                0xFF6A5B5E to
                    "FFF8F8 FFF0F2 FEE9EC F8E3E7 E9D5D8 433639 3E3134 33272A 281D1F 1B1113 150B0E",
                0xFF6B5B54 to
                    "FFF8F6 FFF1EB FFEAE1 FAE4DA EBD6CD 443630 3F322C 342721 291D18 1B110C 160C07",
                0xFF655E4F to
                    "FFF8F1 FDF2DF F8EDD9 F2E7D4 E3D9C6 3E392B 3A3427 2F291D 241F13 171308 120E04",
                0xFF5B6055 to
                    "F7FBED F1F5E7 EBF0E1 E5EADC D7DCCE 363B31 31362D 272B22 1C2118 10150D 0B0F08",
                0xFF57605D to
                    "F2FBF7 ECF6F1 E7F0EC E1EAE6 D3DCD8 323B38 2E3734 232C29 19211F 0D1513 08100E",
                0xFF576064 to
                    "F2FBFF ECF5FA E6EFF4 E0EAEE D2DBE0 323B3E 2D363A 232B2F 182124 0C1518 070F13",
                0xFF5C5E67 to
                    "FAF8FF F2F3FE ECEDF8 E7E7F2 D8D9E4 373941 32343D 272A32 1D1F27 11131A 0B0E15",
                0xFF635C65 to
                    "FFF7FD FBF0FB F5EBF5 EFE5F0 E1D7E2 3D373F 39333B 2D2830 231E25 161219 110D14",
                // Saturated key colors, which need gamut mapping for some tones.
                0xFFFF0000 to
                    "FFF8F6 FFF0EE FFE9E6 FFE2DD FFCFC7 790100 710100 5C0000 490000 310000 270000",
                0xFF00FF00 to
                    "EDFFE1 D7FFC6 BDFFA9 9FFF88 00FD00 014400 013F00 013200 002600 001800 001200",
                0xFF0000FF to
                    "FBF8FF F5F2FF EEECFF E7E6FF D6D7FF 0001C6 0001B9 000199 00007A 000055 000046",
                0xFFFFFF00 to
                    "FFFEAC FCFC00 F6F600 F0F000 E1E100 3B3B00 373700 2C2C00 212100 141400 0F0F00",
                0xFF6750A4 to
                    "FDF7FF F8F1FF F3EAFF EEE4FF E1D3FF 41287B 3C2377 31166B 260661 190048 13003A",
            )
        expectedTones.forEach { (keyColor, expected) ->
            val cam = Cam.fromInt(keyColor.toInt())
            assertWithMessage("Tones of %08X".format(keyColor))
                .that(tones.map { Color(Cam.getInt(cam.hue, cam.chroma, it)) })
                .containsExactlyElementsIn(
                    expected.split(" ").map { Color(0xFF000000 or it.toLong(16)) }
                )
                .inOrder()
        }
    }

    @Test
    fun testHctReflexivity() {
        val baseline =
            listOf(
                SURFACE,
                SURFACE_CONTAINER,
                ON_SURFACE,
                PRIMARY,
                ON_PRIMARY,
                SECONDARY,
                ON_SECONDARY,
                TERTIARY,
                ON_TERTIARY,
                PRIMARY_CONTAINER,
                ON_PRIMARY_CONTAINER,
                SECONDARY_CONTAINER,
                ON_SECONDARY_CONTAINER,
                TERTIARY_CONTAINER,
                ON_TERTIARY_CONTAINER,
                ERROR,
                ON_ERROR,
                ERROR_CONTAINER,
                ON_ERROR_CONTAINER,
            )
        for (i in baseline) {
            val color = -0x1000000 or i
            val hct: Cam = Cam.fromInt(color)
            val reconstructedFromHct: Int = Cam.getInt(hct.hue, hct.chroma, lstarFromInt(color))

            assertEquals(
                "input was " +
                    Integer.toHexString(color) +
                    "; output was " +
                    Integer.toHexString(reconstructedFromHct),
                color.toLong(),
                reconstructedFromHct.toLong(),
            )
        }
    }

    companion object {
        const val BLACK: Int = -0x1000000
        const val WHITE: Int = -0x1
        const val MIDGRAY: Int = -0x888889

        const val RED: Int = -0x10000
        const val GREEN: Int = -0xff0100
        const val BLUE: Int = -0xffff01

        val SURFACE: Int = ColorLightTokens.Surface.toArgb()
        val SURFACE_CONTAINER: Int = ColorLightTokens.SurfaceContainer.toArgb()
        val ON_SURFACE: Int = ColorLightTokens.OnSurface.toArgb()
        val PRIMARY: Int = ColorLightTokens.Primary.toArgb()
        val ON_PRIMARY: Int = ColorLightTokens.OnPrimary.toArgb()
        val SECONDARY: Int = ColorLightTokens.Secondary.toArgb()
        val ON_SECONDARY: Int = ColorLightTokens.OnSecondary.toArgb()
        val TERTIARY: Int = ColorLightTokens.Tertiary.toArgb()
        val ON_TERTIARY: Int = ColorLightTokens.OnTertiary.toArgb()
        val PRIMARY_CONTAINER: Int = ColorLightTokens.PrimaryContainer.toArgb()
        val ON_PRIMARY_CONTAINER: Int = ColorLightTokens.OnPrimaryContainer.toArgb()
        val SECONDARY_CONTAINER: Int = ColorLightTokens.SecondaryContainer.toArgb()
        val ON_SECONDARY_CONTAINER: Int = ColorLightTokens.OnSecondaryContainer.toArgb()
        val TERTIARY_CONTAINER: Int = ColorLightTokens.TertiaryContainer.toArgb()
        val ON_TERTIARY_CONTAINER: Int = ColorLightTokens.OnTertiaryContainer.toArgb()
        val ERROR: Int = ColorLightTokens.Error.toArgb()
        val ON_ERROR: Int = ColorLightTokens.OnError.toArgb()
        val ERROR_CONTAINER: Int = ColorLightTokens.ErrorContainer.toArgb()
        val ON_ERROR_CONTAINER: Int = ColorLightTokens.OnErrorContainer.toArgb()
    }

    private fun assertColorWithinTolerance(expected: Color, actual: Color, tolerance: Float = 1f) {
        assertThat(expected.red).isWithin(tolerance).of(actual.red)
        assertThat(expected.green).isWithin(tolerance).of(actual.green)
        assertThat(expected.blue).isWithin(tolerance).of(actual.blue)
        assertThat(expected.alpha).isWithin(tolerance).of(actual.alpha)
    }
}

/**
 * Sets the luminance (tone) of this color, keeping its hue and chroma. Chroma may decrease because
 * chroma has a different maximum for any given hue and luminance.
 */
private fun Color.setLuminance(newLuminance: Float): Color {
    val cam = Cam.fromInt(toArgb())
    return Color(Cam.getInt(cam.hue, cam.chroma, newLuminance))
}

/** Returns L* from L*a*b*, perceptual luminance, from an ARGB integer (ColorInt). */
private fun lstarFromInt(argb: Int): Float {
    val matrix = SRGB_TO_XYZ
    val y =
        (linearized(argb.red) * matrix[3]) +
            (linearized(argb.green) * matrix[4]) +
            (linearized(argb.blue) * matrix[5])
    val yNormalized = y.toFloat() / 100.0f
    return if (yNormalized <= 216f / 24389f) {
        (24389f / 27f) * yNormalized
    } else {
        116f * cbrt(yNormalized.toDouble()).toFloat() - 16f
    }
}
