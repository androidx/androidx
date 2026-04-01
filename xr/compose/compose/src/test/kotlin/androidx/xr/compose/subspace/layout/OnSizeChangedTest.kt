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

package androidx.xr.compose.subspace.layout

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.unit.IntVolumeSize
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [onSizeChanged] SubspaceModifier. */
@RunWith(AndroidJUnit4::class)
class OnSizeChangedModifierTest {
    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun onSizeChanged_initialComposition_providesCorrectSize() {
        var panelSize: IntVolumeSize? = null
        val expectedSize =
            with(composeTestRule.density) {
                IntVolumeSize(100.dp.roundToPx(), 150.dp.roundToPx(), 50.dp.roundToPx())
            }

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.width(100.dp).height(150.dp).depth(50.dp).onSizeChanged {
                        newSize ->
                        panelSize = newSize
                    }
                ) {}
            }
        }

        assertNotNull(panelSize)
        assertThat(panelSize).isEqualTo(expectedSize)
    }

    @Test
    fun onSizeChanged_whenSizeStateChanges_callbackIsInvokedWithNewSize() {
        var currentSize by mutableStateOf(50.dp)
        var panelSize: IntVolumeSize? = null
        val expectedInitialSize = with(composeTestRule.density) { 50.dp.roundToPx() }
        val expectedFinalSize = with(composeTestRule.density) { 100.dp.roundToPx() }

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.size(currentSize).onSizeChanged { newSize ->
                        panelSize = newSize
                    }
                ) {}
            }
        }

        assertNotNull(panelSize)
        assertThat(panelSize)
            .isEqualTo(IntVolumeSize(expectedInitialSize, expectedInitialSize, expectedInitialSize))

        currentSize = 100.dp
        composeTestRule.waitForIdle()

        assertNotNull(panelSize)
        assertThat(panelSize)
            .isEqualTo(IntVolumeSize(expectedFinalSize, expectedFinalSize, expectedFinalSize))
    }

    @Test
    fun onSizeChanged_whenRecomposedWithSameSize_callbackIsNotInvokedAgain() {
        var currentSize by mutableStateOf(50.dp)
        var callbackCount = 0

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.size(currentSize).onSizeChanged { callbackCount++ }
                ) {}
            }
        }

        assertThat(callbackCount).isEqualTo(1)

        currentSize = 50.dp
        composeTestRule.waitForIdle()

        assertThat(callbackCount).isEqualTo(1)
    }

    @Test
    fun onSizeChanged_initialComposition_callbackIsInvokedOnce() {
        var callbackCount = 0

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp).onSizeChanged { callbackCount++ }) {}
            }
        }

        assertThat(callbackCount).isEqualTo(1)
    }

    @Test
    fun onSizeChanged_onRecompositionWithoutSizeChange_doesNotInvokeCallback() {
        val fixedSize = 50.dp
        var unrelatedState by mutableStateOf(false)
        var callbackInvocationCount = 0

        composeTestRule.setContent {
            val stateRead = unrelatedState

            val callback: (IntVolumeSize) -> Unit = remember { { callbackInvocationCount++ } }

            Subspace {
                SpatialPanel(modifier = SubspaceModifier.size(fixedSize).onSizeChanged(callback)) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(callbackInvocationCount).isEqualTo(1)

        unrelatedState = true
        composeTestRule.waitForIdle()

        assertThat(callbackInvocationCount).isEqualTo(1)
    }

    @Test
    fun onSizeChanged_whenSizeChanges_callbackIsInvokedWithNewSize() {
        var currentSize by mutableStateOf(50.dp)
        var panelSize: IntVolumeSize? = null

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.size(currentSize).onSizeChanged { size ->
                            panelSize = size
                        }
                ) {
                    Text(text = "Panel")
                }
            }
        }

        val expectedInitialSize = with(composeTestRule.density) { 50.dp.roundToPx() }
        assertThat(panelSize)
            .isEqualTo(IntVolumeSize(expectedInitialSize, expectedInitialSize, expectedInitialSize))

        currentSize = 100.dp
        composeTestRule.waitForIdle()

        val expectedNewSize = with(composeTestRule.density) { 100.dp.roundToPx() }
        assertThat(panelSize)
            .isEqualTo(IntVolumeSize(expectedNewSize, expectedNewSize, expectedNewSize))
    }

    @Test
    fun onSizeChanged_whenCallbackChanges_callbackIsInvokedWithCurrentSize() {
        var currentSize by mutableStateOf(50.dp)
        var panelSize: IntVolumeSize? = null
        val expectedInitialSize = with(composeTestRule.density) { 50.dp.roundToPx() }

        var currentCallback by
            mutableStateOf<(IntVolumeSize) -> Unit>({ newSize -> panelSize = newSize })
        var callbackInvocationCount = 0

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.size(currentSize)
                        .onSizeChanged(onSizeChanged = currentCallback)
                ) {}
            }
        }

        assertNotNull(panelSize)
        assertThat(panelSize)
            .isEqualTo(IntVolumeSize(expectedInitialSize, expectedInitialSize, expectedInitialSize))

        // Update the callback to a new instance.
        currentCallback = { newSize ->
            panelSize = newSize
            callbackInvocationCount++
        }
        composeTestRule.waitForIdle()

        assertNotNull(panelSize)
        assertThat(panelSize)
            .isEqualTo(IntVolumeSize(expectedInitialSize, expectedInitialSize, expectedInitialSize))
        assertThat(callbackInvocationCount).isEqualTo(1)
    }

    @Test
    fun onSizeChanged_whenModifierRemoved_doesNotInvokeCallback() {
        var hasModifier by mutableStateOf(true)
        var invocationCount = 0

        composeTestRule.setContent {
            Subspace {
                val callback: (IntVolumeSize) -> Unit = remember { { invocationCount++ } }

                val modifier =
                    if (hasModifier) {
                        SubspaceModifier.onSizeChanged(callback)
                    } else {
                        SubspaceModifier
                    }
                SpatialPanel(modifier = modifier) { Text(text = "Panel") }
            }
        }

        composeTestRule.waitForIdle()
        val initialFires = invocationCount
        assertThat(initialFires).isGreaterThan(0)

        hasModifier = false
        composeTestRule.waitForIdle()

        // The dying modifier did not fire an update.
        assertThat(invocationCount).isEqualTo(initialFires)
    }

    @Test
    fun onSizeChanged_doesNotDispatchPhantomSizeOnInitialComposition() {
        val capturedSizes = mutableListOf<IntVolumeSize>()

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.size(50.dp).onSizeChanged { size ->
                            capturedSizes.add(size)
                        }
                ) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule.waitForIdle()

        assertThat(capturedSizes.size).isEqualTo(1)
        assertThat(capturedSizes[0].width).isGreaterThan(0)
    }
}
