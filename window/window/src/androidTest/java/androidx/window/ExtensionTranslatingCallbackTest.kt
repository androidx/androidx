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
import androidx.window.ExtensionInterfaceCompat.ExtensionCallbackInterface
import androidx.window.FoldingFeature.State.Companion.FLAT
import androidx.window.FoldingFeature.Type.Companion.FOLD
import androidx.window.extensions.ExtensionDisplayFeature
import androidx.window.extensions.ExtensionFoldingFeature
import androidx.window.extensions.ExtensionFoldingFeature.STATE_FLAT
import androidx.window.extensions.ExtensionFoldingFeature.TYPE_HINGE
import androidx.window.extensions.ExtensionWindowLayoutInfo
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

public class ExtensionTranslatingCallbackTest {

    private lateinit var extensionAdapter: ExtensionAdapter

    @Before
    public fun setUp() {
        val windowMetricsCalculator = TestWindowMetricsCalculator()
        windowMetricsCalculator.setCurrentBounds(WINDOW_BOUNDS)
        extensionAdapter = ExtensionAdapter(windowMetricsCalculator)
    }

    @Test
    public fun testOnWindowLayoutChange_validFeature() {
        val mockActivity = mock<Activity>()
        val bounds = Rect(WINDOW_BOUNDS.left, 0, WINDOW_BOUNDS.right, 0)
        val foldFeature: ExtensionDisplayFeature = ExtensionFoldingFeature(
            bounds,
            ExtensionFoldingFeature.TYPE_FOLD, STATE_FLAT
        )
        val extensionDisplayFeatures = listOf(foldFeature)
        val windowLayoutInfo = ExtensionWindowLayoutInfo(extensionDisplayFeatures)
        val expectedFeatures = listOf(FoldingFeature(Bounds(foldFeature.bounds), FOLD, FLAT))
        val expected = WindowLayoutInfo(expectedFeatures)
        val mockCallback = mock<ExtensionCallbackInterface>()
        val extensionTranslatingCallback =
            ExtensionTranslatingCallback(mockCallback, extensionAdapter)
        extensionTranslatingCallback.onWindowLayoutChanged(mockActivity, windowLayoutInfo)
        val captor = argumentCaptor<WindowLayoutInfo>()
        verify(mockCallback)
            .onWindowLayoutChanged(eq(mockActivity), captor.capture())
        assertEquals(expected, captor.firstValue)
    }

    @Test
    public fun testOnWindowLayoutChange_filterRemovesHingeFeatureNotSpanningFullDimension() {
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
        val mockCallback = mock<ExtensionCallbackInterface>()
        val extensionTranslatingCallback =
            ExtensionTranslatingCallback(mockCallback, extensionAdapter)
        val windowLayoutInfo = ExtensionWindowLayoutInfo(extensionDisplayFeatures)
        val mockActivity = mock<Activity>()
        extensionTranslatingCallback.onWindowLayoutChanged(mockActivity, windowLayoutInfo)
        verify(mockCallback).onWindowLayoutChanged(
            eq(mockActivity),
            argThat { layoutInfo -> layoutInfo.displayFeatures.isEmpty() }
        )
    }

    @Test
    public fun testOnWindowLayoutChange_filterRemovesFoldFeatureNotSpanningFullDimension() {
        val fullWidthBounds = Rect(
            WINDOW_BOUNDS.left, WINDOW_BOUNDS.top,
            WINDOW_BOUNDS.right / 2, WINDOW_BOUNDS.top
        )
        val fullHeightBounds = Rect(
            WINDOW_BOUNDS.left, WINDOW_BOUNDS.top, WINDOW_BOUNDS.left,
            WINDOW_BOUNDS.bottom / 2
        )
        val extensionDisplayFeatures = listOf(
            ExtensionFoldingFeature(
                fullWidthBounds,
                TYPE_HINGE, STATE_FLAT
            ),
            ExtensionFoldingFeature(
                fullHeightBounds,
                TYPE_HINGE, STATE_FLAT
            )
        )
        val mockCallback = mock<ExtensionCallbackInterface>()
        val extensionTranslatingCallback =
            ExtensionTranslatingCallback(mockCallback, extensionAdapter)
        val windowLayoutInfo = ExtensionWindowLayoutInfo(extensionDisplayFeatures)
        val mockActivity = mock<Activity>()
        extensionTranslatingCallback.onWindowLayoutChanged(mockActivity, windowLayoutInfo)
        verify(mockCallback).onWindowLayoutChanged(
            eq(mockActivity),
            argThat { layoutInfo -> layoutInfo.displayFeatures.isEmpty() }
        )
    }

    internal companion object {
        private val WINDOW_BOUNDS = Rect(0, 0, 50, 100)
    }
}
