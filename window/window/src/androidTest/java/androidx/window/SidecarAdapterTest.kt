/*
 * Copyright 2021 The Android Open Source Project
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
// Sidecar is deprecated but consuming code must be maintained for compatibility reasons
@file:Suppress("DEPRECATION")

package androidx.window

import android.graphics.Rect
import androidx.window.FoldingFeature.State.Companion.FLAT
import androidx.window.FoldingFeature.Type.Companion.FOLD
import androidx.window.sidecar.SidecarDeviceState
import androidx.window.sidecar.SidecarDisplayFeature
import androidx.window.sidecar.SidecarWindowLayoutInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

public class SidecarAdapterTest : TranslatorTestInterface {

    private lateinit var windowBoundsHelper: TestWindowBoundsHelper

    @Before
    public fun setUp() {
        windowBoundsHelper = TestWindowBoundsHelper()
        windowBoundsHelper.setCurrentBounds(WINDOW_BOUNDS)
        WindowBoundsHelper.setForTesting(windowBoundsHelper)
    }

    @After
    public fun tearDown() {
        WindowBoundsHelper.setForTesting(null)
    }

    @Test
    override fun testTranslate_validFeature() {
        val bounds = Rect(WINDOW_BOUNDS.left, 0, WINDOW_BOUNDS.right, 0)
        val foldFeature = sidecarDisplayFeature(
            bounds,
            SidecarDisplayFeature.TYPE_FOLD
        )
        val sidecarDisplayFeatures = listOf(foldFeature)
        val windowLayoutInfo = sidecarWindowLayoutInfo(sidecarDisplayFeatures)
        val state = sidecarDeviceState(SidecarDeviceState.POSTURE_OPENED)
        val expectedFeatures = listOf(FoldingFeature(Bounds(foldFeature.rect), FOLD, FLAT))
        val expected = WindowLayoutInfo(expectedFeatures)
        val sidecarAdapter = SidecarAdapter()
        val actual = sidecarAdapter.translate(windowLayoutInfo, state)
        assertEquals(expected, actual)
    }

    @Test
    public fun testTranslateWindowLayoutInfo_filterRemovesEmptyBoundsFeature() {
        val sidecarDisplayFeatures = listOf(
            sidecarDisplayFeature(Rect(), SidecarDisplayFeature.TYPE_FOLD)
        )
        val sidecarAdapter = SidecarAdapter()
        val windowLayoutInfo = sidecarWindowLayoutInfo(sidecarDisplayFeatures)
        val state = sidecarDeviceState(SidecarDeviceState.POSTURE_OPENED)
        val actual = sidecarAdapter.translate(windowLayoutInfo, state)
        assertTrue(actual.displayFeatures.isEmpty())
    }

    @Test
    public fun testTranslateWindowLayoutInfo_filterRemovesNonEmptyAreaFoldFeature() {
        val fullWidthBounds = Rect(0, 1, WINDOW_BOUNDS.width(), 2)
        val fullHeightBounds = Rect(1, 0, 2, WINDOW_BOUNDS.height())
        val sidecarDisplayFeatures = listOf(
            sidecarDisplayFeature(
                fullWidthBounds,
                SidecarDisplayFeature.TYPE_FOLD
            ),
            sidecarDisplayFeature(
                fullHeightBounds,
                SidecarDisplayFeature.TYPE_FOLD
            )
        )
        val sidecarCallbackAdapter = SidecarAdapter()
        val windowLayoutInfo = sidecarWindowLayoutInfo(sidecarDisplayFeatures)
        val state = sidecarDeviceState(SidecarDeviceState.POSTURE_OPENED)
        val actual = sidecarCallbackAdapter.translate(windowLayoutInfo, state)
        assertTrue(actual.displayFeatures.isEmpty())
    }

    // TODO(b/175507310): Reinstate after fix.
    // @Test
    override fun testTranslateWindowLayoutInfo_filterRemovesHingeFeatureNotSpanningFullDimension() {
        val fullWidthBounds = Rect(
            WINDOW_BOUNDS.left, WINDOW_BOUNDS.top,
            WINDOW_BOUNDS.right / 2, 2
        )
        val fullHeightBounds = Rect(
            WINDOW_BOUNDS.left, WINDOW_BOUNDS.top, 2,
            WINDOW_BOUNDS.bottom / 2
        )
        val sidecarDisplayFeatures = listOf(
            sidecarDisplayFeature(
                fullWidthBounds,
                SidecarDisplayFeature.TYPE_HINGE
            ),
            sidecarDisplayFeature(
                fullHeightBounds,
                SidecarDisplayFeature.TYPE_HINGE
            )
        )
        val sidecarAdapter = SidecarAdapter()
        val windowLayoutInfo = sidecarWindowLayoutInfo(sidecarDisplayFeatures)
        val state = sidecarDeviceState(SidecarDeviceState.POSTURE_OPENED)
        val actual = sidecarAdapter.translate(windowLayoutInfo, state)
        assertTrue(actual.displayFeatures.isEmpty())
    }

    // TODO(b/175507310): Reinstate after fix.
    // @Test
    override fun testTranslateWindowLayoutInfo_filterRemovesFoldFeatureNotSpanningFullDimension() {
        val fullWidthBounds = Rect(
            WINDOW_BOUNDS.left, WINDOW_BOUNDS.top,
            WINDOW_BOUNDS.right / 2, WINDOW_BOUNDS.top
        )
        val fullHeightBounds = Rect(
            WINDOW_BOUNDS.left, WINDOW_BOUNDS.top, WINDOW_BOUNDS.left,
            WINDOW_BOUNDS.bottom / 2
        )
        val extensionDisplayFeatures = listOf(
            sidecarDisplayFeature(
                fullWidthBounds,
                SidecarDisplayFeature.TYPE_HINGE
            ),
            sidecarDisplayFeature(
                fullHeightBounds,
                SidecarDisplayFeature.TYPE_HINGE
            )
        )
        val sidecarCallbackAdapter = SidecarAdapter()
        val windowLayoutInfo = sidecarWindowLayoutInfo(
            extensionDisplayFeatures
        )
        val state = sidecarDeviceState(SidecarDeviceState.POSTURE_OPENED)
        val actual = sidecarCallbackAdapter.translate(windowLayoutInfo, state)
        assertTrue(actual.displayFeatures.isEmpty())
    }

    @Test
    override fun testTranslateWindowLayoutInfo_filterRemovesUnknownFeature() {
        val bounds = Rect(WINDOW_BOUNDS.left, 0, WINDOW_BOUNDS.right, 0)
        val unknownFeature = sidecarDisplayFeature(bounds, 0 /* unknown */)

        val sidecarDisplayFeatures = listOf(unknownFeature)
        val sidecarAdapter = SidecarAdapter()
        val windowLayoutInfo = sidecarWindowLayoutInfo(sidecarDisplayFeatures)
        val state = sidecarDeviceState(SidecarDeviceState.POSTURE_OPENED)
        val actual = sidecarAdapter.translate(windowLayoutInfo, state)
        assertTrue(actual.displayFeatures.isEmpty())
    }

    @Test
    public fun testTranslateWindowLayoutInfo_filterRemovesInvalidPostureFeature() {
        val bounds = Rect(WINDOW_BOUNDS.left, 0, WINDOW_BOUNDS.right, 0)
        val unknownFeature = sidecarDisplayFeature(bounds, -1000 /* invalid */)
        val sidecarDisplayFeatures = listOf(unknownFeature)
        val sidecarAdapter = SidecarAdapter()
        val windowLayoutInfo = sidecarWindowLayoutInfo(sidecarDisplayFeatures)
        val state = sidecarDeviceState(SidecarDeviceState.POSTURE_OPENED)
        val actual = sidecarAdapter.translate(windowLayoutInfo, state)
        assertTrue(actual.displayFeatures.isEmpty())
    }

    internal companion object {
        private val WINDOW_BOUNDS = Rect(0, 0, 50, 100)
        private fun sidecarDisplayFeature(bounds: Rect, type: Int): SidecarDisplayFeature {
            val feature = SidecarDisplayFeature()
            feature.rect = bounds
            feature.type = type
            return feature
        }

        private fun sidecarWindowLayoutInfo(
            features: List<SidecarDisplayFeature>
        ): SidecarWindowLayoutInfo {
            val layoutInfo = SidecarWindowLayoutInfo()
            SidecarAdapter.setSidecarDisplayFeatures(layoutInfo, features)
            return layoutInfo
        }

        private fun sidecarDeviceState(posture: Int): SidecarDeviceState {
            val deviceState = SidecarDeviceState()
            SidecarAdapter.setSidecarDevicePosture(deviceState, posture)
            return deviceState
        }
    }
}
