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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.modifiers.DimensionConstraintsModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.OffsetModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "mdpi")
class LayoutDimensionBehaviorTest {

    @get:Rule val playerRule = RcPlayerTestRule()

    @Test
    fun requiredMinimumCanOverrideTheIncomingParentConstraint() {
        assertMeasuredWidth(80) { wire ->
            DimensionConstraintsModifierOperation.apply(
                wire,
                DimensionConstraintsModifierOperation.REQUIRED_HORIZONTAL_CONSTRAINTS.toByte(),
                80f,
                -1f,
            )
        }
    }

    @Test
    fun requiredVerticalMinimumCanOverrideTheIncomingParentConstraint() {
        assertMeasuredHeight(80) { wire ->
            DimensionConstraintsModifierOperation.apply(
                wire,
                DimensionConstraintsModifierOperation.REQUIRED_VERTICAL_CONSTRAINTS.toByte(),
                80f,
                -1f,
            )
        }
    }

    @Test
    fun fillParentMaxWidthUsesTheAvailableWidth() {
        assertMeasuredWidth(100) { wire ->
            WidthModifierOperation.apply(
                wire,
                DimensionModifierOperation.Type.FILL_PARENT_MAX_WIDTH.ordinal,
                1f,
            )
        }
    }

    @Test
    fun fillMaxWidthPreservesItsFraction() {
        assertMeasuredWidth(50) { wire ->
            WidthModifierOperation.apply(wire, DimensionModifierOperation.Type.FILL.ordinal, 0.5f)
        }
    }

    @Test
    fun fillParentMaxHeightUsesTheAvailableHeight() {
        assertMeasuredHeight(100) { wire ->
            HeightModifierOperation.apply(
                wire,
                DimensionModifierOperation.Type.FILL_PARENT_MAX_HEIGHT.ordinal,
                1f,
            )
        }
    }

    @Test
    fun fillMaxHeightPreservesItsFraction() {
        assertMeasuredHeight(50) { wire ->
            HeightModifierOperation.apply(wire, DimensionModifierOperation.Type.FILL.ordinal, 0.5f)
        }
    }

    @Test
    fun dynamicWidthInRespondsToVariableUpdates() {
        val document = buildDocument {
            val maxVar = addNamedFloat("maxWidth", 100f)
            val descId = addText("target")
            root {
                startBox(
                    RecordingModifier().then { w ->
                        val wire = w.buffer.buffer
                        CoreSemantics().apply { mContentDescriptionId = descId }.write(wire)
                        WidthInModifierOperation.apply(wire, 0f, maxVar)
                        WidthModifierOperation.apply(
                            wire,
                            DimensionModifierOperation.Type.EXACT.ordinal,
                            200f,
                        )
                        HeightModifierOperation.apply(
                            wire,
                            DimensionModifierOperation.Type.EXACT.ordinal,
                            200f,
                        )
                    },
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                endBox()
            }
        }
        val state = RcPlayerState(document)
        playerRule.setContent { RcPlayer(state = state, modifier = Modifier.size(300.dp)) }
        playerRule.waitForIdle()
        assertEquals(
            100.dp,
            playerRule.onNodeWithContentDescription("target").getUnclippedBoundsInRoot().width,
        )

        requireNotNull(state.floatState("maxWidth")).value = 60f
        playerRule.waitForIdle()
        assertEquals(
            60.dp,
            playerRule.onNodeWithContentDescription("target").getUnclippedBoundsInRoot().width,
        )
    }

    @Test
    fun dynamicHeightInRespondsToVariableUpdates() {
        val document = buildDocument {
            val maxVar = addNamedFloat("maxHeight", 120f)
            val descId = addText("target")
            root {
                startBox(
                    RecordingModifier().then { w ->
                        val wire = w.buffer.buffer
                        CoreSemantics().apply { mContentDescriptionId = descId }.write(wire)
                        HeightInModifierOperation.apply(wire, 0f, maxVar)
                        WidthModifierOperation.apply(
                            wire,
                            DimensionModifierOperation.Type.EXACT.ordinal,
                            200f,
                        )
                        HeightModifierOperation.apply(
                            wire,
                            DimensionModifierOperation.Type.EXACT.ordinal,
                            200f,
                        )
                    },
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                endBox()
            }
        }
        val state = RcPlayerState(document)
        playerRule.setContent { RcPlayer(state = state, modifier = Modifier.size(300.dp)) }
        playerRule.waitForIdle()
        assertEquals(
            120.dp,
            playerRule.onNodeWithContentDescription("target").getUnclippedBoundsInRoot().height,
        )

        requireNotNull(state.floatState("maxHeight")).value = 75f
        playerRule.waitForIdle()
        assertEquals(
            75.dp,
            playerRule.onNodeWithContentDescription("target").getUnclippedBoundsInRoot().height,
        )
    }

    @Test
    fun dynamicOffsetRespondsToVariableUpdates() {
        val document = buildDocument {
            val offsetVar = addNamedFloat("offsetX", 15f)
            val descId = addText("target")
            root {
                startBox(
                    RecordingModifier().then { w ->
                        val wire = w.buffer.buffer
                        OffsetModifierOperation.apply(wire, offsetVar, 0f)
                        WidthModifierOperation.apply(
                            wire,
                            DimensionModifierOperation.Type.EXACT.ordinal,
                            50f,
                        )
                        HeightModifierOperation.apply(
                            wire,
                            DimensionModifierOperation.Type.EXACT.ordinal,
                            50f,
                        )
                        CoreSemantics().apply { mContentDescriptionId = descId }.write(wire)
                    },
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                endBox()
            }
        }
        val state = RcPlayerState(document)
        playerRule.setContent { RcPlayer(state = state, modifier = Modifier.size(200.dp)) }
        playerRule.waitForIdle()
        assertEquals(
            15f,
            playerRule.onNodeWithContentDescription("target").getUnclippedBoundsInRoot().left.value,
            0.01f,
        )

        requireNotNull(state.floatState("offsetX")).value = 30f
        playerRule.waitForIdle()
        assertEquals(
            30f,
            playerRule.onNodeWithContentDescription("target").getUnclippedBoundsInRoot().left.value,
            0.01f,
        )
    }

    @Test
    fun dynamicPaddingRespondsToVariableUpdates() {
        val document = buildDocument {
            val padVar = addNamedFloat("padLeft", 10f)
            val descId = addText("child")
            root {
                startBox(
                    RecordingModifier().then { w ->
                        PaddingModifierOperation.apply(w.buffer.buffer, padVar, 0f, 0f, 0f)
                    },
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                startBox(
                    RecordingModifier().width(40f).height(40f).then { w ->
                        CoreSemantics()
                            .apply { mContentDescriptionId = descId }
                            .write(w.buffer.buffer)
                    },
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                endBox()
                endBox()
            }
        }
        val state = RcPlayerState(document)
        playerRule.setContent { RcPlayer(state = state, modifier = Modifier.size(200.dp)) }
        playerRule.waitForIdle()
        assertEquals(
            10f,
            playerRule.onNodeWithContentDescription("child").getUnclippedBoundsInRoot().left.value,
            0.01f,
        )

        requireNotNull(state.floatState("padLeft")).value = 25f
        playerRule.waitForIdle()
        assertEquals(
            25f,
            playerRule.onNodeWithContentDescription("child").getUnclippedBoundsInRoot().left.value,
            0.01f,
        )
    }

    private fun assertMeasuredWidth(expected: Int, writeModifier: (WireBuffer) -> Unit) {
        val document = buildDocument {
            val descId = addText("target")
            root {
                startBox(
                    RecordingModifier().fillMaxSize(),
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                startBox(
                    RecordingModifier().then { w ->
                        val wire = w.buffer.buffer
                        CoreSemantics().apply { mContentDescriptionId = descId }.write(wire)
                        writeModifier(wire)
                        WidthModifierOperation.apply(
                            wire,
                            DimensionModifierOperation.Type.EXACT.ordinal,
                            10f,
                        )
                        HeightModifierOperation.apply(
                            wire,
                            DimensionModifierOperation.Type.EXACT.ordinal,
                            10f,
                        )
                    },
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                endBox()
                endBox()
            }
        }
        playerRule.setContent {
            RcPlayer(document = document, modifier = Modifier.size(100.dp, 50.dp))
        }
        playerRule.waitForIdle()
        assertEquals(
            expected.dp,
            playerRule.onNodeWithContentDescription("target").getUnclippedBoundsInRoot().width,
        )
    }

    private fun assertMeasuredHeight(expected: Int, writeModifier: (WireBuffer) -> Unit) {
        val document = buildDocument {
            val descId = addText("target")
            root {
                startBox(
                    RecordingModifier().fillMaxSize(),
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                startBox(
                    RecordingModifier().then { w ->
                        val wire = w.buffer.buffer
                        CoreSemantics().apply { mContentDescriptionId = descId }.write(wire)
                        writeModifier(wire)
                        WidthModifierOperation.apply(
                            wire,
                            DimensionModifierOperation.Type.EXACT.ordinal,
                            10f,
                        )
                        HeightModifierOperation.apply(
                            wire,
                            DimensionModifierOperation.Type.EXACT.ordinal,
                            10f,
                        )
                    },
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                endBox()
                endBox()
            }
        }
        playerRule.setContent {
            RcPlayer(document = document, modifier = Modifier.size(50.dp, 100.dp))
        }
        playerRule.waitForIdle()
        assertEquals(
            expected.dp,
            playerRule.onNodeWithContentDescription("target").getUnclippedBoundsInRoot().height,
        )
    }
}
