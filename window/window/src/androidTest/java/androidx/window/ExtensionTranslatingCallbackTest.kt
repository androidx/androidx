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
import androidx.window.extensions.ExtensionDisplayFeature
import androidx.window.extensions.ExtensionFoldingFeature
import androidx.window.extensions.ExtensionWindowLayoutInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

public class ExtensionTranslatingCallbackTest {

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
    public fun testOnWindowLayoutChange_validFeature() {
        val mockActivity = mock(
            Activity::class.java
        )
        val bounds = Rect(WINDOW_BOUNDS.left, 0, WINDOW_BOUNDS.right, 0)
        val foldFeature: ExtensionDisplayFeature = ExtensionFoldingFeature(
            bounds,
            ExtensionFoldingFeature.TYPE_FOLD, ExtensionFoldingFeature.STATE_FLAT
        )
        val extensionDisplayFeatures = listOf(foldFeature)
        val windowLayoutInfo = ExtensionWindowLayoutInfo(extensionDisplayFeatures)
        val expectedFeatures = listOf(
            FoldingFeature(
                foldFeature.bounds, FoldingFeature.TYPE_FOLD,
                FoldingFeature.STATE_FLAT
            )
        )
        val expected = WindowLayoutInfo(expectedFeatures)
        val mockCallback = mock(
            ExtensionCallbackInterface::class.java
        )
        val extensionTranslatingCallback =
            ExtensionTranslatingCallback(mockCallback, ExtensionAdapter())
        extensionTranslatingCallback.onWindowLayoutChanged(mockActivity, windowLayoutInfo)
        val captor = ArgumentCaptor.forClass(
            WindowLayoutInfo::class.java
        )
        verify(mockCallback)
            .onWindowLayoutChanged(ArgumentMatchers.eq(mockActivity), captor.capture())
        assertEquals(expected, captor.value)
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
            ExtensionFoldingFeature(
                fullWidthBounds,
                ExtensionFoldingFeature.TYPE_HINGE, ExtensionFoldingFeature.STATE_FLAT
            ),
            ExtensionFoldingFeature(
                fullHeightBounds,
                ExtensionFoldingFeature.TYPE_HINGE, ExtensionFoldingFeature.STATE_FLAT
            )
        )
        val mockCallback = mock(
            ExtensionCallbackInterface::class.java
        )
        val extensionTranslatingCallback =
            ExtensionTranslatingCallback(mockCallback, ExtensionAdapter())
        val windowLayoutInfo = ExtensionWindowLayoutInfo(extensionDisplayFeatures)
        val mockActivity = mock(
            Activity::class.java
        )
        extensionTranslatingCallback.onWindowLayoutChanged(mockActivity, windowLayoutInfo)
        verify(mockCallback).onWindowLayoutChanged(
            ArgumentMatchers.eq(mockActivity),
            ArgumentMatchers.argThat { layoutInfo -> layoutInfo.displayFeatures.isEmpty() }
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
                ExtensionFoldingFeature.TYPE_HINGE, ExtensionFoldingFeature.STATE_FLAT
            ),
            ExtensionFoldingFeature(
                fullHeightBounds,
                ExtensionFoldingFeature.TYPE_HINGE, ExtensionFoldingFeature.STATE_FLAT
            )
        )
        val mockCallback = mock(
            ExtensionCallbackInterface::class.java
        )
        val extensionTranslatingCallback =
            ExtensionTranslatingCallback(mockCallback, ExtensionAdapter())
        val windowLayoutInfo = ExtensionWindowLayoutInfo(extensionDisplayFeatures)
        val mockActivity = mock(
            Activity::class.java
        )
        extensionTranslatingCallback.onWindowLayoutChanged(mockActivity, windowLayoutInfo)
        verify(mockCallback).onWindowLayoutChanged(
            ArgumentMatchers.eq(mockActivity),
            ArgumentMatchers.argThat { layoutInfo -> layoutInfo.displayFeatures.isEmpty() }
        )
    }

    internal companion object {
        private val WINDOW_BOUNDS = Rect(0, 0, 50, 100)
    }
}
