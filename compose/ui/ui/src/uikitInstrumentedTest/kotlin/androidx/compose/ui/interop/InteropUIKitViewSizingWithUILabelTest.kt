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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.utils.DpRectZero
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.asDpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.size
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toDpSize
import androidx.compose.ui.unit.width
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UILabel

class InteropUIKitViewSizingWithUILabelTest {
    private val SHORT_TEXT: String = "TEXT"
    private val LONG_TEXT: String = List(100) { "TEXT" }.joinToString(" ")

    @Test
    @OptIn(ExperimentalForeignApi::class)
    fun testUILabelFrameMatchesInteropBoundsShortTextSingleLine() = runUIKitInstrumentedTestWithInterop { overlay ->
        var interopRect = DpRectZero()
        var uiLabelRect: () -> DpRect = { DpRectZero() }

        setContent {
            UIKitView(
                factory = {
                    UILabel().apply {
                        numberOfLines = 1
                        text = SHORT_TEXT
                        uiLabelRect = { frame.useContents { asDpRect() } }
                    }
                },
                modifier = Modifier.onGloballyPositioned { interopRect = it.boundsInRoot().toDpRect(density) },
                properties = UIKitInteropProperties(placedAsOverlay = overlay)
            )
        }

        assertEquals(interopRect, uiLabelRect())
    }

    @Test
    @OptIn(ExperimentalForeignApi::class)
    fun testUILabelFrameMatchesInteropBoundsShortTextMultiLine() = runUIKitInstrumentedTestWithInterop { overlay ->
        var interopRect = DpRectZero()
        var uiLabelRect: () -> DpRect = { DpRectZero() }

        setContent {
            UIKitView(
                factory = {
                    UILabel().apply {
                        numberOfLines = 0
                        text = SHORT_TEXT
                        uiLabelRect = { frame.useContents { asDpRect() } }
                    }
                },
                modifier = Modifier.onGloballyPositioned { interopRect = it.boundsInRoot().toDpRect(density) },
                properties = UIKitInteropProperties(placedAsOverlay = overlay)
            )
        }

        assertEquals(interopRect, uiLabelRect())
    }

    @Test
    @OptIn(ExperimentalForeignApi::class)
    fun testUILabelFrameMatchesInteropBoundsLongTextSingleLine() = runUIKitInstrumentedTestWithInterop { overlay ->
        var interopRect = DpRectZero()
        var uiLabelRect: () -> DpRect = { DpRectZero() }

        setContent {
            UIKitView(
                factory = {
                    UILabel().apply {
                        numberOfLines = 1
                        text = LONG_TEXT
                        uiLabelRect = { frame.useContents { asDpRect() } }
                    }
                },
                modifier = Modifier.onGloballyPositioned { interopRect = it.boundsInRoot().toDpRect(density) },
                properties = UIKitInteropProperties(placedAsOverlay = overlay)
            )
        }

        assertEquals(interopRect, uiLabelRect())
    }

    @Test
    @OptIn(ExperimentalForeignApi::class)
    fun testUILabelFrameMatchesInteropBoundsLongTextMultiLine() = runUIKitInstrumentedTestWithInterop { overlay ->
        var interopRect = DpRectZero()
        var uiLabelRect: () -> DpRect = { DpRectZero() }

        setContent {
            UIKitView(
                factory = {
                    UILabel().apply {
                        numberOfLines = 0
                        text = LONG_TEXT
                        uiLabelRect = { frame.useContents { asDpRect() } }
                    }
                },
                modifier = Modifier.onGloballyPositioned { interopRect = it.boundsInRoot().toDpRect(density) },
                properties = UIKitInteropProperties(placedAsOverlay = overlay)
            )
        }

        assertEquals(interopRect, uiLabelRect())
    }

    @Test
    fun testUILabelShortText() = runUIKitInstrumentedTestWithInterop { overlay ->
        var singleLineLabel = DpRectZero()
        var multiLineLabel = DpRectZero()

        setContent {
            Column {
                UIKitView(
                    factory = {
                        UILabel().apply {
                            numberOfLines = 1
                            text = SHORT_TEXT
                        }
                    },
                    modifier = Modifier.onGloballyPositioned { singleLineLabel = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
                UIKitView(
                    factory = {
                        UILabel().apply {
                            numberOfLines = 0
                            text = SHORT_TEXT
                        }
                    },
                    modifier = Modifier.onGloballyPositioned { multiLineLabel = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertTrue(multiLineLabel.width > 0.dp && multiLineLabel.width < screenSize.width)
        assertTrue(multiLineLabel.height > 0.dp)
        assertEquals(singleLineLabel.height, multiLineLabel.height)
        assertEquals(singleLineLabel.width, multiLineLabel.width)
        assertEquals(singleLineLabel.left, multiLineLabel.left)
        assertEquals(singleLineLabel.bottom, multiLineLabel.top)
    }

    @Test
    fun testUILabelLongText() = runUIKitInstrumentedTestWithInterop { overlay ->
        var singleLineText = DpRectZero()
        var multiLineText = DpRectZero()

        setContent {
            Column {
                UIKitView(
                    factory = {
                        UILabel().apply {
                            numberOfLines = 1
                            text = LONG_TEXT
                        }
                    },
                    modifier = Modifier.onGloballyPositioned { singleLineText = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
                UIKitView(
                    factory = {
                        UILabel().apply {
                            numberOfLines = 0
                            text = LONG_TEXT
                        }
                    },
                    modifier = Modifier.onGloballyPositioned { multiLineText = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertEquals(screenSize.width, singleLineText.width)
        assertTrue(multiLineText.width <= singleLineText.width)
        assertTrue(singleLineText.height < multiLineText.height)
    }

    @Test
    fun testUILabelSingleLineShortTextFixedWidth() = runUIKitInstrumentedTestWithInterop { overlay ->
        var referenceViewRect = DpRectZero()
        var fixedWidthViewRect = DpRectZero()

        setContent {
            Column {
                UIKitView(
                    factory = {
                        UILabel().apply {
                            numberOfLines = 1
                            text = SHORT_TEXT
                        }
                    },
                    modifier = Modifier.onGloballyPositioned { referenceViewRect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
                UIKitView(
                    factory = {
                        UILabel().apply {
                            numberOfLines = 1
                            text = SHORT_TEXT
                        }
                    },
                    modifier = Modifier
                        .width(200.dp)
                        .onGloballyPositioned { fixedWidthViewRect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertEquals(200.dp, fixedWidthViewRect.width)
        assertEquals(referenceViewRect.height, fixedWidthViewRect.height)
    }

    @Test
    fun testUILabelSingleLineShortTextFixedHeight() = runUIKitInstrumentedTestWithInterop { overlay ->
        var referenceViewRect = DpRectZero()
        var fixedWidthViewRect = DpRectZero()

        setContent {
            Column {
                UIKitView(
                    factory = {
                        UILabel().apply {
                            numberOfLines = 1
                            text = SHORT_TEXT
                        }
                    },
                    modifier = Modifier.onGloballyPositioned { referenceViewRect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
                UIKitView(
                    factory = {
                        UILabel().apply {
                            numberOfLines = 1
                            text = SHORT_TEXT
                        }
                    },
                    modifier = Modifier
                        .height(200.dp)
                        .onGloballyPositioned { fixedWidthViewRect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertEquals(200.dp, fixedWidthViewRect.height)
        assertEquals(referenceViewRect.width, fixedWidthViewRect.width)
    }

    @Test
    fun testUILabelSingleLineShortTextFixedSize() = runUIKitInstrumentedTestWithInterop { overlay ->
        var referenceViewRect = DpRectZero()
        var fixedSizeViewRect = DpRectZero()

        setContent {
            Column {
                UIKitView(
                    factory = {
                        UILabel().apply {
                            numberOfLines = 1
                            text = SHORT_TEXT
                        }
                    },
                    modifier = Modifier.onGloballyPositioned { referenceViewRect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
                UIKitView(
                    factory = {
                        UILabel().apply {
                            numberOfLines = 1
                            text = SHORT_TEXT
                        }
                    },
                    modifier = Modifier
                        .size(width = 200.dp, height = 400.dp)
                        .onGloballyPositioned { fixedSizeViewRect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertEquals(DpSize(width = 200.dp, height = 400.dp), fixedSizeViewRect.size)
        assertNotEquals(referenceViewRect.size, fixedSizeViewRect.size)
    }

    @Test
    fun testUILabelMultiLineLongTextFixedWidth() = runUIKitInstrumentedTestWithInterop { overlay ->
        var unboundedTextRect = DpRectZero()
        var boundedWidthTextRect = DpRectZero()

        setContent {
            Column {
                UIKitView(
                    factory = {
                        UILabel().apply {
                            numberOfLines = 0
                            text = LONG_TEXT
                        }
                    },
                    modifier = Modifier.onGloballyPositioned { unboundedTextRect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
                UIKitView(
                    factory = {
                        UILabel().apply {
                            numberOfLines = 0
                            text = LONG_TEXT
                        }
                    },
                    modifier = Modifier
                        .width(width = 200.dp)
                        .onGloballyPositioned { boundedWidthTextRect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertEquals(200.dp, boundedWidthTextRect.width)
        assertTrue(boundedWidthTextRect.width <= unboundedTextRect.width)
        assertTrue(boundedWidthTextRect.height > unboundedTextRect.height)
    }

    @Test
    fun testUILabelMultiLineLongTextFixedHeight() = runUIKitInstrumentedTestWithInterop { overlay ->
        var unboundedTextSize = DpSize.Zero
        var boundedHeightTextSize = DpSize.Zero

        setContent {
            Column {
                UIKitView(
                    factory = {
                        UILabel().apply {
                            numberOfLines = 0
                            text = LONG_TEXT
                        }
                    },
                    modifier = Modifier.onGloballyPositioned {
                        unboundedTextSize = it.boundsInRoot().size.toDpSize(density)
                    },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
                UIKitView(
                    factory = {
                        UILabel().apply {
                            numberOfLines = 0
                            text = LONG_TEXT
                        }
                    },
                    modifier = Modifier
                        .height(100.dp)
                        .onGloballyPositioned {
                            boundedHeightTextSize = it.boundsInRoot().size.toDpSize(density)
                        },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertEquals(100.dp, boundedHeightTextSize.height)
        assertEquals(unboundedTextSize.width, boundedHeightTextSize.width)
        assertTrue(boundedHeightTextSize.height < unboundedTextSize.height)
    }

    @Test
    fun testUILabelAndTextInRow() = runUIKitInstrumentedTestWithInterop { overlay ->
        var composeRect = DpRectZero()
        var uiKitRect = DpRectZero()

        setContent {
            Row {
                Text(
                    SHORT_TEXT,
                    modifier = Modifier.onGloballyPositioned { composeRect = it.boundsInRoot().toDpRect(density) }
                )
                UIKitView(
                    factory = {
                        UILabel().apply {
                            text = LONG_TEXT
                        }
                    },
                    modifier = Modifier.onGloballyPositioned { uiKitRect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertEquals(composeRect.right, uiKitRect.left)
        assertEquals(composeRect.top, uiKitRect.top)
        assertEquals(uiKitRect.right, screenSize.width)
        assertTrue(uiKitRect.height > 0.dp)
    }

    @Test
    fun testUILabelAndTextInColumn() = runUIKitInstrumentedTestWithInterop { overlay ->
        var composeRect = DpRectZero()
        var uiKitRect = DpRectZero()

        setContent {
            Column {
                Text(
                    SHORT_TEXT,
                    modifier = Modifier.onGloballyPositioned { composeRect = it.boundsInRoot().toDpRect(density) }
                )
                UIKitView(
                    factory = {
                        UILabel().apply {
                            text = LONG_TEXT
                        }
                    },
                    modifier = Modifier.onGloballyPositioned { uiKitRect = it.boundsInRoot().toDpRect(density) },
                    properties = UIKitInteropProperties(placedAsOverlay = overlay)
                )
            }
        }

        assertEquals(composeRect.left, uiKitRect.left)
        assertEquals(composeRect.bottom, uiKitRect.top)
        assertEquals(uiKitRect.right, screenSize.width)
        assertTrue(uiKitRect.bottom > composeRect.bottom)
    }
}