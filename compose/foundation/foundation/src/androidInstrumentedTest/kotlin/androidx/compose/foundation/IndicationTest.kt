/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class IndicationTest {

    @get:Rule val rule = createComposeRule()

    val testTag = "indication"

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun indication_drawIsCalled() {
        val interactionSource = MutableInteractionSource()
        val countDownLatch = CountDownLatch(1)
        val indication = TestPressIndication { countDownLatch.countDown() }
        rule.setContent {
            Box(Modifier.testTag(testTag).size(100.dp).indication(interactionSource, indication))
        }
        assertThat(countDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun indicationNodeFactory_drawIsCalled() {
        val interactionSource = MutableInteractionSource()
        val countDownLatch = CountDownLatch(1)
        val indication =
            TestPressIndicationNodeFactory(onCreate = null, onDraw = { countDownLatch.countDown() })
        rule.setContent {
            Box(Modifier.testTag(testTag).size(100.dp).indication(interactionSource, indication))
        }
        assertThat(countDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun indication_receivesUpdates() {
        // Draw should be called 3 times: initial draw, press, and release
        var drawCalls = 0
        val press = PressInteraction.Press(Offset.Zero)
        val release = PressInteraction.Release(press)

        val interactionSource = MutableInteractionSource()

        val indication = TestPressIndication { drawCalls++ }

        rule.setContent {
            Box(Modifier.testTag(testTag).size(100.dp).indication(interactionSource, indication))
        }

        // Due to b/302303969 there are no guarantees runOnIdle() will wait for drawing to happen
        rule.waitUntil { drawCalls == 1 }

        rule.runOnUiThread { runBlocking { interactionSource.emit(press) } }

        // Due to b/302303969 there are no guarantees runOnIdle() will wait for drawing to happen
        rule.waitUntil { drawCalls == 2 }

        rule.runOnUiThread { runBlocking { interactionSource.emit(release) } }

        // Due to b/302303969 there are no guarantees runOnIdle() will wait for drawing to happen
        rule.waitUntil { drawCalls == 3 }
    }

    @Test
    fun indicationNodeFactory_receivesUpdates() {
        // Draw should be called 3 times: initial draw, press, and release
        var drawCalls = 0
        val press = PressInteraction.Press(Offset.Zero)
        val release = PressInteraction.Release(press)

        val interactionSource = MutableInteractionSource()

        val indication = TestPressIndicationNodeFactory(onDraw = { drawCalls++ }, onCreate = null)

        rule.setContent {
            Box(Modifier.testTag(testTag).size(100.dp).indication(interactionSource, indication))
        }

        // Due to b/302303969 there are no guarantees runOnIdle() will wait for drawing to happen
        rule.waitUntil { drawCalls == 1 }

        rule.runOnUiThread { runBlocking { interactionSource.emit(press) } }

        // Due to b/302303969 there are no guarantees runOnIdle() will wait for drawing to happen
        rule.waitUntil { drawCalls == 2 }

        rule.runOnUiThread { runBlocking { interactionSource.emit(release) } }

        // Due to b/302303969 there are no guarantees runOnIdle() will wait for drawing to happen
        rule.waitUntil { drawCalls == 3 }
    }

    @Test
    fun indication_testInspectorValue() {
        val state = MutableInteractionSource()
        val indication = TestPressIndication()
        rule.setContent {
            val modifier = Modifier.indication(state, indication) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("indication")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly("indication", "interactionSource")
        }
    }

    @Test
    fun indicationNodeFactory_testInspectorValue() {
        val state = MutableInteractionSource()
        val indication = TestPressIndicationNodeFactory(null, null)
        rule.setContent {
            val modifier = Modifier.indication(state, indication) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("indication")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly("indication", "interactionSource")
        }
    }

    @Test
    fun indicationNodeFactory_newInteractionSource() {
        var interactionSource by mutableStateOf(MutableInteractionSource())
        var createCalls = 0
        var drawCalls = 0
        val indication =
            TestPressIndicationNodeFactory(onCreate = { createCalls++ }, onDraw = { drawCalls++ })

        rule.setContent {
            Box(Modifier.testTag(testTag).size(100.dp).indication(interactionSource, indication))
        }

        // Due to b/302303969 there are no guarantees runOnIdle() will wait for drawing to happen
        rule.waitUntil { drawCalls == 1 }

        rule.runOnIdle {
            assertThat(createCalls).isEqualTo(1)
            assertThat(drawCalls).isEqualTo(1)
            // Create a new interaction source
            interactionSource = MutableInteractionSource()
        }

        // Due to b/302303969 there are no guarantees runOnIdle() will wait for drawing to happen
        rule.waitUntil { drawCalls == 2 }

        rule.runOnIdle {
            // New instance should be created
            assertThat(createCalls).isEqualTo(2)
            // The new node should be drawn
            assertThat(drawCalls).isEqualTo(2)
        }
    }

    @Test
    fun indicationNodeFactory_reuse_sameIndicationInstance() {
        val interactionSource = MutableInteractionSource()
        var createCalls = 0
        val onCreate: () -> Unit = { createCalls++ }
        val indication = TestPressIndicationNodeFactory(onCreate = onCreate, onDraw = null)

        var state by mutableStateOf(false)

        rule.setContent {
            // state read to force recomposition
            @Suppress("UNUSED_VARIABLE") val readState = state
            Box(Modifier.testTag(testTag).size(100.dp).indication(interactionSource, indication))
        }

        rule.runOnIdle {
            assertThat(createCalls).isEqualTo(1)
            // force recomposition
            state = !state
        }

        rule.runOnIdle {
            // We should still reuse the old instance
            assertThat(createCalls).isEqualTo(1)
        }
    }

    @Test
    fun indicationNodeFactory_reuse_differentIndicationInstance_comparesEqual() {
        val interactionSource = MutableInteractionSource()
        var createCalls = 0
        val onCreate: () -> Unit = { createCalls++ }
        var indication by
            mutableStateOf(TestPressIndicationNodeFactory(onCreate = onCreate, onDraw = null))

        rule.setContent {
            Box(Modifier.testTag(testTag).size(100.dp).indication(interactionSource, indication))
        }

        rule.runOnIdle {
            assertThat(createCalls).isEqualTo(1)
            // Create a new indication instance that should compare equal
            indication = TestPressIndicationNodeFactory(onCreate = onCreate, onDraw = null)
        }

        rule.runOnIdle {
            // We should still reuse the old instance
            assertThat(createCalls).isEqualTo(1)
        }
    }

    @Test
    fun indicationNodeFactory_recreation() {
        val interactionSource = MutableInteractionSource()
        var createCalls = 0
        var drawnNode = 0
        var indication by
            mutableStateOf(
                TestPressIndicationNodeFactory(
                    onCreate = { createCalls++ },
                    onDraw = { drawnNode = 1 }
                )
            )

        rule.setContent {
            Box(Modifier.testTag(testTag).size(100.dp).indication(interactionSource, indication))
        }

        // Due to b/302303969 there are no guarantees runOnIdle() will wait for drawing to happen
        rule.waitUntil { drawnNode == 1 }

        rule.runOnIdle {
            assertThat(createCalls).isEqualTo(1)
            assertThat(drawnNode).isEqualTo(1)
            // Reset drawn node
            drawnNode = 0
            // Create a new indication instance that should not compare equal
            indication =
                TestPressIndicationNodeFactory(
                    onCreate = { createCalls++ },
                    onDraw = { drawnNode = 2 }
                )
        }

        // Due to b/302303969 there are no guarantees runOnIdle() will wait for drawing to happen
        rule.waitUntil { drawnNode == 2 }

        rule.runOnIdle {
            // New instance that doesn't compare equal, so we should create again
            assertThat(createCalls).isEqualTo(2)
            // The new node should be drawn
            assertThat(drawnNode).isEqualTo(2)
        }
    }
}

/**
 * Simple [Indication] that draws a black overlay for pressed state. [rememberUpdatedInstance] is
 * deprecated, but this exists to test the backwards compat path for older indication
 * implementations.
 *
 * @param onDraw lambda executed when draw is called for the created instance
 */
@Suppress("DEPRECATION_ERROR")
private class TestPressIndication(val onDraw: () -> Unit = {}) : Indication {
    @Deprecated("Super method is deprecated")
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val isPressed = interactionSource.collectIsPressedAsState()
        return remember(interactionSource, isPressed) {
            object : IndicationInstance {
                override fun ContentDrawScope.drawIndication() {
                    onDraw()
                    drawContent()
                    if (isPressed.value) {
                        drawRect(color = Color.Black.copy(alpha = 0.3f), size = size)
                    }
                }
            }
        }
    }
}

/**
 * Simple [IndicationNodeFactory] that draws a black overlay for pressed state.
 *
 * @param onDraw lambda executed when draw is called for the created node
 * @param onCreate lambda executed when [create] is called
 */
private class TestPressIndicationNodeFactory(
    val onCreate: (() -> Unit)?,
    val onDraw: (() -> Unit)?
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        onCreate?.invoke()
        return object : Modifier.Node(), DrawModifierNode {
            private var isPressed by mutableStateOf(false)

            override fun onAttach() {
                coroutineScope.launch {
                    val pressInteractions = mutableListOf<PressInteraction.Press>()
                    interactionSource.interactions.collect { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> pressInteractions.add(interaction)
                            is PressInteraction.Release ->
                                pressInteractions.remove(interaction.press)
                            is PressInteraction.Cancel ->
                                pressInteractions.remove(interaction.press)
                        }
                        isPressed = pressInteractions.isNotEmpty()
                    }
                }
            }

            override fun ContentDrawScope.draw() {
                onDraw?.invoke()
                drawContent()
                if (isPressed) {
                    drawRect(color = Color.Black.copy(alpha = 0.3f), size = size)
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TestPressIndicationNodeFactory) return false

        if (onDraw != other.onDraw) return false
        if (onCreate != other.onCreate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = onDraw.hashCode()
        result = 31 * result + onCreate.hashCode()
        return result
    }
}
