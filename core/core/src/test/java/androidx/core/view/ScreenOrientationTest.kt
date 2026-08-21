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

package androidx.core.view

import android.content.pm.ActivityInfo
import android.view.Surface.ROTATION_0
import android.view.Surface.ROTATION_180
import android.view.Surface.ROTATION_270
import android.view.Surface.ROTATION_90
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenOrientationTest {

    /**
     * Simulates resolving an orientation by taking unrotated baseline display dimensions, rotating
     * width and height according to [currentRotation], and evaluating [targetRotation].
     */
    private fun resolveFromBaseline(
        baseWidth: Int,
        baseHeight: Int,
        currentRotation: Int,
        isReverseDefault: Boolean,
        targetRotation: Int,
    ): Int {
        val isSideways = currentRotation == ROTATION_90 || currentRotation == ROTATION_270
        val currentWidth = if (isSideways) baseHeight else baseWidth
        val currentHeight = if (isSideways) baseWidth else baseHeight
        return ScreenOrientation.resolveOrientation(
            currentWidth,
            currentHeight,
            currentRotation,
            isReverseDefault,
            targetRotation,
        )
    }

    @Test
    fun testPortraitNatural_NormalDefault() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolveFromBaseline(1080, 1920, ROTATION_0, false, ROTATION_0),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolveFromBaseline(1080, 1920, ROTATION_0, false, ROTATION_90),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
            resolveFromBaseline(1080, 1920, ROTATION_0, false, ROTATION_180),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            resolveFromBaseline(1080, 1920, ROTATION_0, false, ROTATION_270),
        )
    }

    @Test
    fun testPortraitNatural_NormalDefault_Rotated() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolveFromBaseline(1080, 1920, ROTATION_90, false, ROTATION_0),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolveFromBaseline(1080, 1920, ROTATION_90, false, ROTATION_90),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
            resolveFromBaseline(1080, 1920, ROTATION_90, false, ROTATION_180),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            resolveFromBaseline(1080, 1920, ROTATION_90, false, ROTATION_270),
        )
    }

    @Test
    fun testPortraitNatural_ReverseDefault() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolveFromBaseline(1080, 1920, ROTATION_0, true, ROTATION_0),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            resolveFromBaseline(1080, 1920, ROTATION_0, true, ROTATION_90),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
            resolveFromBaseline(1080, 1920, ROTATION_0, true, ROTATION_180),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolveFromBaseline(1080, 1920, ROTATION_0, true, ROTATION_270),
        )
    }

    @Test
    fun testLandscapeNatural_NormalDefault() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolveFromBaseline(1920, 1080, ROTATION_0, false, ROTATION_0),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
            resolveFromBaseline(1920, 1080, ROTATION_0, false, ROTATION_90),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            resolveFromBaseline(1920, 1080, ROTATION_0, false, ROTATION_180),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolveFromBaseline(1920, 1080, ROTATION_0, false, ROTATION_270),
        )
    }

    @Test
    fun testLandscapeNatural_ReverseDefault() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolveFromBaseline(1920, 1080, ROTATION_0, true, ROTATION_0),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolveFromBaseline(1920, 1080, ROTATION_0, true, ROTATION_90),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            resolveFromBaseline(1920, 1080, ROTATION_0, true, ROTATION_180),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
            resolveFromBaseline(1920, 1080, ROTATION_0, true, ROTATION_270),
        )
    }

    @Test
    fun testIsPortraitAndLandscape() {
        assertTrue(ScreenOrientation.isPortrait(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT))
        assertTrue(ScreenOrientation.isPortrait(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT))
        assertFalse(ScreenOrientation.isPortrait(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE))
        assertFalse(ScreenOrientation.isPortrait(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE))
        assertFalse(ScreenOrientation.isPortrait(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED))

        assertTrue(ScreenOrientation.isLandscape(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE))
        assertTrue(ScreenOrientation.isLandscape(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE))
        assertFalse(ScreenOrientation.isLandscape(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT))
        assertFalse(ScreenOrientation.isLandscape(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT))
        assertFalse(ScreenOrientation.isLandscape(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED))
    }
}
