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
package androidx.window

import android.app.Activity
import android.graphics.Rect
import androidx.window.FoldingFeature.State.Companion.FLAT
import androidx.window.FoldingFeature.Type.Companion.FOLD
import androidx.window.extensions.ExtensionDisplayFeature
import androidx.window.extensions.ExtensionFoldingFeature
import androidx.window.extensions.ExtensionFoldingFeature.STATE_FLAT
import androidx.window.extensions.ExtensionFoldingFeature.TYPE_FOLD
import androidx.window.extensions.ExtensionFoldingFeature.TYPE_HINGE
import androidx.window.extensions.ExtensionWindowLayoutInfo
import com.nhaarman.mockitokotlin2.mock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [ExtensionAdapter] checking that the data is translated correctly from
 * window.extensions to window.window.  Implements [TranslatorTestInterface] to ensure same
 * requirements are met as [SidecarAdapter]
 *
 * Run: ./gradlew window:window:connectedAndroidTest
 */
public class ExtensionAdapterTest : TranslatorTestInterface {

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
        val mockActivity = mock<Activity>()
        val bounds = Rect(WINDOW_BOUNDS.left, 0, WINDOW_BOUNDS.right, 0)
        val foldFeature = ExtensionFoldingFeature(bounds, TYPE_FOLD, STATE_FLAT)
        val extensionDisplayFeatures = listOf(foldFeature)
        val windowLayoutInfo = ExtensionWindowLayoutInfo(extensionDisplayFeatures)
        val expectedFeatures = listOf(FoldingFeature(Bounds(foldFeature.bounds), FOLD, FLAT))
        val expected = WindowLayoutInfo(expectedFeatures)
        val adapter = ExtensionAdapter()
        val actual = adapter.translate(mockActivity, windowLayoutInfo)
        assertEquals(expected, actual)
    }

    @Test
    override fun testTranslateWindowLayoutInfo_filterRemovesHingeFeatureNotSpanningFullDimension() {
        val fullWidthBounds = Rect(
            WINDOW_BOUNDS.left, WINDOW_BOUNDS.top,
            WINDOW_BOUNDS.right / 2, 2
        )
        val fullHeightBounds = Rect(
            WINDOW_BOUNDS.left, WINDOW_BOUNDS.top, 2,
            WINDOW_BOUNDS.bottom / 2
        )
        val extensionDisplayFeatures = listOf(
            ExtensionFoldingFeature(fullWidthBounds, TYPE_HINGE, STATE_FLAT),
            ExtensionFoldingFeature(fullHeightBounds, TYPE_HINGE, STATE_FLAT)
        )
        val extensionCallbackAdapter = ExtensionAdapter()
        val windowLayoutInfo = ExtensionWindowLayoutInfo(extensionDisplayFeatures)
        val mockActivity = mock<Activity>()
        val actual = extensionCallbackAdapter.translate(
            mockActivity,
            windowLayoutInfo
        )
        assertTrue(
            "Remove hinge feature not spanning full dimension",
            actual.displayFeatures.isEmpty()
        )
    }

    @Test
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
            ExtensionFoldingFeature(fullWidthBounds, TYPE_HINGE, STATE_FLAT),
            ExtensionFoldingFeature(fullHeightBounds, TYPE_HINGE, STATE_FLAT)
        )
        val adapter = ExtensionAdapter()
        val windowLayoutInfo = ExtensionWindowLayoutInfo(extensionDisplayFeatures)
        val mockActivity = mock<Activity>()
        val actual = adapter.translate(mockActivity, windowLayoutInfo)
        assertTrue(
            "Remove fold feature not spanning full dimension",
            actual.displayFeatures.isEmpty()
        )
    }

    @Test
    override fun testTranslateWindowLayoutInfo_filterRemovesUnknownFeature() {
        val bounds = Rect(WINDOW_BOUNDS.left, 0, WINDOW_BOUNDS.right, 0)
        val foldFeature: ExtensionDisplayFeature = ExtensionFoldingFeature(
            bounds,
            0 /* unknown */, STATE_FLAT
        )
        val extensionDisplayFeatures = listOf(foldFeature)
        val windowLayoutInfo = ExtensionWindowLayoutInfo(extensionDisplayFeatures)
        val mockActivity = mock<Activity>()
        val adapter = ExtensionAdapter()
        val actual = adapter.translate(mockActivity, windowLayoutInfo)
        assertTrue(
            "Remove fold feature not spanning full dimension",
            actual.displayFeatures.isEmpty()
        )
    }

    private companion object {
        private val WINDOW_BOUNDS = Rect(0, 0, 50, 100)
    }
}
