/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.OverscrollTest.TestOverscrollEffect
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertShape
import androidx.compose.testutils.toList
import androidx.compose.ui.CombinedModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.size
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@MediumTest
class ScrollingContainerTest {
    @get:Rule val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun testInspectorValue() {
        rule.setContent {
            val modifiers =
                (Modifier.scrollingContainer(
                        rememberScrollState(),
                        orientation = Horizontal,
                        enabled = true,
                        reverseScrolling = false,
                        flingBehavior = null,
                        interactionSource = null,
                        useLocalOverscrollFactory = false,
                        overscrollEffect = null,
                        bringIntoViewSpec = null
                    ) as CombinedModifier)
                    .toList()
            val clip = modifiers[0] as InspectableValue
            assertThat(clip.nameFallback).isEqualTo("graphicsLayer")
            val scrollingContainer = modifiers[1] as InspectableValue
            assertThat(scrollingContainer.nameFallback).isEqualTo("scrollingContainer")
            assertThat(scrollingContainer.valueOverride).isNull()
            assertThat(scrollingContainer.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "state",
                    "orientation",
                    "enabled",
                    "reverseScrolling",
                    "flingBehavior",
                    "interactionSource",
                    "useLocalOverscrollFactory",
                    "overscrollEffect",
                    "bringIntoViewSpec"
                )
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun clipUpdatesWhenOrientationChanges() {
        var orientation by mutableStateOf(Horizontal)
        rule.setContent {
            val scrollState = rememberScrollState(20)
            Box(Modifier.size(60.dp).testTag("container").background(Color.Gray)) {
                Box(
                    Modifier.padding(20.dp)
                        .fillMaxSize()
                        .scrollingContainer(
                            state = scrollState,
                            orientation = orientation,
                            enabled = true,
                            reverseScrolling = false,
                            flingBehavior = null,
                            interactionSource = null,
                            useLocalOverscrollFactory = false,
                            overscrollEffect = null
                        )
                ) {
                    repeat(4) { Box(Modifier.size(20.dp).drawOutsideOfBounds()) }
                }
            }
        }

        rule
            .onNodeWithTag("container")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Red,
                backgroundColor = Color.Gray,
                horizontalPadding = 20.dp,
                verticalPadding = 0.dp
            )

        rule.runOnIdle { orientation = Vertical }

        rule
            .onNodeWithTag("container")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Red,
                backgroundColor = Color.Gray,
                horizontalPadding = 0.dp,
                verticalPadding = 20.dp
            )
    }

    @Test
    fun layoutDirectionChange_updatesScrollDirection() {
        val size = with(rule.density) { 100.toDp() }
        var scrollAmount = 0f
        val scrollState = ScrollableState {
            scrollAmount = (scrollAmount + it).coerceIn(0f, 10f)
            it
        }
        var layoutDirection by mutableStateOf(Ltr)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box {
                    Box(
                        Modifier.size(size)
                            .testTag("container")
                            .scrollingContainer(
                                state = scrollState,
                                orientation = Horizontal,
                                enabled = true,
                                reverseScrolling = false,
                                flingBehavior = null,
                                interactionSource = null,
                                useLocalOverscrollFactory = false,
                                overscrollEffect = null
                            )
                    )
                }
            }
        }

        rule.onNodeWithTag("container").performTouchInput { swipeLeft() }

        rule.runOnIdle {
            assertThat(scrollAmount).isEqualTo(10f)
            layoutDirection = Rtl
        }

        rule.onNodeWithTag("container").performTouchInput { swipeLeft() }

        // Now that layout direction changed, we should go back to 0
        rule.runOnIdle { assertThat(scrollAmount).isEqualTo(0f) }
    }

    @Test
    fun attachesOverscrollEffectNode() {
        val overscrollEffect = TestOverscrollEffect()

        rule.setContent {
            Box(
                Modifier.scrollingContainer(
                    rememberScrollState(),
                    orientation = Horizontal,
                    enabled = true,
                    reverseScrolling = false,
                    flingBehavior = null,
                    interactionSource = null,
                    useLocalOverscrollFactory = false,
                    overscrollEffect = overscrollEffect,
                    bringIntoViewSpec = null
                )
            )
        }

        rule.runOnIdle { assertThat(overscrollEffect.node.node.isAttached).isTrue() }
    }

    @Test
    fun undelegatesOverscrollEffectNodeOnDetach() {
        val overscrollEffect = TestOverscrollEffect()

        var addModifier by mutableStateOf(true)

        rule.setContent {
            Box(
                if (addModifier) {
                    Modifier.scrollingContainer(
                        rememberScrollState(),
                        orientation = Horizontal,
                        enabled = true,
                        reverseScrolling = false,
                        flingBehavior = null,
                        interactionSource = null,
                        useLocalOverscrollFactory = false,
                        overscrollEffect = overscrollEffect,
                        bringIntoViewSpec = null
                    )
                } else {
                    Modifier
                }
            )
        }

        rule.runOnIdle {
            // The node property points to the 'real node in the tree', so it will be different
            // from node if it has been delegated to. I.e., assert that this node has been delegated
            // to.
            assertThat(overscrollEffect.node.node).isNotEqualTo(overscrollEffect.node)
            assertThat(overscrollEffect.node.node.isAttached).isTrue()
            // Remove the scrolling container modifier
            addModifier = false
        }

        rule.runOnIdle {
            // Assert that this node is no longer delegated - the node property should point to
            // itself.
            assertThat(overscrollEffect.node.node).isEqualTo(overscrollEffect.node)
            assertThat(overscrollEffect.node.node.isAttached).isFalse()
            addModifier = true
        }

        rule.runOnIdle {
            // The node should be delegated again, and attached.
            assertThat(overscrollEffect.node.node).isNotEqualTo(overscrollEffect.node)
            assertThat(overscrollEffect.node.node.isAttached).isTrue()
        }
    }

    @Test
    fun updatesToNewOverscrollEffectNode() {
        val overscrollEffect1 = TestOverscrollEffect()
        val overscrollEffect2 = TestOverscrollEffect()
        var effect by mutableStateOf(overscrollEffect1)

        rule.setContent {
            Box(
                Modifier.scrollingContainer(
                    rememberScrollState(),
                    orientation = Horizontal,
                    enabled = true,
                    reverseScrolling = false,
                    flingBehavior = null,
                    interactionSource = null,
                    useLocalOverscrollFactory = false,
                    overscrollEffect = effect,
                    bringIntoViewSpec = null
                )
            )
        }

        rule.runOnIdle {
            assertThat(overscrollEffect1.node.node.isAttached).isTrue()
            assertThat(overscrollEffect2.node.node.isAttached).isFalse()
            effect = overscrollEffect2
        }

        // The old node should be detached, and the new one should be attached
        rule.runOnIdle {
            assertThat(overscrollEffect1.node.node.isAttached).isFalse()
            assertThat(overscrollEffect2.node.node.isAttached).isTrue()
        }
    }

    @Test
    fun doesNotAddAlreadyAttachedOverscrollEffectNode() {
        val overscrollEffect = TestOverscrollEffect()
        class CustomDelegatingNode : DelegatingNode() {
            init {
                delegate(overscrollEffect.node)
            }
        }

        val element =
            object : ModifierNodeElement<CustomDelegatingNode>() {
                override fun create() = CustomDelegatingNode()

                override fun update(node: CustomDelegatingNode) {}

                override fun equals(other: Any?) = other === this

                override fun hashCode() = -1
            }

        var addScrollingContainer by mutableStateOf(false)

        rule.setContent {
            Box(
                element.then(
                    if (addScrollingContainer)
                        Modifier.scrollingContainer(
                            rememberScrollState(),
                            orientation = Horizontal,
                            enabled = true,
                            reverseScrolling = false,
                            flingBehavior = null,
                            interactionSource = null,
                            useLocalOverscrollFactory = false,
                            overscrollEffect = overscrollEffect,
                            bringIntoViewSpec = null
                        )
                    else Modifier
                )
            )
        }

        rule.runOnIdle {
            assertThat(overscrollEffect.node.node.isAttached).isTrue()
            addScrollingContainer = true
        }

        // Should not crash - the node should not be added by Modifier.scrollingContainer
        rule.waitForIdle()
    }

    @Test
    fun attachesLocalOverscrollFactoryOverscrollEffectNode() {
        val tag = "scrollingContainer"
        val overscrollEffect = TestOverscrollEffect()
        val factory =
            object : OverscrollFactory {
                override fun createOverscrollEffect(): OverscrollEffect = overscrollEffect

                override fun equals(other: Any?): Boolean = other === this

                override fun hashCode(): Int = -1
            }

        rule.setContent {
            CompositionLocalProvider(LocalOverscrollFactory provides factory) {
                Box(
                    Modifier.fillMaxSize()
                        .testTag(tag)
                        .scrollingContainer(
                            rememberScrollState(),
                            orientation = Horizontal,
                            enabled = true,
                            reverseScrolling = false,
                            flingBehavior = null,
                            interactionSource = null,
                            useLocalOverscrollFactory = true,
                            overscrollEffect = null,
                            bringIntoViewSpec = null
                        )
                )
            }
        }

        rule.runOnIdle { assertThat(overscrollEffect.node.node.isAttached).isTrue() }

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(1000f, 0f))
        }

        // Events should be dispatched to the returned overscroll effect
        rule.runOnIdle {
            assertThat(overscrollEffect.lastOverscrollDelta).isNotEqualTo(Offset.Zero)
        }
    }

    @Test
    fun updatesToNewLocalOverscrollFactory() {
        val tag = "scrollingContainer"
        val overscrollEffect1 = TestOverscrollEffect()
        val overscrollEffect2 = TestOverscrollEffect()

        val factory1 =
            object : OverscrollFactory {
                override fun createOverscrollEffect(): OverscrollEffect = overscrollEffect1

                override fun equals(other: Any?): Boolean = other === this

                override fun hashCode(): Int = -1
            }

        val factory2 =
            object : OverscrollFactory {
                override fun createOverscrollEffect(): OverscrollEffect = overscrollEffect2

                override fun equals(other: Any?): Boolean = other === this

                override fun hashCode(): Int = -2
            }

        var factory by mutableStateOf<OverscrollFactory>(factory1)

        rule.setContent {
            CompositionLocalProvider(LocalOverscrollFactory provides factory) {
                Box(
                    Modifier.fillMaxSize()
                        .testTag(tag)
                        .scrollingContainer(
                            rememberScrollState(),
                            orientation = Horizontal,
                            enabled = true,
                            reverseScrolling = false,
                            flingBehavior = null,
                            interactionSource = null,
                            useLocalOverscrollFactory = true,
                            overscrollEffect = null,
                            bringIntoViewSpec = null
                        )
                )
            }
        }

        rule.runOnIdle {
            assertThat(overscrollEffect1.node.node.isAttached).isTrue()
            assertThat(overscrollEffect2.node.node.isAttached).isFalse()
        }

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(1000f, 0f))
            up()
        }

        // Events should be dispatched to the first overscroll effect
        rule.runOnIdle {
            assertThat(overscrollEffect1.lastOverscrollDelta).isNotEqualTo(Offset.Zero)
            assertThat(overscrollEffect2.lastOverscrollDelta).isEqualTo(Offset.Zero)
            overscrollEffect1.lastOverscrollDelta = Offset.Zero
            factory = factory2
        }

        // The old node should be detached, and the new one should be attached
        rule.runOnIdle {
            assertThat(overscrollEffect1.node.node.isAttached).isFalse()
            assertThat(overscrollEffect2.node.node.isAttached).isTrue()
        }

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(1000f, 0f))
            up()
        }

        // Events should be dispatched to the second overscroll effect
        rule.runOnIdle {
            assertThat(overscrollEffect1.lastOverscrollDelta).isEqualTo(Offset.Zero)
            assertThat(overscrollEffect2.lastOverscrollDelta).isNotEqualTo(Offset.Zero)
        }
    }

    @Test
    fun updatesBetweenProvidedOverscrollEffectAndLocalOverscrollFactory() {
        val tag = "scrollingContainer"
        val overscrollEffect1 = TestOverscrollEffect()
        val overscrollEffect2 = TestOverscrollEffect()

        val factory =
            object : OverscrollFactory {
                override fun createOverscrollEffect(): OverscrollEffect = overscrollEffect1

                override fun equals(other: Any?): Boolean = other === this

                override fun hashCode(): Int = -1
            }

        var useLocalOverscrollFactory by mutableStateOf(true)

        rule.setContent {
            CompositionLocalProvider(LocalOverscrollFactory provides factory) {
                Box(
                    Modifier.fillMaxSize()
                        .testTag(tag)
                        .scrollingContainer(
                            rememberScrollState(),
                            orientation = Horizontal,
                            enabled = true,
                            reverseScrolling = false,
                            flingBehavior = null,
                            interactionSource = null,
                            useLocalOverscrollFactory = useLocalOverscrollFactory,
                            overscrollEffect = overscrollEffect2,
                            bringIntoViewSpec = null
                        )
                )
            }
        }

        // useLocalOverscrollFactory = true, so it will override the overscrollEffect2 we set
        // on the modifier
        rule.runOnIdle {
            assertThat(overscrollEffect1.node.node.isAttached).isTrue()
            assertThat(overscrollEffect2.node.node.isAttached).isFalse()
        }

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(1000f, 0f))
            up()
        }

        // Events should be dispatched to the first overscroll effect
        rule.runOnIdle {
            assertThat(overscrollEffect1.lastOverscrollDelta).isNotEqualTo(Offset.Zero)
            assertThat(overscrollEffect2.lastOverscrollDelta).isEqualTo(Offset.Zero)
            overscrollEffect1.lastOverscrollDelta = Offset.Zero
            useLocalOverscrollFactory = false
        }

        // The factory-provided node should be detached, and the explicit node should be attached
        rule.runOnIdle {
            assertThat(overscrollEffect1.node.node.isAttached).isFalse()
            assertThat(overscrollEffect2.node.node.isAttached).isTrue()
        }

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(1000f, 0f))
            up()
        }

        // Events should be dispatched to the second overscroll effect
        rule.runOnIdle {
            assertThat(overscrollEffect1.lastOverscrollDelta).isEqualTo(Offset.Zero)
            assertThat(overscrollEffect2.lastOverscrollDelta).isNotEqualTo(Offset.Zero)
            overscrollEffect2.lastOverscrollDelta = Offset.Zero
            // Use the factory again
            useLocalOverscrollFactory = true
        }

        // useLocalOverscrollFactory = true, so it should be used again
        rule.runOnIdle {
            assertThat(overscrollEffect1.node.node.isAttached).isTrue()
            assertThat(overscrollEffect2.node.node.isAttached).isFalse()
        }

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(1000f, 0f))
            up()
        }

        // Events should be dispatched to the first overscroll effect again
        rule.runOnIdle {
            assertThat(overscrollEffect1.lastOverscrollDelta).isNotEqualTo(Offset.Zero)
            assertThat(overscrollEffect2.lastOverscrollDelta).isEqualTo(Offset.Zero)
        }
    }

    @Test
    fun changesToProvidedOverscrollEffectIgnoredIfUseLocalOverscrollFactoryTrue() {
        val overscrollEffect1 = TestOverscrollEffect()
        val overscrollEffect2 = TestOverscrollEffect()
        var creationCalls = 0

        val factory =
            object : OverscrollFactory {
                override fun createOverscrollEffect(): OverscrollEffect {
                    creationCalls++
                    return overscrollEffect1
                }

                override fun equals(other: Any?): Boolean = other === this

                override fun hashCode(): Int = -1
            }

        var overscrollEffect by mutableStateOf<OverscrollEffect?>(null)

        rule.setContent {
            CompositionLocalProvider(LocalOverscrollFactory provides factory) {
                Box(
                    Modifier.scrollingContainer(
                        rememberScrollState(),
                        orientation = Horizontal,
                        enabled = true,
                        reverseScrolling = false,
                        flingBehavior = null,
                        interactionSource = null,
                        useLocalOverscrollFactory = true,
                        overscrollEffect = overscrollEffect,
                        bringIntoViewSpec = null
                    )
                )
            }
        }

        rule.runOnIdle {
            assertThat(creationCalls).isEqualTo(1)
            assertThat(overscrollEffect1.node.node.isAttached).isTrue()
            assertThat(overscrollEffect2.node.node.isAttached).isFalse()
            // Change the provided overscrollEffect - this should no-op as useLocalOverscrollFactory
            // is true
            overscrollEffect = overscrollEffect2
        }

        rule.runOnIdle {
            // create should not be called again on the factory
            assertThat(creationCalls).isEqualTo(1)
            assertThat(overscrollEffect1.node.node.isAttached).isTrue()
            assertThat(overscrollEffect2.node.node.isAttached).isFalse()
        }
    }

    /**
     * Test for b/392060494
     *
     * Currently LayoutModifierNodes cannot delegate measurement to other LayoutModifierNodes. So if
     * scrollingContainer is a LayoutModifierNode, it prevents overscroll implementations from using
     * LayoutModifierNode internally. This test ensures that an overscroll implementation using
     * LayoutModifierNode works inside scrollingContainer.
     */
    @Test
    fun doesNotIgnoreOverscrollEffectNodeLayout() {
        val expectedOffset = IntOffset(20, 20)
        val overscrollEffect =
            object : OverscrollEffect {
                override val isInProgress = false

                override suspend fun applyToFling(
                    velocity: Velocity,
                    performFling: suspend (Velocity) -> Velocity
                ) {}

                override fun applyToScroll(
                    delta: Offset,
                    source: NestedScrollSource,
                    performScroll: (Offset) -> Offset
                ): Offset = performScroll(delta)

                override val node: DelegatableNode =
                    object : Modifier.Node(), LayoutModifierNode {
                        override fun MeasureScope.measure(
                            measurable: Measurable,
                            constraints: Constraints
                        ): MeasureResult {
                            val placeable = measurable.measure(constraints)
                            return layout(placeable.width, placeable.height) {
                                placeable.placeRelative(expectedOffset)
                            }
                        }
                    }
            }

        rule.setContent {
            Box(
                Modifier.scrollingContainer(
                    rememberScrollState(),
                    orientation = Horizontal,
                    enabled = true,
                    reverseScrolling = false,
                    flingBehavior = null,
                    interactionSource = null,
                    useLocalOverscrollFactory = false,
                    overscrollEffect = overscrollEffect,
                    bringIntoViewSpec = null
                )
            ) {
                Box(Modifier.size(20.dp).background(Color.Red).testTag("content"))
            }
        }

        rule.runOnIdle { assertThat(overscrollEffect.node.node.isAttached).isTrue() }
        val bounds = rule.onNodeWithTag("content").getUnclippedBoundsInRoot()
        with(rule.density) {
            assertThat(bounds.left.toPx()).isEqualTo(expectedOffset.x)
            assertThat(bounds.top.toPx()).isEqualTo(expectedOffset.y)
        }
    }

    private fun Modifier.drawOutsideOfBounds() = drawBehind {
        val inflate = 20.dp.roundToPx().toFloat()
        drawRect(
            Color.Red,
            Offset(-inflate, -inflate),
            Size(size.width + inflate * 2, size.height + inflate * 2)
        )
    }
}
