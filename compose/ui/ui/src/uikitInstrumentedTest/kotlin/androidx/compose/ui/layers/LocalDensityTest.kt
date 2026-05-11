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

package androidx.compose.ui.layers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.DpRectZero
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toDpSize
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIButton
import platform.UIKit.UIColor
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIView

class LocalDensityTest {
    @Test
    fun testCustomDensityNotPropagatedToDialog() = runUIKitInstrumentedTest {
        val customDensity = Density(density = 5f)
        var density = -1f

        setContent {
            CompositionLocalProvider(LocalDensity provides customDensity) {
                Dialog(onDismissRequest = {}) {
                    density = LocalDensity.current.density
                }
            }
        }

        assertNotEquals(customDensity.density, density)
        assertEquals(this.density.density, density)
    }

    @Test
    fun testCustomDensityPropagatedToPopup() = runUIKitInstrumentedTest {
        val customDensity = Density(density = 5f)
        var density = -1f

        setContent {
            CompositionLocalProvider(LocalDensity provides customDensity) {
                Popup {
                    density = LocalDensity.current.density
                }
            }
        }

        assertNotEquals(customDensity.density, density)
        assertEquals(this.density.density, density)
    }

    @Test
    fun testCustomDensityPropagatedInDialogContent() = runUIKitInstrumentedTest {
        val outerDensity = Density(density = 5f)
        val innerDensity = Density(density = 10f)
        var actualOuterDensity = -1f
        var actualInnerDensity = -1f

        setContent {
            CompositionLocalProvider(LocalDensity provides outerDensity) {
                Dialog(onDismissRequest = {}) {
                    actualOuterDensity = LocalDensity.current.density
                    CompositionLocalProvider(LocalDensity provides innerDensity) {
                        Button(onClick = {}) {
                            actualInnerDensity = LocalDensity.current.density
                        }
                    }
                }
            }
        }

        assertNotEquals(outerDensity.density, actualOuterDensity)
        assertEquals(this.density.density, actualOuterDensity)
        assertEquals(innerDensity.density, actualInnerDensity)
    }

    @Test
    fun testCustomDensityPropagatedInPopupContent() = runUIKitInstrumentedTest {
        val outerDensity = Density(density = 5f)
        val innerDensity = Density(density = 10f)
        var actualOuterDensity = -1f
        var actualInnerDensity = -1f

        setContent {
            CompositionLocalProvider(LocalDensity provides outerDensity) {
                Popup {
                    actualOuterDensity = LocalDensity.current.density
                    CompositionLocalProvider(LocalDensity provides innerDensity) {
                        Button(onClick = {}) {
                            actualInnerDensity = LocalDensity.current.density
                        }
                    }
                }
            }
        }

        assertNotEquals(outerDensity.density, actualOuterDensity)
        assertEquals(this.density.density, actualOuterDensity)
        assertEquals(innerDensity.density, actualInnerDensity)
    }

    @Test
    fun testTapInteractionsInDialogWithOuterCustomDensity() = runUIKitInstrumentedTest {
        val density = Density(density = 5f)
        val interactionButtonNumber = 8
        var interactionCount = 0

        setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                Dialog(onDismissRequest = {}) {
                    Column {
                        repeat(10) { number ->
                            Button(
                                onClick = { if (number == interactionButtonNumber) { interactionCount++ } },
                                modifier = Modifier.fillMaxWidth().weight(1f).testTag("Button $number")
                            ) {}
                        }
                    }
                }
            }
        }

        findNodeWithTag("Button $interactionButtonNumber").tap()

        waitForIdle()

        assertEquals(1, interactionCount)
    }

    @Test
    fun testTapInteractionsInPopupWithOuterCustomDensity() = runUIKitInstrumentedTest {
        val density = Density(density = 5f)
        val targetButtonIndex = 8
        var tappedButtonIndex = -1

        setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                Popup {
                    Column {
                        repeat(10) { index ->
                            Button(
                                onClick = { tappedButtonIndex = index },
                                modifier = Modifier.fillMaxWidth().weight(1f).testTag("Button $index")
                            ) {}
                        }
                    }
                }
            }
        }

        findNodeWithTag("Button $targetButtonIndex").tap()

        waitForIdle()

        assertEquals(targetButtonIndex, tappedButtonIndex)
    }

    @Test
    fun testTapInteractionsInDialogWithInnerCustomDensity() = runUIKitInstrumentedTest {
        val density = Density(density = 5f)
        val targetButtonIndex = 8
        var tappedButtonIndex = -1

        setContent {
            Dialog(onDismissRequest = {}) {
                CompositionLocalProvider(LocalDensity provides density) {
                    Column {
                        repeat(10) { index ->
                            Button(
                                onClick = { tappedButtonIndex = index },
                                modifier = Modifier.fillMaxWidth().weight(1f).testTag("Button $index")
                            ) {}
                        }
                    }
                }
            }
        }

        findNodeWithTag("Button $targetButtonIndex").tap()

        waitForIdle()

        assertEquals(targetButtonIndex, tappedButtonIndex)
    }

    @Test
    fun testTapInteractionsInPopupWithInnerCustomDensity() = runUIKitInstrumentedTest {
        val density = Density(density = 5f)
        val targetButtonIndex = 8
        var tappedButtonIndex = -1

        setContent {
            Popup {
                CompositionLocalProvider(LocalDensity provides density) {
                    Column {
                        repeat(10) { index ->
                            Button(
                                onClick = { tappedButtonIndex = index },
                                modifier = Modifier.fillMaxWidth().weight(1f).testTag("Button $index")
                            ) {}
                        }
                    }
                }
            }
        }

        findNodeWithTag("Button $targetButtonIndex").tap()

        waitForIdle()

        assertEquals(targetButtonIndex, tappedButtonIndex)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testInteropViewSizeScaledCustomDensity() = runUIKitInstrumentedTest {
        val interopSize = DpSize(20.dp, 20.dp)
        var densityScale by mutableFloatStateOf(1f)
        val interopView = UIView()

        setContent {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(densityScale * density)) {
                    UIKitView(
                        factory = { interopView },
                        modifier = Modifier.size(interopSize)
                    )
                }
            }
        }

        assertEquals(
            expected = interopSize,
            actual = interopView.frame.useContents { size.toDpSize() }
        )

        densityScale = 2f
        waitForIdle()

        assertEquals(
            expected = (interopSize * densityScale),
            actual = interopView.frame.useContents { size.toDpSize() }
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun testInteropViewPositionForCustomDensity() = runUIKitInstrumentedTest {
        val interopHeight = 100.dp
        val padding = 10.dp
        var densityScale by mutableFloatStateOf(1f)
        var interopViewRect = DpRectZero()
        val interopView = UIButton().also {
            it.setTitle("TAP", forState = UIControlStateNormal)
            it.backgroundColor = UIColor.redColor
        }

        setContent {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopStart
            ) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(densityScale * density.density)) {
                    val currentDensity = LocalDensity.current
                    UIKitView(
                        factory = { interopView },
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxWidth()
                            .height(interopHeight)
                            .onGloballyPositioned { interopViewRect = it.boundsInWindow().toDpRect(currentDensity) }
                    )
                }
            }
        }

        assertEquals(
            expected = DpSize(
                width = screenSize.width - padding * 2 * densityScale,
                height = interopHeight * densityScale
            ),
            actual = interopView.frame.useContents { size.toDpSize() },
        )

        assertEquals(
            expected = DpRect(
                origin = DpOffset(padding, padding),
                size = DpSize(
                    width = (screenSize.width - padding.times(2f)).times(1f / densityScale),
                    height = interopHeight
                )
            ),
            actual = interopViewRect,
        )

        densityScale = 2f
        waitForIdle()

        assertEquals(
            expected = DpSize(
                width = screenSize.width - padding * 2 * densityScale,
                height = interopHeight * densityScale
            ),
            actual = interopView.frame.useContents { size.toDpSize() },
        )

        assertEquals(
            expected = DpRect(
                origin = DpOffset(padding, padding),
                size = DpSize(
                    width = (screenSize.width - padding * 2 * densityScale).times(1f / densityScale),
                    height = interopHeight
                )
            ),
            actual = interopViewRect
        )
    }
}