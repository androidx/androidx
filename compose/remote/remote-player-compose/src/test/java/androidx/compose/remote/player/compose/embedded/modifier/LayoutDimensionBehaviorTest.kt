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

package androidx.compose.remote.player.compose.embedded.modifier

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.modifiers.DimensionConstraintsModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.OffsetModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthInModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.RcPlayerTestRule
import androidx.compose.remote.player.compose.embedded.SnapshotRemoteComposeState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
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
        val operation =
            DimensionConstraintsModifierOperation(
                DimensionConstraintsModifierOperation.REQUIRED_HORIZONTAL_CONSTRAINTS,
                80f,
                -1f,
            )

        assertMeasuredWidth(80) { Modifier.dimensionConstraints(operation).size(10.dp) }
    }

    @Test
    fun requiredVerticalMinimumCanOverrideTheIncomingParentConstraint() {
        val operation =
            DimensionConstraintsModifierOperation(
                DimensionConstraintsModifierOperation.REQUIRED_VERTICAL_CONSTRAINTS,
                80f,
                -1f,
            )

        assertMeasuredHeight(80) { Modifier.dimensionConstraints(operation).size(10.dp) }
    }

    @Test
    fun fillParentMaxWidthUsesTheAvailableWidth() {
        val operation =
            WidthModifierOperation(DimensionModifierOperation.Type.FILL_PARENT_MAX_WIDTH, 1f)

        assertMeasuredWidth(100) { Modifier.width(operation).size(10.dp) }
    }

    @Test
    fun fillMaxWidthPreservesItsFraction() {
        val operation = WidthModifierOperation(DimensionModifierOperation.Type.FILL, 0.5f)

        assertMeasuredWidth(50) { Modifier.width(operation).size(10.dp) }
    }

    @Test
    fun fillParentMaxHeightUsesTheAvailableHeight() {
        val operation =
            HeightModifierOperation(DimensionModifierOperation.Type.FILL_PARENT_MAX_HEIGHT, 1f)

        assertMeasuredHeight(100) { Modifier.height(operation).size(10.dp) }
    }

    @Test
    fun fillMaxHeightPreservesItsFraction() {
        val operation = HeightModifierOperation(DimensionModifierOperation.Type.FILL, 0.5f)

        assertMeasuredHeight(50) { Modifier.height(operation).size(10.dp) }
    }

    @Test
    fun dynamicWidthInRespondsToVariableUpdates() {
        val state = SnapshotRemoteComposeState()
        val doc = CoreDocument()
        doc.setRemoteComposeState(state)
        val varId = 50
        state.updateFloat(varId, 100f)
        val op = WidthInModifierOperation(0f, Utils.asNan(varId))
        val measuredWidth = AtomicInteger()

        playerRule.setContent {
            CompositionLocalProvider(LocalCoreDocument provides doc) {
                Box(Modifier.widthIn(op).size(200.dp).onSizeChanged { measuredWidth.set(it.width) })
            }
        }
        playerRule.waitForIdle()
        assertEquals(100, measuredWidth.get())

        state.overrideFloat(varId, 60f)
        playerRule.waitForIdle()
        assertEquals(60, measuredWidth.get())
    }

    @Test
    fun dynamicHeightInRespondsToVariableUpdates() {
        val state = SnapshotRemoteComposeState()
        val doc = CoreDocument()
        doc.setRemoteComposeState(state)
        val varId = 51
        state.updateFloat(varId, 120f)
        val op = HeightInModifierOperation(0f, Utils.asNan(varId))
        val measuredHeight = AtomicInteger()

        playerRule.setContent {
            CompositionLocalProvider(LocalCoreDocument provides doc) {
                Box(
                    Modifier.heightIn(op).size(200.dp).onSizeChanged {
                        measuredHeight.set(it.height)
                    }
                )
            }
        }
        playerRule.waitForIdle()
        assertEquals(120, measuredHeight.get())

        state.overrideFloat(varId, 75f)
        playerRule.waitForIdle()
        assertEquals(75, measuredHeight.get())
    }

    @Test
    fun dynamicOffsetRespondsToVariableUpdates() {
        val state = SnapshotRemoteComposeState()
        val doc = CoreDocument()
        doc.setRemoteComposeState(state)
        val varId = 52
        state.updateFloat(varId, 15f)
        val op = OffsetModifierOperation(Utils.asNan(varId), 0f)
        val positionX = AtomicReference(0f)

        playerRule.setContent {
            CompositionLocalProvider(LocalCoreDocument provides doc) {
                Box(
                    Modifier.offset(op).size(50.dp).onGloballyPositioned {
                        positionX.set(it.positionInRoot().x)
                    }
                )
            }
        }
        playerRule.waitForIdle()
        assertEquals(15f, positionX.get(), 0.01f)

        state.overrideFloat(varId, 30f)
        playerRule.waitForIdle()
        assertEquals(30f, positionX.get(), 0.01f)
    }

    @Test
    fun dynamicPaddingRespondsToVariableUpdates() {
        val state = SnapshotRemoteComposeState()
        val doc = CoreDocument()
        doc.setRemoteComposeState(state)
        val varId = 53
        state.updateFloat(varId, 10f)
        val op = PaddingModifierOperation(Utils.asNan(varId), 0f, 0f, 0f)
        val positionX = AtomicReference(0f)

        playerRule.setContent {
            CompositionLocalProvider(LocalCoreDocument provides doc) {
                Box(
                    Modifier.padding(op).size(40.dp).onGloballyPositioned {
                        positionX.set(it.positionInRoot().x)
                    }
                )
            }
        }
        playerRule.waitForIdle()
        assertEquals(10f, positionX.get(), 0.01f)

        state.overrideFloat(varId, 25f)
        playerRule.waitForIdle()
        assertEquals(25f, positionX.get(), 0.01f)
    }

    private fun assertMeasuredWidth(expected: Int, childModifier: @Composable () -> Modifier) {
        val measuredWidth = AtomicInteger()
        playerRule.setContent {
            CompositionLocalProvider(LocalCoreDocument provides CoreDocument()) {
                Box(Modifier.size(100.dp, 50.dp)) {
                    Box(childModifier().onSizeChanged { measuredWidth.set(it.width) })
                }
            }
        }
        playerRule.waitForIdle()
        assertEquals(expected, measuredWidth.get())
    }

    private fun assertMeasuredHeight(expected: Int, childModifier: @Composable () -> Modifier) {
        val measuredHeight = AtomicInteger()
        playerRule.setContent {
            CompositionLocalProvider(LocalCoreDocument provides CoreDocument()) {
                Box(Modifier.size(50.dp, 100.dp)) {
                    Box(childModifier().onSizeChanged { measuredHeight.set(it.height) })
                }
            }
        }
        playerRule.waitForIdle()
        assertEquals(expected, measuredHeight.get())
    }
}
