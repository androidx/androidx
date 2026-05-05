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

package androidx.compose.ui.interop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.utils.DpRectZero
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.toDpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.size
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIColor
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
class UIKitInteropSizingBasicTest {
    @Test
    fun testFixedSizeUIKitConstraintsAndNoComposeSize() = runUIKitInstrumentedTestWithInterop { overlay ->
        var rect = DpRectZero()

        setContent {
            UIKitView(
                factory = {
                    UIView().apply {
                        backgroundColor = UIColor.redColor
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
                    .onGloballyPositioned { rect = it.boundsInRoot().toDpRect(density) },
                properties = UIKitInteropProperties(placedAsOverlay = overlay)
            )
        }

        assertEquals(DpRect(0.dp, 0.dp, 80.dp, 100.dp), rect)
    }

    @Test
    fun testFixedSizeUIKitConstraintsAndFixedComposeSizeOverrides() =
        runUIKitInstrumentedTestWithInterop { overlay ->
            var rect = DpRectZero()
            val view = UIView().apply {
                translatesAutoresizingMaskIntoConstraints = false
                backgroundColor = UIColor.redColor
                NSLayoutConstraint.activateConstraints(
                    listOf(
                        widthAnchor.constraintEqualToConstant(80.0),
                        heightAnchor.constraintEqualToConstant(100.0)
                    )
                )
            }

            setContent {
                UIKitView(
                    factory = { view },
                    modifier = Modifier
                        .background(Color.Blue)
                        .size(width = 100.dp, height = 200.dp)
                        .background(Color.Green)
                        .onGloballyPositioned { rect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }

            assertEquals(DpSize(100.dp, 200.dp), rect.size)
            assertEquals(DpSize(100.dp, 200.dp), view.frame.useContents { size.toDpSize() })
        }

    @Test
    fun testNoUIKitConstraintsAndNoComposeSize() = runUIKitInstrumentedTestWithInterop { overlay ->
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
                        UIView().also {
                            it.backgroundColor = UIColor.blueColor
                        }
                    },
                    modifier = Modifier
                        .background(Color.Blue)
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
    fun testNoUIKitConstraintsAndComposeFillMaxSize() = runUIKitInstrumentedTestWithInterop { overlay ->
        var composeRect = DpRectZero()
        var uiKitRect = DpRectZero()
        val showCompose = mutableStateOf(false)
        val uiKitView = UIView()

        setContent {
            if (showCompose.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { composeRect = it.boundsInRoot().toDpRect(density) }
                )
            } else {
                UIKitView(
                    factory = { uiKitView },
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
        assertEquals(composeRect.size, uiKitView.frame.useContents { size.toDpSize() })
    }

    @Test
    fun testNoUIKitConstraintsAndComposeSize() = runUIKitInstrumentedTestWithInterop { overlay ->
        var rect = DpRectZero()
        val view = UIView()

        setContent {
            UIKitView(
                factory = { view },
                modifier = Modifier
                    .size(100.dp, 200.dp)
                    .onGloballyPositioned { rect = it.boundsInRoot().toDpRect(density) },
                properties = UIKitInteropProperties(placedAsOverlay = overlay)
            )
        }

        assertEquals(DpSize(width = 100.dp, height = 200.dp), rect.size)
        assertEquals(DpSize(width = 100.dp, height = 200.dp), view.frame.useContents { size.toDpSize() })
    }

    @Test
    fun testUIKitViewHeightLargerThanScreenHeight() =
        runUIKitInstrumentedTestWithInterop { overlay ->
            var rect = DpRectZero()
            val view = UIView().apply {
                translatesAutoresizingMaskIntoConstraints = false
                NSLayoutConstraint.activateConstraints(
                    listOf(
                        widthAnchor.constraintEqualToConstant(100.0),
                        heightAnchor.constraintEqualToConstant(screenSize.height.value.toDouble() + 100.0)
                    )
                )
            }

            setContent {
                Column {
                    UIKitView(
                        factory = { view },
                        modifier = Modifier
                            .onGloballyPositioned { rect = it.boundsInRoot().toDpRect(density) },
                        properties = UIKitInteropProperties(placedAsOverlay = overlay)
                    )
                }
            }

            assertEquals(DpSize(100.dp, screenSize.height), rect.size)
            assertEquals(DpSize(100.dp, screenSize.height), view.frame.useContents { size.toDpSize() })
        }

    @Test
    fun testUIKitViewWidthLargerThanScreenWidth() = runUIKitInstrumentedTestWithInterop { overlay ->
        var rect = DpRectZero()
        val view = UIView().apply {
            translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activateConstraints(
                listOf(
                    widthAnchor.constraintEqualToConstant(screenSize.width.value.toDouble() + 100.0),
                    heightAnchor.constraintEqualToConstant(100.0)
                )
            )
        }

        setContent {
            Column {
                UIKitView(
                    factory = { view },
                    modifier = Modifier
                        .onGloballyPositioned { rect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertEquals(DpSize(screenSize.width, 100.dp), rect.size)
        assertEquals(DpSize(screenSize.width, 100.dp), view.frame.useContents { size.toDpSize() })
    }

    @Test
    fun testUIKitViewSizeLargerThanScreenSize() = runUIKitInstrumentedTestWithInterop { overlay ->
        var rect = DpRectZero()
        val view = UIView().apply {
            translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activateConstraints(
                listOf(
                    widthAnchor.constraintEqualToConstant(screenSize.width.value.toDouble() + 110.0),
                    heightAnchor.constraintEqualToConstant(screenSize.height.value.toDouble() + 120.0)
                )
            )
        }

        setContent {
            Column {
                UIKitView(
                    factory = { view },
                    modifier = Modifier
                        .onGloballyPositioned { rect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertEquals(screenSize, rect.size)
        assertEquals(screenSize, view.frame.useContents { size.toDpSize() })
    }
}