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

package androidx.compose.ui.layout

import androidx.collection.mutableIntSetOf
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DelegatableNode.RegistrationHandle
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semanticsId
import androidx.compose.ui.spatial.RelativeLayoutBounds
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.zIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class OnGlobalLayoutListenerTest {
    @get:Rule val rule = createComposeRule()

    private val targetTag = "target"

    private val occlusionTag = "occluding"

    @Test
    fun notOccludingSiblings_whenNotOverlapping() =
        with(rule.density) {
            val rootSizePx = 300f
            val itemSizePx = 100f

            rule.setContent {
                PlaceTwoLayoutsApartVert(Modifier.size(rootSizePx.toDp())) {
                    Box(Modifier.size(itemSizePx.toDp()).testTag(targetTag))
                    Box(Modifier.size(itemSizePx.toDp()).testTag(occlusionTag))
                }
            }
            rule.onNodeWithTag(targetTag).assertNoOcclusions()
        }

    @Test
    fun notOccludingSiblings_whenPlacedBeforeWithHigherZIndex() =
        with(rule.density) {
            val rootSizePx = 150f
            val itemSizePx = 100f

            rule.setContent {
                PlaceTwoLayoutsApartVert(Modifier.size(rootSizePx.toDp())) {
                    Box(Modifier.size(itemSizePx.toDp()).zIndex(zIndex = 1f).testTag(targetTag))
                    Box(Modifier.size(itemSizePx.toDp()).zIndex(zIndex = 0f).testTag(occlusionTag))
                }
            }
            rule.onNodeWithTag(targetTag).assertNoOcclusions()
        }

    @Test
    fun notOccludingSiblings_whenPlacedAfter() =
        with(rule.density) {
            val rootSizePx = 150f
            val itemSizePx = 100f

            rule.setContent {
                Box(Modifier.size(rootSizePx.toDp())) {
                    Box(Modifier.size(itemSizePx.toDp()).testTag(occlusionTag))
                    Box(Modifier.size(itemSizePx.toDp()).testTag(targetTag))
                }
            }
            rule.onNodeWithTag(targetTag).assertNoOcclusions()
        }

    @Test
    fun notOccludingByChildrenAndGrandChildren() =
        with(rule.density) {
            val itemSizePx = 100f

            rule.setContent {
                Box(
                    // Root Composable cannot be occluded
                    Modifier.testTag(targetTag)
                ) {
                    Row { repeat(3) { Box(Modifier.size(itemSizePx.toDp())) } }
                    Column { repeat(3) { Box(Modifier.size(itemSizePx.toDp())) } }
                }
            }
            rule.onNodeWithTag(targetTag).assertNoOcclusions()
        }

    @Test
    fun notOccluding_whenBranchingParentIsPlacedBeforeWithHigherZIndex() =
        with(rule.density) {
            // Size that forces overlap
            val rootSizePx = 150f
            val itemSizePx = 100f

            rule.setContent {
                PlaceTwoLayoutsApartVert(Modifier.size(rootSizePx.toDp())) {
                    // Branching parent is above its sibling, despite being placed before
                    Column(Modifier.zIndex(1f)) {
                        Box(Modifier.size(itemSizePx.toDp()).testTag(targetTag))
                    }
                    Column(Modifier.zIndex(0f).testTag(occlusionTag)) {
                        Box(Modifier.size(itemSizePx.toDp()).testTag(occlusionTag))
                    }
                }
            }
            rule.onNodeWithTag(targetTag).assertNoOcclusions()
        }

    @Test
    fun occludingSiblings_whenPlacedBefore() =
        with(rule.density) {
            val rootSizePx = 150f
            val itemSizePx = 100f

            rule.setContent {
                PlaceTwoLayoutsApartVert(Modifier.size(rootSizePx.toDp())) {
                    Box(Modifier.size(itemSizePx.toDp()).testTag(targetTag))
                    Box(Modifier.size(itemSizePx.toDp()).testTag(occlusionTag))
                }
            }
            rule
                .onNodeWithTag(targetTag)
                .assertOcclusions(occludingTag = occlusionTag, expectedCount = 1)
        }

    @Test
    fun occludingSiblings_whenPlacedAfterWithLowerZIndex() =
        with(rule.density) {
            val rootSizePx = 150f
            val itemSizePx = 100f

            rule.setContent {
                PlaceTwoLayoutsApartVert(Modifier.size(rootSizePx.toDp())) {
                    Box(Modifier.size(itemSizePx.toDp()).zIndex(1f).testTag(occlusionTag))
                    Box(Modifier.size(itemSizePx.toDp()).zIndex(0f).testTag(targetTag))
                }
            }
            rule
                .onNodeWithTag(targetTag)
                .assertOcclusions(occludingTag = occlusionTag, expectedCount = 1)
        }

    @Test
    fun occludingSiblings_afterRotatingTargetOnZAxis() =
        with(rule.density) {
            val rootSizePx = 200f
            val itemHeightPx = 90f

            var degrees by mutableFloatStateOf(0f)

            rule.setContent {
                Column(
                    modifier = Modifier.size(rootSizePx.toDp()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        Modifier.graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 1f)
                            rotationZ = degrees
                        }
                    ) {
                        Box(Modifier.fillMaxWidth().height(itemHeightPx.toDp()).testTag(targetTag))
                    }
                    Box(Modifier.fillMaxWidth().height(itemHeightPx.toDp()).testTag(occlusionTag))
                }
            }
            rule.onNodeWithTag(targetTag).assertNoOcclusions()

            degrees = 90f
            rule.waitForIdle()

            rule
                .onNodeWithTag(targetTag)
                .assertOcclusions(occludingTag = occlusionTag, expectedCount = 1)
        }

    @Test
    fun occludingTopBar_whenTargetIsScrolled() =
        with(rule.density) {
            // Pick sizes to avoid pixel rounding error
            val topBarHeightPx = 49f
            val topBarComponentWidthPx = 52f

            val columnBoxSizePx = 50f

            val scrollState = ScrollState(0)

            rule.setContent {
                Scaffold(
                    // Top bar root component and one child will occlude target after scroll
                    topBar = {
                        Row(
                            modifier = Modifier.height(topBarHeightPx.toDp()).testTag(occlusionTag),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                Modifier.fillMaxHeight()
                                    .width(topBarComponentWidthPx.toDp())
                                    .testTag(occlusionTag)
                            )

                            // This will not occlude due to the size of the target Box
                            Box(Modifier.fillMaxHeight().width(topBarComponentWidthPx.toDp()))
                        }
                    }
                ) { paddingValues ->
                    Column(
                        modifier =
                            Modifier.padding(paddingValues = paddingValues)
                                // Extra padding to avoid adjacency collision
                                .padding(top = 2f.toDp())
                                // Will only fit one box, so that it can scroll up
                                .size(columnBoxSizePx.toDp())
                                .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(2f.toDp())
                    ) {
                        Box(Modifier.size(columnBoxSizePx.toDp()).testTag(targetTag))
                        Box(Modifier.size(columnBoxSizePx.toDp()))
                    }
                }
            }
            rule.onNodeWithTag(targetTag).assertNoOcclusions()

            rule.runOnIdle { runBlocking { scrollState.scrollBy(columnBoxSizePx) } }
            rule.waitForIdle()

            rule
                .onNodeWithTag(targetTag)
                .assertOcclusions(occludingTag = occlusionTag, expectedCount = 2)
        }

    @Test
    fun occluding_whenBranchingParentIsPlacedAfterWithLowerZIndex() =
        with(rule.density) {
            // Size that forces overlap
            val rootSizePx = 150f
            val itemSizePx = 100f

            rule.setContent {
                PlaceTwoLayoutsApartVert(Modifier.size(rootSizePx.toDp())) {
                    Column(Modifier.zIndex(1f).testTag(occlusionTag)) {
                        Box(Modifier.size(itemSizePx.toDp()).testTag(occlusionTag))
                    }
                    // Branching parent is below its sibling, it should not occlude its own child
                    Column(Modifier.zIndex(0f)) {
                        Box(Modifier.size(itemSizePx.toDp()).testTag(targetTag))
                    }
                }
            }
            rule.waitForIdle()

            rule
                .onNodeWithTag(targetTag)
                .assertOcclusions(occludingTag = occlusionTag, expectedCount = 2)
        }

    @Test
    fun globalLayoutListenerCallback_whenTargetChangesIntoOcclusion() =
        with(rule.density) {
            val rootSizePx = 300f
            val itemSizePx = 100f

            var sizeMultiplier by mutableFloatStateOf(1f)

            var width = -1
            var occlusionCount = -1
            var countDown = CountDownLatch(1)

            rule.setContent {
                PlaceTwoLayoutsApartVert(Modifier.size(rootSizePx.toDp())) {
                    Box(
                        Modifier.testGlobalLayoutListener { rectInfo ->
                                // Check rectInfo is also updated properly
                                width = rectInfo.width
                                occlusionCount = rectInfo.calculateOcclusions().size
                                countDown.countDown()
                            }
                            .size((itemSizePx * sizeMultiplier).toDp())
                    )
                    Box(Modifier.size(itemSizePx.toDp()))
                }
            }

            assertTrue(countDown.await(1, TimeUnit.SECONDS))
            assertEquals(100, width)
            assertEquals(0, occlusionCount)

            countDown = CountDownLatch(1)
            sizeMultiplier = 3f
            rule.waitForIdle()

            assertTrue(countDown.await(1, TimeUnit.SECONDS))
            assertEquals(300, width)
            assertEquals(1, occlusionCount)
        }

    // Tests that changes not related to the target node still trigger a callback
    @Test
    fun globalLayoutListener_whenSiblingChangesIntoOcclusion() =
        with(rule.density) {
            val rootSizePx = 300f
            val itemSizePx = 100f

            var sizeMultiplier by mutableFloatStateOf(1f)

            var occlusionCount = -1
            var countDown = CountDownLatch(1)

            rule.setContent {
                PlaceTwoLayoutsApartVert(Modifier.size(rootSizePx.toDp())) {
                    Box(
                        Modifier.size(itemSizePx.toDp()).testGlobalLayoutListener { rectInfo ->
                            occlusionCount = rectInfo.calculateOcclusions().size
                            countDown.countDown()
                        }
                    )
                    Box(Modifier.size((itemSizePx * sizeMultiplier).toDp()))
                }
            }

            assertTrue(countDown.await(1, TimeUnit.SECONDS))
            assertEquals(0, occlusionCount)

            countDown = CountDownLatch(1)
            // Changes the sibling size, should trigger a callback on the target node too
            sizeMultiplier = 3f
            rule.waitForIdle()

            assertTrue(countDown.await(1, TimeUnit.SECONDS))
            assertEquals(1, occlusionCount)
        }

    @Test
    fun globalLayoutListener_whenParentChangesIntoOcclusion() =
        with(rule.density) {
            val rootSizePx = 300f
            val itemSizePx = 100f

            var sizeMultiplier by mutableFloatStateOf(1f)

            var occlusionCount = -1
            var countDown = CountDownLatch(1)

            rule.setContent {
                PlaceTwoLayoutsApartVert(Modifier.size((rootSizePx * sizeMultiplier).toDp())) {
                    Box(
                        Modifier.size(itemSizePx.toDp()).testGlobalLayoutListener { rectInfo ->
                            occlusionCount = rectInfo.calculateOcclusions().size
                            countDown.countDown()
                        }
                    )
                    Box(Modifier.size(itemSizePx.toDp()))
                }
            }

            assertTrue(countDown.await(1, TimeUnit.SECONDS))
            assertEquals(0, occlusionCount)

            countDown = CountDownLatch(1)
            // Changes the parent size, the new size should case an occlusion between the children,
            // and
            // the callback should reflect on that change
            sizeMultiplier = 1f / 3f
            rule.waitForIdle()

            assertTrue(countDown.await(1, TimeUnit.SECONDS))
            assertEquals(1, occlusionCount)
        }

    @Test
    fun globalLayoutListener_updatedOffset() =
        with(rule.density) {
            val rootSizePx = 300f
            val itemSizePx = 100f

            var sizeMultiplier by mutableFloatStateOf(1f)

            var fromRoot = IntOffset(-1, -1)
            var countDown = CountDownLatch(1)

            rule.setContent {
                Column(
                    modifier = Modifier.size((rootSizePx).toDp()),
                ) {
                    Box(Modifier.size((itemSizePx * sizeMultiplier).toDp()))
                    Box(
                        Modifier.testGlobalLayoutListener { rectInfo ->
                                fromRoot = rectInfo.positionInRoot
                                countDown.countDown()
                            }
                            .size(itemSizePx.toDp())
                    )
                }
            }

            assertTrue(countDown.await(1, TimeUnit.SECONDS))
            // Position should be the size of the first item
            assertEquals(IntOffset(0, (itemSizePx * sizeMultiplier).fastRoundToInt()), fromRoot)

            countDown = CountDownLatch(1)
            sizeMultiplier = 2f
            rule.waitForIdle()

            assertTrue(countDown.await(1, TimeUnit.SECONDS))
            assertEquals(IntOffset(0, (itemSizePx * sizeMultiplier).fastRoundToInt()), fromRoot)
        }

    /** Asserts that there are no occlusions around the current SemanticsNode. */
    private fun SemanticsNodeInteraction.assertNoOcclusions() {
        var callbackCount = 0
        forEachOcclusionValue { _ -> callbackCount++ }
        assertEquals(0, callbackCount)
    }

    /**
     * Asserts expected amount of occlusions around the current SemanticsNode.
     *
     * It also asserts that every Node tagged with [occludingTag] corresponds to the Rects found to
     * be occluding in RectManager.
     */
    private fun SemanticsNodeInteraction.assertOcclusions(
        occludingTag: String,
        expectedCount: Int
    ) {
        val occlusionSet = mutableIntSetOf()
        var callbackCount = 0
        forEachOcclusionValue { value ->
            callbackCount++
            occlusionSet.add(value)
        }
        // Assert that the expected amount of occlusions happened
        assertEquals(expectedCount, callbackCount)
        assertEquals(expectedCount, occlusionSet.size)

        val nodeCollection = rule.onAllNodesWithTag(occludingTag)

        // Assert that every tagged node is an occluding node
        nodeCollection.assertAll(
            SemanticsMatcher("Tagged node is occluding") { node ->
                occlusionSet.contains(node.layoutNode.semanticsId)
            }
        )

        // Assert that all occluding Rect had their Nodes tagged.
        nodeCollection.assertCountEquals(expectedCount)
    }

    private inline fun SemanticsNodeInteraction.forEachOcclusionValue(block: (value: Int) -> Unit) {
        val node = fetchSemanticsNode().layoutNode
        val rectManager = node.requireOwner().rectManager
        val rectList = rectManager.rects
        val id = this.semanticsId()
        val idIndex = rectList.indexOf(id)
        if (idIndex < 0) {
            return
        }
        rectList.forEachIntersectingRectWithValueAt(idIndex) { _, _, _, _, intersectingValue ->
            if (rectManager.isTargetDrawnFirst(id, intersectingValue)) {
                block(intersectingValue)
            }
        }
    }

    private fun Modifier.testGlobalLayoutListener(callback: (RelativeLayoutBounds) -> Unit) =
        this then
            OnGlobaLayoutListenerElement(
                throttleMillis = 0,
                debounceMillis = 0,
                callback = callback
            )
}

/**
 * Similar to Box when child0 is top aligned and child1 bottom aligned, but helps keeping the outer
 * modifier clear.
 */
@Composable
private fun PlaceTwoLayoutsApartVert(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        measurePolicy = { measurables, constraints ->
            require(constraints.hasFixedHeight)
            require(measurables.size == 2)

            val placeable0 = measurables[0].measure(Constraints())
            val placeable1 = measurables[1].measure(Constraints())
            val wrapWidth = maxOf(placeable0.width, placeable1.width)

            val width =
                if (constraints.hasFixedWidth) {
                    constraints.maxWidth
                } else if (constraints.hasBoundedWidth) {
                    wrapWidth.coerceAtMost(constraints.maxWidth)
                } else {
                    maxOf(wrapWidth, constraints.minWidth)
                }

            layout(width, constraints.maxHeight) {
                placeable0.place(0, 0)
                placeable1.place(0, constraints.maxHeight - placeable1.height)
            }
        },
        modifier = modifier,
        content = content
    )
}

private data class OnGlobaLayoutListenerElement(
    val throttleMillis: Long,
    val debounceMillis: Long,
    val callback: (RelativeLayoutBounds) -> Unit,
) : ModifierNodeElement<OnGlobalLayoutListenerNode>() {
    override fun create(): OnGlobalLayoutListenerNode =
        OnGlobalLayoutListenerNode(
            throttleMillis = throttleMillis,
            debounceMillis = debounceMillis,
            callback = callback
        )

    override fun update(node: OnGlobalLayoutListenerNode) {
        node.throttleMillis = throttleMillis
        node.debounceMillis = debounceMillis
        node.callback = callback
        node.diposeAndRegister()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "onLayoutCalculatorChanged"
        properties["throttleMillis"] = throttleMillis
        properties["debounceMillis"] = debounceMillis
        properties["callback"] = callback
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as OnGlobaLayoutListenerElement

        if (throttleMillis != other.throttleMillis) return false
        if (debounceMillis != other.debounceMillis) return false
        if (callback != other.callback) return false

        return true
    }

    override fun hashCode(): Int {
        var result = throttleMillis.hashCode()
        result = 31 * result + debounceMillis.hashCode()
        result = 31 * result + callback.hashCode()
        return result
    }
}

private class OnGlobalLayoutListenerNode(
    var throttleMillis: Long,
    var debounceMillis: Long,
    var callback: (RelativeLayoutBounds) -> Unit,
) : Modifier.Node() {
    var handle: RegistrationHandle? = null

    fun diposeAndRegister() {
        handle?.unregister()
        handle = registerOnGlobalLayoutListener(throttleMillis, debounceMillis, callback)
    }

    override fun onAttach() {
        diposeAndRegister()
    }

    override fun onDetach() {
        handle?.unregister()
    }
}
