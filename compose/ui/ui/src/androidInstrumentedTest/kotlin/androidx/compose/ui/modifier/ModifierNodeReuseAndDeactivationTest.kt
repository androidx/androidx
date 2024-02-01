/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.modifier

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.ComposeUiNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ModifierNodeReuseAndDeactivationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun reusingCallsResetOnModifier() {
        var reuseKey by mutableStateOf(0)

        var resetCalls = 0

        rule.setContent {
            ReusableContent(reuseKey) {
                TestLayout(onReset = { resetCalls++ })
            }
        }

        rule.runOnIdle {
            assertThat(resetCalls).isEqualTo(0)
            reuseKey = 1
        }

        rule.runOnIdle {
            assertThat(resetCalls).isEqualTo(1)
        }
    }

    @Test
    fun nodeIsNotRecreatedWhenReused() {
        var reuseKey by mutableStateOf(0)

        var createCalls = 0

        rule.setContent {
            ReusableContent(reuseKey) {
                TestLayout(onCreate = { createCalls++ })
            }
        }

        rule.runOnIdle {
            assertThat(createCalls).isEqualTo(1)
            reuseKey = 1
        }

        rule.runOnIdle {
            assertThat(createCalls).isEqualTo(1)
        }
    }

    @Test
    fun resetIsCalledWhenContentIsDeactivated() {
        var active by mutableStateOf(true)
        var resetCalls = 0

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    TestLayout(onReset = { resetCalls++ })
                }
            }
        }

        rule.runOnIdle {
            assertThat(resetCalls).isEqualTo(0)
            active = false
        }

        rule.runOnIdle {
            assertThat(resetCalls).isEqualTo(1)
        }
    }

    @Test
    fun resetIsCalledAgainWhenContentIsReactivated() {
        var active by mutableStateOf(true)
        var resetCalls = 0

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    TestLayout(onReset = { resetCalls++ })
                }
            }
        }

        rule.runOnIdle {
            active = false
        }

        rule.runOnIdle {
            active = true
        }

        rule.runOnIdle {
            assertThat(resetCalls).isEqualTo(1)
        }
    }

    @Test
    fun updateIsNotCalledWhenReusedWithTheSameParams() {
        var reuseKey by mutableStateOf(0)
        var updateCalls = 0

        rule.setContent {
            ReusableContent(reuseKey) {
                TestLayout(
                    key = 1,
                    onUpdate = { updateCalls++ }
                )
            }
        }

        rule.runOnIdle {
            assertThat(updateCalls).isEqualTo(0)
            reuseKey++
        }

        rule.runOnIdle {
            assertThat(updateCalls).isEqualTo(0)
        }
    }

    @Test
    fun updateIsCalledWhenReusedWithDifferentParam() {
        var reuseKey by mutableStateOf(0)
        var updateCalls = 0

        rule.setContent {
            ReusableContent(reuseKey) {
                TestLayout(
                    key = reuseKey,
                    onUpdate = { updateCalls++ }
                )
            }
        }

        rule.runOnIdle {
            assertThat(updateCalls).isEqualTo(0)
            reuseKey++
        }

        rule.runOnIdle {
            assertThat(updateCalls).isEqualTo(1)
        }
    }

    @Test
    fun nodesAreDetachedWhenReused() {
        var reuseKey by mutableStateOf(0)

        var onResetCalls = 0
        var onAttachCalls = 0
        var onResetCallsWhenDetached: Int? = null

        rule.setContent {
            ReusableContent(reuseKey) {
                TestLayout(
                    onAttach = { onAttachCalls++ },
                    onReset = { onResetCalls++ },
                    onDetach = { onResetCallsWhenDetached = onResetCalls }
                )
            }
        }

        rule.runOnIdle {
            assertThat(onAttachCalls).isEqualTo(1)
            assertThat(onResetCallsWhenDetached).isNull()
            reuseKey = 1
        }

        rule.runOnIdle {
            assertThat(onResetCalls).isEqualTo(1)
            // makes sure onReset is called before detach:
            assertThat(onResetCallsWhenDetached).isEqualTo(1)
            assertThat(onAttachCalls).isEqualTo(2)
        }
    }

    // Regression test for b/275919849
    @Test
    fun unchangedNodesAreDetachedAndReattachedWhenReused() {
        val nodeInstance = object : Modifier.Node() {}
        val element = object : ModifierNodeElement<Modifier.Node>() {
            override fun create(): Modifier.Node = nodeInstance
            override fun hashCode(): Int = System.identityHashCode(this)
            override fun equals(other: Any?) = (other === this)
            override fun update(node: Modifier.Node) { }
        }

        var active by mutableStateOf(true)
        rule.setContent {
            ReusableContentHost(active) {
                // Custom Layout that measures to 1x1 pixels and only assigns the modifiers once
                // when the node is created. Even if the modifiers aren't reassigned, they should
                // still undergo the same lifecycle.
                ReusableComposeNode<ComposeUiNode, Applier<Any>>(
                    factory = {
                        LayoutNode().apply {
                            measurePolicy = MeasurePolicy { _, _ -> layout(1, 1) {} }
                            modifier = element
                        }
                    },
                    update = { },
                    content = { }
                )
            }
        }

        rule.runOnIdle {
            assertWithMessage("Modifier Node was not attached when being initially created")
                .that(nodeInstance.isAttached).isTrue()
        }

        active = false
        rule.runOnIdle {
            assertWithMessage("Modifier Node should be detached when its LayoutNode is deactivated")
                .that(nodeInstance.isAttached).isFalse()
        }

        active = true
        rule.runOnIdle {
            assertWithMessage("Modifier Node was not attached after being reactivated")
                .that(nodeInstance.isAttached).isTrue()
        }
    }

    @Test
    fun nodesAreDetachedAndAttachedWhenDeactivatedAndReactivated() {
        var active by mutableStateOf(true)

        var onResetCalls = 0
        var onAttachCalls = 0
        var onResetCallsWhenDetached: Int? = null

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    TestLayout(
                        onAttach = { onAttachCalls++ },
                        onReset = { onResetCalls++ },
                        onDetach = { onResetCallsWhenDetached = onResetCalls }
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(onAttachCalls).isEqualTo(1)
            assertThat(onResetCallsWhenDetached).isNull()
            active = false
        }

        rule.runOnIdle {
            assertThat(onResetCalls).isEqualTo(1)
            // makes sure onReset is called before detach:
            assertThat(onResetCallsWhenDetached).isEqualTo(1)
            assertThat(onAttachCalls).isEqualTo(1)
            active = true
        }

        rule.runOnIdle {
            assertThat(onAttachCalls).isEqualTo(2)
        }
    }

    @Test
    fun reusingStatelessModifierNotCausingInvalidation() {
        var active by mutableStateOf(true)
        var reuseKey by mutableStateOf(0)

        var invalidations = 0
        val onInvalidate: () -> Unit = {
            invalidations++
        }

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(reuseKey) {
                    Layout(
                        modifier = StatelessElement(onInvalidate),
                        measurePolicy = MeasurePolicy
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            active = false
        }

        rule.runOnIdle {
            active = true
            reuseKey = 1
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
        }
    }

    @Test
    fun reusingStatelessModifierWithUpdatedInputCausingInvalidation() {
        var active by mutableStateOf(true)
        var reuseKey by mutableStateOf(0)
        var size by mutableStateOf(10)

        var invalidations = 0
        val onInvalidate: () -> Unit = {
            invalidations++
        }

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(reuseKey) {
                    Layout(
                        modifier = StatelessElement(onInvalidate, size),
                        measurePolicy = MeasurePolicy
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            active = false
        }

        rule.runOnIdle {
            active = true
            reuseKey = 1
            size = 20
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(2)
        }
    }

    @Test
    fun reusingModifierCausingInvalidationOnDelegatedInnerNode() {
        var reuseKey by mutableStateOf(0)

        var resetCalls = 0
        val onReset: () -> Unit = {
            resetCalls++
        }

        rule.setContent {
            ReusableContent(reuseKey) {
                Layout(
                    modifier = DelegatingElement(onReset),
                    measurePolicy = MeasurePolicy
                )
            }
        }

        rule.runOnIdle {
            assertThat(resetCalls).isEqualTo(0)
            reuseKey = 1
        }

        rule.runOnIdle {
            assertThat(resetCalls).isEqualTo(1)
        }
    }

    @Test
    fun reusingModifierReadingStateInLayerBlock() {
        var active by mutableStateOf(true)
        var counter by mutableStateOf(0)

        var invalidations = 0
        val layerBlock: () -> Unit = {
            // state read
            counter.toString()
            invalidations++
        }

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    Layout(
                        modifier = LayerElement(layerBlock),
                        measurePolicy = MeasurePolicy
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            active = false
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            counter++
        }

        rule.runOnIdle {
            active = true
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(2)
            counter++
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(3)
        }
    }

    @Test
    fun reusingModifierReadingStateInMeasureBlock() {
        var active by mutableStateOf(true)
        var counter by mutableStateOf(0)

        var invalidations = 0
        val measureBlock: () -> Unit = {
            // state read
            counter.toString()
            invalidations++
        }

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    Layout(
                        modifier = LayoutElement(measureBlock),
                        measurePolicy = MeasurePolicy
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            active = false
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            counter++
        }

        rule.runOnIdle {
            active = true
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(2)
            counter++
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(3)
        }
    }

    @Test
    fun reusingModifierReadingStateInMeasureBlock_oldModifiers() {
        var active by mutableStateOf(true)
        var counter by mutableStateOf(0)

        var invalidations = 0
        val measureBlock: () -> Unit = {
            // state read
            counter.toString()
            invalidations++
        }

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    Layout(
                        modifier = OldLayoutModifier(measureBlock),
                        measurePolicy = MeasurePolicy
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            active = false
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            counter++
        }

        rule.runOnIdle {
            active = true
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(2)
            counter++
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(3)
        }
    }

    @Test
    fun reusingModifierWithoutDeactivation_ReadingStateInMeasurelock_oldModifiers() {
        var key by mutableStateOf(0)
        var counter by mutableStateOf(0)

        var invalidations = 0
        val drawBlock: () -> Unit = {
            // state read
            counter.toString()
            invalidations++
        }

        rule.setContent {
            ReusableContent(key) {
                Layout(
                    modifier = OldLayoutModifier(drawBlock),
                    measurePolicy = MeasurePolicy
                )
            }
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            key = 1
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            counter++
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(2)
        }
    }

    @Test
    fun reusingModifierReadingStateInDrawBlock() {
        var active by mutableStateOf(true)
        var counter by mutableStateOf(0)

        var invalidations = 0
        val drawBlock: () -> Unit = {
            // state read
            counter.toString()
            invalidations++
        }

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    Layout(
                        modifier = DrawElement(drawBlock),
                        measurePolicy = MeasurePolicy
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            active = false
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            counter++
        }

        rule.runOnIdle {
            active = true
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(2)
            counter++
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(3)
        }
    }

    @Test
    fun reusingModifierReadingStateInDrawBlock_oldModifiers() {
        var active by mutableStateOf(true)
        var counter by mutableStateOf(0)

        var invalidations = 0
        val drawBlock: () -> Unit = {
            // state read
            counter.toString()
            invalidations++
        }

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    Layout(
                        modifier = OldDrawModifier(drawBlock),
                        measurePolicy = MeasurePolicy
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            active = false
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            counter++
        }

        rule.runOnIdle {
            active = true
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(2)
            counter++
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(3)
        }
    }

    @Test
    fun reusingModifierWithoutDeactivation_ReadingStateInDrawBlock_oldModifiers() {
        var key by mutableStateOf(0)
        var counter by mutableStateOf(0)

        var invalidations = 0
        val drawBlock: () -> Unit = {
            // state read
            counter.toString()
            invalidations++
        }

        rule.setContent {
                ReusableContent(key) {
                    Layout(
                        modifier = OldDrawModifier(drawBlock),
                        measurePolicy = MeasurePolicy
                    )
                }
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            key = 1
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            counter++
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(2)
        }
    }

    @Test
    fun reusingModifierObservingState() {
        var active by mutableStateOf(true)
        var counter by mutableStateOf(0)

        var invalidations = 0
        val observedBlock: () -> Unit = {
            // state read
            counter.toString()
            invalidations++
        }

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    Layout(
                        modifier = ObserverElement(observedBlock),
                        measurePolicy = MeasurePolicy
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            active = false
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(1)
            counter++
        }

        rule.runOnIdle {
            active = true
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(2)
            counter++
        }

        rule.runOnIdle {
            assertThat(invalidations).isEqualTo(3)
        }
    }

    @Test
    fun reusingModifierLocalProviderAndConsumer() {
        val key = modifierLocalOf { -1 }
        var active by mutableStateOf(true)
        var providedValue by mutableStateOf(0)

        var receivedValue: Int? = null

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    @OptIn(ExperimentalComposeUiApi::class)
                    Layout(
                        modifier = Modifier
                            .modifierLocalProvider(key) { providedValue }
                            .modifierLocalConsumer { receivedValue = key.current },
                        measurePolicy = MeasurePolicy
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(receivedValue).isEqualTo(0)
            active = false
        }

        rule.runOnIdle {
            providedValue = 1
        }

        rule.runOnIdle {
            active = true
        }

        rule.runOnIdle {
            assertThat(receivedValue).isEqualTo(1)
        }
    }

    @Test
    fun placingChildWithReusedUnchangedModifier() {
        // regression test for b/271430143
        var active by mutableStateOf(true)
        var modifier by mutableStateOf(StatelessLayoutElement1.then(StatelessLayoutElement2))
        var childX by mutableStateOf(0)

        rule.setContent {
            ReusableContentHost(active) {
                ReusableContent(0) {
                    Layout(content = {
                        Layout(
                            modifier = modifier.testTag("child"),
                            measurePolicy = MeasurePolicy
                        )
                    }) { measurables, constraints ->
                        val placeable = measurables.first().measure(constraints)
                        layout(placeable.width, placeable.height) {
                            childX.toString()
                            placeable.place(childX, 0)
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            active = false
        }

        rule.runOnIdle {
            active = true
            modifier = StatelessLayoutElement1
            // force relayout parent
            childX = 10
        }

        rule.onNodeWithTag("child")
            .assertLeftPositionInRootIsEqualTo(with(rule.density) { 10.toDp() })
    }

    @Test
    fun layoutCoordinatesOnDeactivatedNode() {
        var active by mutableStateOf(true)
        var coordinates: LayoutCoordinates? = null

        rule.setContent {
            ReusableContentHost(active) {
                Layout(content = {
                    Layout(
                        modifier = Modifier.size(50.dp).testTag("child"),
                        measurePolicy = MeasurePolicy
                    )
                }) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    layout(placeable.width, placeable.height) {
                        coordinates = this.coordinates
                        placeable.place(0, 0)
                    }
                }
            }
        }

        rule.runOnIdle {
            active = false
        }

        rule.runOnIdle {
            assertThat(coordinates?.isAttached).isEqualTo(false)
        }
    }
}

@Composable
private fun TestLayout(
    key: Any? = null,
    onReset: () -> Unit = {},
    onCreate: () -> Unit = {},
    onUpdate: () -> Unit = {},
    onDetach: () -> Unit = {},
    onAttach: () -> Unit = {}
) {
    val currentOnReset by rememberUpdatedState(onReset)
    val currentOnCreate by rememberUpdatedState(onCreate)
    val currentOnUpdate by rememberUpdatedState(onUpdate)
    val currentOnDetach by rememberUpdatedState(onDetach)
    val currentOnAttach by rememberUpdatedState(onAttach)
    Layout(
        modifier = createModifier(
            key = key,
            onCreate = { currentOnCreate.invoke() },
            onUpdate = { currentOnUpdate.invoke() },
            onReset = { currentOnReset.invoke() },
            onDetach = { currentOnDetach.invoke() },
            onAttach = { currentOnAttach.invoke() },
        ),
        measurePolicy = MeasurePolicy
    )
}

private fun createModifier(
    key: Any? = null,
    onCreate: () -> Unit = {},
    onUpdate: () -> Unit = {},
    onReset: () -> Unit = {},
    onDetach: () -> Unit = {},
    onAttach: () -> Unit = {},
): Modifier {
    class GenericModifierWithLifecycle(
        val key: Any?
    ) : ModifierNodeElement<Modifier.Node>() {
        override fun create(): Modifier.Node {
            onCreate()
            return object : Modifier.Node() {
                override fun onReset() = onReset()
                override fun onAttach() = onAttach()
                override fun onDetach() = onDetach()
            }
        }

        override fun update(node: Modifier.Node) { onUpdate() }

        override fun hashCode(): Int = "ModifierNodeReuseAndDeactivationTest".hashCode()

        override fun equals(other: Any?) = (other === this) ||
            (other is GenericModifierWithLifecycle && other.key == this.key)
    }
    return GenericModifierWithLifecycle(key)
}

private val MeasurePolicy = MeasurePolicy { _, _ ->
    layout(100, 100) { }
}

private data class StatelessElement(
    private val onInvalidate: () -> Unit,
    private val size: Int = 10
) : ModifierNodeElement<StatelessElement.Node>() {
    override fun create() = Node(size, onInvalidate)

    override fun update(node: Node) {
        node.size = size
        node.onMeasure = onInvalidate
    }

    class Node(var size: Int, var onMeasure: () -> Unit) : Modifier.Node(), LayoutModifierNode {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val placeable = measurable.measure(Constraints.fixed(size, size))
            onMeasure()
            return layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }
}

private data class DelegatingElement(
    private val onDelegatedNodeReset: () -> Unit,
) : ModifierNodeElement<DelegatingElement.Node>() {
    override fun create() = Node(onDelegatedNodeReset)

    override fun update(node: Node) {
        node.onReset = onDelegatedNodeReset
    }

    class Node(var onReset: () -> Unit) : DelegatingNode() {
        private val inner = delegate(
            object : Modifier.Node() {
                override fun onReset() {
                    this@Node.onReset.invoke()
                }
            }
        )
    }
}

private data class LayerElement(
    private val layerBlock: () -> Unit,
) : ModifierNodeElement<LayerElement.Node>() {
    override fun create() = Node(layerBlock)

    override fun update(node: Node) {
        node.layerBlock = layerBlock
    }

    class Node(var layerBlock: () -> Unit) : Modifier.Node(), LayoutModifierNode {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            return layout(placeable.width, placeable.height) {
                placeable.placeWithLayer(0, 0) {
                    layerBlock.invoke()
                }
            }
        }
    }
}

private data class ObserverElement(
    private val observedBlock: () -> Unit,
) : ModifierNodeElement<ObserverElement.Node>() {
    override fun create() = Node(observedBlock)

    override fun update(node: Node) {
        node.observedBlock = observedBlock
    }

    class Node(var observedBlock: () -> Unit) : Modifier.Node(), ObserverModifierNode {

        override fun onAttach() {
            observe()
        }

        private fun observe() {
            observeReads {
                observedBlock()
            }
        }

        override fun onObservedReadsChanged() {
            observe()
        }
    }
}

private data class LayoutElement(
    private val measureBlock: () -> Unit,
) : ModifierNodeElement<LayoutElement.Node>() {
    override fun create() = Node(measureBlock)

    override fun update(node: Node) {
        node.measureBlock = measureBlock
    }

    class Node(var measureBlock: () -> Unit) : Modifier.Node(), LayoutModifierNode {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            measureBlock.invoke()
            return layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }
}

private data class OldLayoutModifier(
    private val measureBlock: () -> Unit,
) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        measureBlock.invoke()
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}

private data class DrawElement(
    private val drawBlock: () -> Unit,
) : ModifierNodeElement<DrawElement.Node>() {
    override fun create() = Node(drawBlock)

    override fun update(node: Node) {
        node.drawBlock = drawBlock
    }

    class Node(var drawBlock: () -> Unit) : Modifier.Node(), DrawModifierNode {
        override fun ContentDrawScope.draw() {
            drawBlock.invoke()
        }
    }
}

private data class OldDrawModifier(
    private val measureBlock: () -> Unit,
) : DrawModifier {

    override fun ContentDrawScope.draw() {
        measureBlock.invoke()
    }
}

private object StatelessLayoutElement1 : ModifierNodeElement<StatelessLayoutModifier1>() {
    override fun create() = StatelessLayoutModifier1()
    override fun update(node: StatelessLayoutModifier1) {}
    override fun hashCode(): Int = 241
    override fun equals(other: Any?) = other === this
}

private class StatelessLayoutModifier1 : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}

private object StatelessLayoutElement2 : ModifierNodeElement<StatelessLayoutModifier2>() {
    override fun create() = StatelessLayoutModifier2()
    override fun update(node: StatelessLayoutModifier2) {}
    override fun hashCode(): Int = 242
    override fun equals(other: Any?) = other === this
}

private class StatelessLayoutModifier2 : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}
