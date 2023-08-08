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

package androidx.compose.ui.graphics

import android.graphics.BlendModeColorFilter as AndroidBlendModeColorFilter
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter as AndroidColorMatrixColorFilter
import android.graphics.LightingColorFilter as AndroidLightingColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter as AndroidPorterDuffColorFilter
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidColorFilterTest {

    @Test
    fun testBlendModeColorFilterEquals() {
        assertEquals(
            BlendModeColorFilter(Color.Yellow, BlendMode.Softlight),
            BlendModeColorFilter(Color.Yellow, BlendMode.Softlight)
        )
    }

    @Test
    fun testColorMatrixColorFilterEquals() {
        assertEquals(
            ColorMatrixColorFilter(ColorMatrix(FloatArray(20) { i -> i.toFloat() })),
            ColorMatrixColorFilter(ColorMatrix(FloatArray(20) { i -> i.toFloat() }))
        )
    }

    @Test
    fun testLightingColorFilterEquals() {
        assertEquals(
            LightingColorFilter(Color.Blue, Color.Yellow),
            LightingColorFilter(Color.Blue, Color.Yellow)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testAndroidBlendModeColorFilterToCompose() {
        val androidFilter = AndroidBlendModeColorFilter(
            android.graphics.Color.RED,
            android.graphics.BlendMode.COLOR_DODGE
        )

        val composeFilter = androidFilter.asComposeColorFilter()
        assertTrue(composeFilter is BlendModeColorFilter)
        val blendModeFilter = composeFilter as BlendModeColorFilter
        assertEquals(Color.Red, blendModeFilter.color)
        assertEquals(BlendMode.ColorDodge, blendModeFilter.blendMode)
        assertEquals(BlendModeColorFilter(Color.Red, BlendMode.ColorDodge), blendModeFilter)

        val convertedAndroidFilter = composeFilter.asAndroidColorFilter()
        assertTrue(convertedAndroidFilter is AndroidBlendModeColorFilter)
        val convertedAndroidBlendModeFilter = convertedAndroidFilter as AndroidBlendModeColorFilter
        assertEquals(android.graphics.Color.RED, convertedAndroidBlendModeFilter.color)
        assertEquals(android.graphics.BlendMode.COLOR_DODGE, convertedAndroidBlendModeFilter.mode)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAndroidLightingColorFilterToCompose() {
        val androidFilter = AndroidLightingColorFilter(
            android.graphics.Color.RED,
            android.graphics.Color.BLUE
        )

        val composeFilter = androidFilter.asComposeColorFilter()
        assertTrue(composeFilter is LightingColorFilter)
        val lightingColorFilter = composeFilter as LightingColorFilter
        assertEquals(Color.Red, lightingColorFilter.multiply)
        assertEquals(Color.Blue, lightingColorFilter.add)

        val convertedAndroidFilter = composeFilter.asAndroidColorFilter()
        assertTrue(convertedAndroidFilter is AndroidLightingColorFilter)
        val convertedAndroidLightingFilter = convertedAndroidFilter as AndroidLightingColorFilter
        assertEquals(android.graphics.Color.RED, convertedAndroidLightingFilter.colorMultiply)
        assertEquals(android.graphics.Color.BLUE, convertedAndroidLightingFilter.colorAdd)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAndroidColorMatrixColorFilterToCompose() {
        val inputColorMatrix = FloatArray(20) { i -> i.toFloat() }
        val androidFilter = AndroidColorMatrixColorFilter(inputColorMatrix)

        val composeFilter = androidFilter.asComposeColorFilter()
        assertTrue(composeFilter is ColorMatrixColorFilter)
        val colorMatrixColorFilter = composeFilter as ColorMatrixColorFilter
        val colorMatrix = ColorMatrix()
        colorMatrixColorFilter.copyColorMatrix(colorMatrix)

        assertTrue(inputColorMatrix.contentEquals(colorMatrix.values))

        val convertedAndroidFilter = composeFilter.asAndroidColorFilter()
        assertTrue(convertedAndroidFilter is AndroidColorMatrixColorFilter)
        val convertedColorMatrixFilter = convertedAndroidFilter as AndroidColorMatrixColorFilter

        val convertedColorMatrix = AndroidColorMatrix()
        convertedColorMatrixFilter.getColorMatrix(convertedColorMatrix)
        assertTrue(colorMatrix.values.contentEquals(convertedColorMatrix.array))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAndroidColorMatrixColorFilterAsComposeColorFilterEquals() {
        val androidColorMatrixFilter = android.graphics.ColorMatrixColorFilter(
            FloatArray(20) { i -> i.toFloat() }
        )
        assertEquals(
            ColorMatrixColorFilter(ColorMatrix(FloatArray(20) { i -> i.toFloat() })),
            androidColorMatrixFilter.asComposeColorFilter()
        )
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    @Test
    fun testAndroidLightingColorFilterToComposeFallback() {
        val androidFilter = AndroidLightingColorFilter(
            android.graphics.Color.RED,
            android.graphics.Color.BLUE
        )

        val composeFilter = androidFilter.asComposeColorFilter()
        // Returns an opaque ColorFilter instance as LightingColorFilter parameters
        // were not inspectable until Android O+
        assertFalse(composeFilter is LightingColorFilter)
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testAndroidBlendModeColorFilterToComposeFallback() {
        val androidFilter = AndroidPorterDuffColorFilter(
            android.graphics.Color.RED,
            PorterDuff.Mode.XOR
        )

        val composeFilter = androidFilter.asComposeColorFilter()
        // Returns an opaque ColorFilter instance as PorterDuffColorFilter is not
        // inspectable and replaced by BlendModeColorFilter on Android Q+
        assertFalse(composeFilter is BlendModeColorFilter)
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    @Test
    fun testAndroidColorMatrixColorFilterToComposeFallback() {
        val androidFilter = AndroidColorMatrixColorFilter(
            FloatArray(20) { i -> i.toFloat() }
        )

        val composeFilter = androidFilter.asComposeColorFilter()
        // Returns an opaque ColorFilter instance as ColorMatrixColorFilter is not
        // inspectable until Android O+
        assertFalse(composeFilter is ColorMatrixColorFilter)
    }
}
