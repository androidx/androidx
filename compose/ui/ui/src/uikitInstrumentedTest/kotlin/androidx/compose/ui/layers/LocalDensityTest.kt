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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.uikit.density
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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
        assertEquals(hostingViewController.view.density.density, density)
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
        assertEquals(hostingViewController.view.density.density, density)
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
        assertEquals(hostingViewController.view.density.density, actualOuterDensity)
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
        assertEquals(hostingViewController.view.density.density, actualOuterDensity)
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
}