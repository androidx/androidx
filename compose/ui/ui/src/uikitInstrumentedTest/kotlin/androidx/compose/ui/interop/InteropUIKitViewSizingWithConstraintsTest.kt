/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.interop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.utils.DpRectZero
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlin.test.Test
import kotlin.test.assertEquals
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIView

class InteropUIKitViewSizingWithConstraintsTest {
    @Test
    fun testFixedSizeConstraints() = runUIKitInstrumentedTestWithInterop { overlay ->
        var rect = DpRectZero()

        setContent {
            UIKitView(
                factory = {
                    UIView().apply {
                        translatesAutoresizingMaskIntoConstraints = false
                        NSLayoutConstraint.activateConstraints(
                            listOf(
                                widthAnchor.constraintEqualToConstant(80.0),
                                heightAnchor.constraintEqualToConstant(100.0)
                            )
                        )
                    }
                },
                modifier = Modifier.onGloballyPositioned { rect = it.boundsInRoot().toDpRect(density) },
                properties = UIKitInteropProperties(placedAsOverlay = overlay)
            )
        }

        assertEquals(DpRect(0.dp, 0.dp, 80.dp, 100.dp), rect)
    }

    @Test
    fun testFixedSizeConstraintsOverrideByCompose() = runUIKitInstrumentedTestWithInterop { overlay ->
        var rect = DpRectZero()

        setContent {
            UIKitView(
                factory = {
                    UIView().apply {
                        translatesAutoresizingMaskIntoConstraints = false
                        NSLayoutConstraint.activateConstraints(
                            listOf(
                                widthAnchor.constraintEqualToConstant(80.0),
                                heightAnchor.constraintEqualToConstant(100.0)
                            )
                        )
                    }
                },
                modifier = Modifier
                    .size(width = 100.dp, height = 200.dp)
                    .onGloballyPositioned { rect = it.boundsInRoot().toDpRect(density) },
                properties = UIKitInteropProperties(placedAsOverlay = overlay)
            )
        }

        assertEquals(DpRect(0.dp, 0.dp, 100.dp, 200.dp), rect)
    }

    @Test
    fun testUnconstrained() = runUIKitInstrumentedTestWithInterop { overlay ->
        var composeRect = DpRectZero()
        var uiKitRect = DpRectZero()

        val showCompose = mutableStateOf(false)

        setContent {
            if (showCompose.value) {
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { composeRect = it.boundsInRoot().toDpRect(density) }
                )
            } else {
                UIKitView(
                    factory = {
                        UIView()
                    },
                    modifier = Modifier
                        .onGloballyPositioned { uiKitRect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        showCompose.value = true

        waitForIdle()

        assertEquals(composeRect, uiKitRect)
        assertEquals(DpRectZero(), uiKitRect)
    }

    @Test
    fun testUnconstrainedFill() = runUIKitInstrumentedTestWithInterop { overlay ->
        var composeRect = DpRectZero()
        var uiKitRect = DpRectZero()

        val showCompose = mutableStateOf(false)

        setContent {
            if (showCompose.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { composeRect = it.boundsInRoot().toDpRect(density) }
                )
            } else {
                UIKitView(
                    factory = {
                        UIView()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { uiKitRect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        showCompose.value = true

        waitForIdle()

        assertEquals(composeRect, uiKitRect)
        assertEquals(DpRect(origin = DpOffset(0.dp, 0.dp), size = screenSize), uiKitRect)
    }

    @Test
    fun testUnconstrainedSize() = runUIKitInstrumentedTestWithInterop { overlay ->
        var rect = DpRectZero()

        setContent {
            UIKitView(
                factory = {
                    UIView()
                },
                modifier = Modifier
                    .size(100.dp, 200.dp)
                    .onGloballyPositioned { rect = it.boundsInRoot().toDpRect(density) },
                properties = UIKitInteropProperties(placedAsOverlay = overlay)
            )
        }

        assertEquals(DpRect(origin = DpOffset(0.dp, 0.dp), size = DpSize(width = 100.dp, height = 200.dp)), rect)
    }

    @Test
    fun testUIKitViewHeightLargerThanScreenHeight() = runUIKitInstrumentedTestWithInterop { overlay ->
        var uiKitRect = DpRectZero()

        setContent {
            Column {
                UIKitView(
                    factory = {
                        UIView().apply {
                            translatesAutoresizingMaskIntoConstraints = false
                            NSLayoutConstraint.activateConstraints(
                                listOf(
                                    widthAnchor.constraintEqualToConstant(100.0),
                                    heightAnchor.constraintEqualToConstant(screenSize.height.value.toDouble() + 100.0)
                                )
                            )
                        }
                    },
                    modifier = Modifier.onGloballyPositioned { uiKitRect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertEquals(DpRect(top = 0.dp, left = 0.dp, right = 100.dp, bottom = screenSize.height), uiKitRect)
    }

    @Test
    fun testUIKitViewWidthLargerThanScreenWidth() = runUIKitInstrumentedTestWithInterop { overlay ->
        var uiKitRect = DpRectZero()

        setContent {
            Column {
                UIKitView(
                    factory = {
                        UIView().apply {
                            translatesAutoresizingMaskIntoConstraints = false
                            NSLayoutConstraint.activateConstraints(
                                listOf(
                                    widthAnchor.constraintEqualToConstant(screenSize.width.value.toDouble() + 100.0),
                                    heightAnchor.constraintEqualToConstant(100.0)
                                )
                            )
                        }
                    },
                    modifier = Modifier.onGloballyPositioned { uiKitRect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertEquals(DpRect(top = 0.dp, left = 0.dp, right = screenSize.width, bottom = 100.dp), uiKitRect)
    }
}