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

package androidx.compose.ui.graphics

import android.os.Build
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class AndroidColorTest {

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    fun testUnspecifiedCompositeOverToColorLong() {
        val real = Color(red = 3 / 255f, green = 252 / 255f, blue = 21 / 255f, alpha = 0.38f)
        val blended = real.compositeOver(Color.Unspecified)
        val colorLong = blended.toColorLong()

        // Low 6 bits should be 0 (ColorSpace.Named.SRGB), not the blue channel's low bits (21)
        val colorSpaceId = (colorLong and 0x3fL).toInt()
        assertEquals(0, colorSpaceId)

        // Native paint setColor should accept the ColorLong without throwing
        // IllegalArgumentException
        val paint = android.graphics.Paint()
        paint.setColor(colorLong)

        // Compose Paint should also accept the blended color
        val composePaint = Paint()
        composePaint.color = blended
        assertEquals(blended.toArgb(), composePaint.color.toArgb())

        // Verify components encoded in the ColorLong match the expected ARGB values
        val delta = 0.01f
        val srgb = blended.convert(ColorSpaces.Srgb)
        assertEquals(0, android.graphics.Color.colorSpace(colorLong).id)
        assertEquals(srgb.alpha, android.graphics.Color.alpha(colorLong), delta)
        assertEquals(srgb.red, android.graphics.Color.red(colorLong), delta)
        assertEquals(srgb.green, android.graphics.Color.green(colorLong), delta)
        assertEquals(srgb.blue, android.graphics.Color.blue(colorLong), delta)

        // Converting back from ColorLong should match blended's sRGB representation
        val restored = Color.fromColorLong(colorLong)
        assertEquals(blended.toArgb(), restored.toArgb())
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    fun testColorUnspecifiedToColorLong() {
        val colorLong = Color.Unspecified.toColorLong()
        assertEquals(0L, colorLong)
        assertEquals(0, (colorLong and 0x3fL).toInt())
        assertEquals(0, android.graphics.Color.colorSpace(colorLong).id)
        assertEquals(0f, android.graphics.Color.alpha(colorLong), 0.001f)

        val paint = android.graphics.Paint()
        paint.setColor(colorLong)

        val composePaint = Paint()
        composePaint.color = Color.Unspecified
        assertEquals(0, composePaint.color.toArgb())
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    fun testUnspecifiedColorSpaceWithVariousBlueChannels() {
        val paint = android.graphics.Paint()
        // Test various blue channel values where low 6 bits could exceed registered platform IDs
        val blueValues = floatArrayOf(21 / 255f, 45 / 255f, 63 / 255f, 1f)
        for (blue in blueValues) {
            val color =
                Color(
                    red = 0.5f,
                    green = 0.5f,
                    blue = blue,
                    alpha = 0.8f,
                    colorSpace = ColorSpaces.Unspecified,
                )
            val colorLong = color.toColorLong()
            assertEquals(0, (colorLong and 0x3fL).toInt())
            assertEquals(0, android.graphics.Color.colorSpace(colorLong).id)
            paint.setColor(colorLong)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun testBt2020FallbackOnOlderApi() {
        val color = Color(0.1f, 0.2f, 0.3f, colorSpace = ColorSpaces.Bt2020Hlg)
        val colorLong = color.toColorLong()
        // On API 33, Bt2020Hlg falls back to sRGB (id 0)
        assertEquals(0, (colorLong and 0x3fL).toInt())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
    fun testOklabFallbackOnOlderApi() {
        val color = Color(0.1f, 0.2f, 0.3f, colorSpace = ColorSpaces.Oklab)
        val colorLong = color.toColorLong()
        // On API 35, Oklab falls back to sRGB (id 0)
        assertEquals(0, (colorLong and 0x3fL).toInt())
    }
}
