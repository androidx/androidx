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

package androidx.compose.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.elementFor
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class OnSizeChangedTest {

    @get:Rule val rule = createAndroidComposeRule<TestActivity>(StandardTestDispatcher())
    private lateinit var activity: TestActivity

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
    }

    @Test
    @SmallTest
    fun normalSizeChange() {
        var changedSize = IntSize.Zero
        var sizePx by mutableStateOf(10)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.padding(10.toDp()).onSizeChanged { changedSize = it }) {
                    Box(Modifier.requiredSize(sizePx.toDp()))
                }
            }
        }

        // Initial setting will call onSizeChanged
        rule.waitForIdle()
        assertEquals(10, changedSize.height)
        assertEquals(10, changedSize.width)

        sizePx = 20

        // We've changed the size of the contents, so we should receive a onSizeChanged call
        rule.waitForIdle()
        assertEquals(20, changedSize.height)
        assertEquals(20, changedSize.width)
    }

    @Test
    @SmallTest
    fun internalSizeChange() {
        var changedSize = IntSize.Zero
        var sizePx by mutableStateOf(10)

        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.padding(10.toDp())
                        .onSizeChanged { changedSize = it }
                        .padding(sizePx.toDp())
                ) {
                    Box(Modifier.requiredSize(10.toDp()))
                }
            }
        }

        // Initial setting will call onSizeChanged
        rule.waitForIdle()
        assertEquals(30, changedSize.height)
        assertEquals(30, changedSize.width)

        sizePx = 20

        // We've changed the size of the contents, so we should receive a onSizeChanged call
        rule.waitForIdle()
        assertEquals(50, changedSize.height)
        assertEquals(50, changedSize.width)
    }

    @Test
    fun onlyInnerSizeChange() {
        var changedSize = IntSize.Zero
        var sizePx by mutableStateOf(10)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.padding(sizePx.toDp()).onSizeChanged { changedSize = it }) {
                    Box(Modifier.requiredSize(10.toDp()))
                }
            }
        }

        // Initial setting will call onSizeChanged
        rule.waitForIdle()
        assertEquals(10, changedSize.height)
        assertEquals(10, changedSize.width)

        sizePx = 5

        // We've changed the padding, but the size of the contents didn't change
        rule.waitForIdle()
        assertEquals(10, changedSize.height)
        assertEquals(10, changedSize.width)
    }

    @Test
    fun layoutButNoSizeChange() {
        var changedSize = IntSize.Zero
        var sizePx by mutableStateOf(10)
        var called = false

        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.padding(10.toDp()).onSizeChanged {
                        changedSize = it
                        called = true
                    }
                ) {
                    Box(
                        Modifier.layout { measurable, _ ->
                            val placeable = measurable.measure(Constraints.fixed(sizePx, sizePx))
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        }
                    )
                }
            }
        }

        // Initial setting will call onSizeChanged
        rule.waitForIdle()
        assertEquals(10, changedSize.height)
        assertEquals(10, changedSize.width)

        called = false
        rule.runOnUiThread {
            sizePx = 20
            sizePx = 10
        }

        // We've triggered a layout, but the size didn't change.
        rule.waitForIdle()
        assertFalse(called)
    }

    @Test
    @MediumTest
    fun addedModifier() {
        var changedSize1 = IntSize.Zero
        var changedSize2 = IntSize.Zero
        var addModifier by mutableStateOf(false)
        var called2 = false

        rule.setContent {
            with(LocalDensity.current) {
                // Remember lambdas to avoid triggering a node update when the lambda changes
                val mod =
                    if (addModifier)
                        Modifier.onSizeChanged(
                            remember {
                                {
                                    changedSize2 = it
                                    called2 = true
                                }
                            }
                        )
                    else Modifier
                Box(
                    // Remember lambdas to avoid triggering a node update when the lambda
                    // changes
                    Modifier.padding(10.toDp())
                        .onSizeChanged(remember { { changedSize1 = it } })
                        .then(mod)
                ) {
                    Box(Modifier.requiredSize(10.toDp()))
                }
            }
        }

        // Initial setting will call onSizeChanged
        rule.waitForIdle()
        assertEquals(10, changedSize1.height)
        assertEquals(10, changedSize1.width)

        addModifier = true

        // We've added an onSizeChanged modifier, so it must trigger another size change.
        rule.waitForIdle()
        assertTrue(called2)
        assertEquals(10, changedSize2.height)
        assertEquals(10, changedSize2.width)
    }

    @Test
    @MediumTest
    fun addedModifierNode() {
        var changedSize1 = IntSize.Zero
        var changedSize2 = IntSize.Zero
        var addModifier by mutableStateOf(false)
        var onRemeasuredCalled1 = false
        var onRemeasuredCalled2 = false
        var onPlacedCalled1 = false
        var onPlacedCalled2 = false

        val node =
            object : LayoutAwareModifierNode, Modifier.Node() {
                override fun onRemeasured(size: IntSize) {
                    changedSize1 = size
                    onRemeasuredCalled1 = true
                }

                override fun onPlaced(coordinates: LayoutCoordinates) {
                    onPlacedCalled1 = true
                }
            }

        val node2 =
            object : LayoutAwareModifierNode, Modifier.Node() {
                override fun onRemeasured(size: IntSize) {
                    changedSize2 = size
                    onRemeasuredCalled2 = true
                }

                override fun onPlaced(coordinates: LayoutCoordinates) {
                    onPlacedCalled2 = true
                }
            }

        rule.setContent {
            with(LocalDensity.current) {
                val mod = if (addModifier) Modifier.elementFor(node2) else Modifier
                Box(Modifier.padding(10.toDp()).elementFor(node).then(mod)) {
                    Box(Modifier.requiredSize(10.toDp()))
                }
            }
        }

        // Initial setting will call onRemeasured and onPlaced
        rule.waitForIdle()
        assertTrue(onRemeasuredCalled1)
        assertTrue(onPlacedCalled1)
        assertEquals(10, changedSize1.height)
        assertEquals(10, changedSize1.width)

        onRemeasuredCalled1 = false
        onPlacedCalled1 = false
        addModifier = true

        // We've added a node, so it must trigger onRemeasured and onPlaced on the new node, and
        // the old node should see a relayout too
        rule.waitForIdle()
        assertTrue(onRemeasuredCalled1)
        assertTrue(onPlacedCalled1)
        assertTrue(onRemeasuredCalled2)
        assertTrue(onPlacedCalled2)
        assertEquals(10, changedSize1.height)
        assertEquals(10, changedSize1.width)
        assertEquals(10, changedSize2.height)
        assertEquals(10, changedSize2.width)
    }

    @Test
    @MediumTest
    fun removedModifier() {
        var changedSize1 = IntSize.Zero
        var changedSize2 = IntSize.Zero
        var addModifier by mutableStateOf(true)
        var called1 = false
        var called2 = false

        rule.setContent {
            with(LocalDensity.current) {
                // Remember lambdas to avoid triggering a node update when the lambda changes
                val mod =
                    if (addModifier)
                        Modifier.onSizeChanged(
                            remember {
                                {
                                    changedSize2 = it
                                    called2 = true
                                }
                            }
                        )
                    else Modifier
                Box(
                    // Remember lambdas to avoid triggering a node update when the lambda
                    // changes
                    Modifier.padding(10.toDp())
                        .onSizeChanged(
                            remember {
                                {
                                    changedSize1 = it
                                    called1 = true
                                }
                            }
                        )
                        .then(mod)
                ) {
                    Box(Modifier.requiredSize(10.toDp()))
                }
            }
        }

        // Initial setting will call onSizeChanged
        rule.waitForIdle()
        assertTrue(called1)
        assertTrue(called2)
        assertEquals(10, changedSize1.height)
        assertEquals(10, changedSize1.width)
        assertEquals(10, changedSize2.height)
        assertEquals(10, changedSize2.width)

        called1 = false
        // Remove the modifier
        addModifier = false

        // We've removed a modifier, so the other modifier should not be informed since there was no
        // layout change.
        rule.waitForIdle()
        assertFalse(called1)
    }

    @Test
    @MediumTest
    fun removedModifierNode() {
        var changedSize1 = IntSize.Zero
        var changedSize2 = IntSize.Zero
        var addModifier by mutableStateOf(true)
        var onRemeasuredCalled1 = 0
        var onRemeasuredCalled2 = 0
        var onPlacedCalled1 = 0
        var onPlacedCalled2 = 0

        val node =
            object : LayoutAwareModifierNode, Modifier.Node() {
                override fun onRemeasured(size: IntSize) {
                    changedSize1 = size
                    onRemeasuredCalled1++
                }

                override fun onPlaced(coordinates: LayoutCoordinates) {
                    onPlacedCalled1++
                }
            }

        val node2 =
            object : LayoutAwareModifierNode, Modifier.Node() {
                override fun onRemeasured(size: IntSize) {
                    changedSize2 = size
                    onRemeasuredCalled2++
                }

                override fun onPlaced(coordinates: LayoutCoordinates) {
                    onPlacedCalled2++
                }
            }

        rule.setContent {
            with(LocalDensity.current) {
                val mod = if (addModifier) Modifier.elementFor(node2) else Modifier
                Box(Modifier.padding(10.toDp()).elementFor(node).then(mod)) {
                    Box(Modifier.requiredSize(10.toDp()))
                }
            }
        }

        // Initial setting will call onRemeasured and onPlaced for both
        rule.waitForIdle()
        assertEquals(1, onRemeasuredCalled1)
        assertEquals(1, onPlacedCalled1)
        assertEquals(1, onRemeasuredCalled2)
        assertEquals(1, onPlacedCalled2)
        assertEquals(10, changedSize1.height)
        assertEquals(10, changedSize1.width)
        assertEquals(10, changedSize2.height)
        assertEquals(10, changedSize2.width)

        onRemeasuredCalled1 = 0
        onPlacedCalled1 = 0
        // Remove the modifier node
        addModifier = false

        // We've removed a node, so the other node should not be informed since there was no layout
        // change
        rule.waitForIdle()
        assertEquals(0, onRemeasuredCalled1)
        assertEquals(0, onPlacedCalled1)
    }

    @Test
    @MediumTest
    fun updatedModifierLambda() {
        var changedSize1 = IntSize.Zero
        var changedSize2 = IntSize.Zero
        var called1 = false
        var called2 = false

        var lambda1: (IntSize) -> Unit by
            mutableStateOf({
                changedSize1 = it
                called1 = true
            })

        // Stable lambda so that this one won't change while we change lambda1
        val lambda2: (IntSize) -> Unit = {
            changedSize2 = it
            called2 = true
        }

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.padding(10.toDp()).onSizeChanged(lambda1).onSizeChanged(lambda2)) {
                    Box(Modifier.requiredSize(10.toDp()))
                }
            }
        }

        // Initial setting will call onSizeChanged
        rule.waitForIdle()
        assertTrue(called1)
        assertTrue(called2)
        assertEquals(10, changedSize1.height)
        assertEquals(10, changedSize1.width)
        assertEquals(10, changedSize2.height)
        assertEquals(10, changedSize2.width)

        var newCalled = false
        // Change lambda instance, this should cause us to invalidate and invoke callbacks again
        lambda1 = {
            changedSize1 = it
            newCalled = true
        }

        // We updated the lambda on the first item, so the new lambda should be called
        rule.waitForIdle()
        assertTrue(newCalled)
        assertEquals(10, changedSize1.height)
        assertEquals(10, changedSize1.width)
    }

    @Test
    @MediumTest
    fun updatedModifierNode() {
        var changedSize1 = IntSize.Zero
        var changedSize2 = IntSize.Zero
        var onRemeasuredCalled1 = false
        var onRemeasuredCalled2 = false
        var onPlacedCalled1 = false
        var onPlacedCalled2 = false

        var onRemeasuredLambda: (IntSize) -> Unit by
            mutableStateOf({
                changedSize1 = it
                onRemeasuredCalled1 = true
            })

        var onPlacedLambda: (LayoutCoordinates) -> Unit by
            mutableStateOf({ onPlacedCalled1 = true })

        class Node1(
            var onRemeasuredLambda: (IntSize) -> Unit,
            var onPlacedLambda: (LayoutCoordinates) -> Unit,
        ) : LayoutAwareModifierNode, Modifier.Node() {
            // We are testing auto invalidation behavior here
            override val shouldAutoInvalidate = true

            override fun onRemeasured(size: IntSize) {
                onRemeasuredLambda(size)
            }

            override fun onPlaced(coordinates: LayoutCoordinates) {
                onPlacedLambda(coordinates)
            }
        }

        class Node1Element(
            private var onRemeasured: (IntSize) -> Unit,
            private var onPlaced: (LayoutCoordinates) -> Unit,
        ) : ModifierNodeElement<Node1>() {
            override fun create(): Node1 {
                return Node1(onRemeasured, onPlaced)
            }

            override fun update(node: Node1) {
                node.onRemeasuredLambda = onRemeasured
                node.onPlacedLambda = onPlaced
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Node1Element) return false

                if (onRemeasured != other.onRemeasured) return false
                if (onPlaced != other.onPlaced) return false

                return true
            }

            override fun hashCode(): Int {
                var result = onRemeasured.hashCode()
                result = 31 * result + onPlaced.hashCode()
                return result
            }
        }

        val node2 =
            object : LayoutAwareModifierNode, Modifier.Node() {
                override fun onRemeasured(size: IntSize) {
                    changedSize2 = size
                    onRemeasuredCalled2 = true
                }

                override fun onPlaced(coordinates: LayoutCoordinates) {
                    onPlacedCalled2 = true
                }
            }

        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.padding(10.toDp())
                        .then(Node1Element(onRemeasuredLambda, onPlacedLambda))
                        .elementFor(node2)
                ) {
                    Box(Modifier.requiredSize(10.toDp()))
                }
            }
        }

        // Initial setting will call onSizeChanged
        rule.waitForIdle()
        assertTrue(onRemeasuredCalled1)
        assertTrue(onPlacedCalled1)
        assertTrue(onRemeasuredCalled2)
        assertTrue(onPlacedCalled2)
        assertEquals(10, changedSize1.height)
        assertEquals(10, changedSize1.width)
        assertEquals(10, changedSize2.height)
        assertEquals(10, changedSize2.width)

        onRemeasuredCalled2 = false
        onPlacedCalled2 = false
        var newRemeasuredCalled = false
        var newPlacedCalled = false
        // Change lambda instance, this should cause us to autoinvalidate and invoke callbacks again
        onRemeasuredLambda = {
            changedSize1 = it
            newRemeasuredCalled = true
        }
        onPlacedLambda = { newPlacedCalled = true }

        // We updated the lambda on the first item, so the new lambda should be called
        rule.waitForIdle()
        assertTrue(newRemeasuredCalled)
        assertTrue(newPlacedCalled)
        // Currently updating causes a relayout, so the existing node should also be invoked.
        assertTrue(onRemeasuredCalled2)
        assertTrue(onPlacedCalled2)
        assertEquals(10, changedSize1.height)
        assertEquals(10, changedSize1.width)
        assertEquals(10, changedSize2.height)
        assertEquals(10, changedSize2.width)
    }

    @Test
    @SmallTest
    fun lazilyDelegatedModifierNode() {
        var changedSize1 = IntSize.Zero
        var changedSize2 = IntSize.Zero
        var onRemeasuredCalled1 = false
        var onRemeasuredCalled2 = false
        var onPlacedCalled1 = false
        var onPlacedCalled2 = false

        val node =
            object : LayoutAwareModifierNode, Modifier.Node() {
                override fun onRemeasured(size: IntSize) {
                    changedSize1 = size
                    onRemeasuredCalled1 = true
                }

                override fun onPlaced(coordinates: LayoutCoordinates) {
                    onPlacedCalled1 = true
                }
            }

        val node2 =
            object : DelegatingNode() {
                fun addDelegate() {
                    delegate(
                        object : LayoutAwareModifierNode, Modifier.Node() {
                            override fun onRemeasured(size: IntSize) {
                                changedSize2 = size
                                onRemeasuredCalled2 = true
                            }

                            override fun onPlaced(coordinates: LayoutCoordinates) {
                                onPlacedCalled2 = true
                            }
                        }
                    )
                }
            }

        rule.setContent {
            with(LocalDensity.current) {
                val mod = Modifier.elementFor(node2)
                Box(Modifier.padding(10.toDp()).elementFor(node).then(mod)) {
                    Box(Modifier.requiredSize(10.toDp()))
                }
            }
        }

        // Initial setting will call onRemeasured and onPlaced
        rule.waitForIdle()
        assertTrue(onRemeasuredCalled1)
        assertTrue(onPlacedCalled1)
        assertEquals(10, changedSize1.height)
        assertEquals(10, changedSize1.width)

        rule.runOnUiThread { node2.addDelegate() }

        // We've delegated to a node, so it must trigger onRemeasured and onPlaced on the new node
        rule.waitForIdle()
        assertTrue(onRemeasuredCalled2)
        assertTrue(onPlacedCalled2)
        assertEquals(10, changedSize2.height)
        assertEquals(10, changedSize2.width)
    }

    @Test
    @SmallTest
    fun modifierIsReturningEqualObjectForTheSameLambda() {
        val lambda: (IntSize) -> Unit = {}
        assertEquals(Modifier.onSizeChanged(lambda), Modifier.onSizeChanged(lambda))
    }

    @Test
    @SmallTest
    fun modifierIsReturningNotEqualObjectForDifferentLambdas() {
        val lambda1: (IntSize) -> Unit = { it.height }
        val lambda2: (IntSize) -> Unit = { it.width }
        assertNotEquals(Modifier.onSizeChanged(lambda1), Modifier.onSizeChanged(lambda2))
    }

    @Test
    @SmallTest
    fun delegatedSizeChanged() {
        var changedSize = IntSize.Zero
        var sizePx by mutableStateOf(10)
        var called = false
        val node =
            object : DelegatingNode() {
                val osc =
                    delegate(
                        object : LayoutAwareModifierNode, Modifier.Node() {
                            override fun onRemeasured(size: IntSize) {
                                changedSize = size
                                called = true
                            }
                        }
                    )
            }

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.padding(10.toDp()).elementFor(node)) {
                    Box(Modifier.requiredSize(sizePx.toDp()))
                }
            }
        }

        // Initial setting will call onSizeChanged
        rule.waitForIdle()
        assertTrue(called)
        assertEquals(10, changedSize.height)
        assertEquals(10, changedSize.width)

        called = false
        sizePx = 20

        // We've changed the size of the contents, so we should receive a onSizeChanged call
        rule.waitForIdle()
        assertTrue(called)
        assertEquals(20, changedSize.height)
        assertEquals(20, changedSize.width)
    }

    @Test
    @SmallTest
    fun multipleDelegatedSizeChanged() {
        var changedSize1 = IntSize.Zero
        var changedSize2 = IntSize.Zero
        var sizePx by mutableStateOf(10)
        var called1 = false
        var called2 = false
        val node =
            object : DelegatingNode() {
                val a =
                    delegate(
                        object : LayoutAwareModifierNode, Modifier.Node() {
                            override fun onRemeasured(size: IntSize) {
                                changedSize1 = size
                                called1 = true
                            }
                        }
                    )
                val b =
                    delegate(
                        object : LayoutAwareModifierNode, Modifier.Node() {
                            override fun onRemeasured(size: IntSize) {
                                changedSize2 = size
                                called2 = true
                            }
                        }
                    )
            }

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.padding(10.toDp()).elementFor(node)) {
                    Box(Modifier.requiredSize(sizePx.toDp()))
                }
            }
        }

        // Initial setting will call onSizeChanged
        rule.waitForIdle()
        assertTrue(called1)
        assertTrue(called2)
        assertEquals(10, changedSize1.height)
        assertEquals(10, changedSize1.width)
        assertEquals(10, changedSize2.height)
        assertEquals(10, changedSize2.width)

        called1 = false
        called2 = false
        sizePx = 20

        // We've changed the size of the contents, so we should receive a onSizeChanged call
        rule.waitForIdle()
        assertTrue(called1)
        assertTrue(called2)
        assertEquals(20, changedSize1.height)
        assertEquals(20, changedSize1.width)
        assertEquals(20, changedSize2.height)
        assertEquals(20, changedSize2.width)
    }

    @Test
    @SmallTest
    fun multipleDelegatedOnPlaced() {
        var paddingDp by mutableStateOf(10)
        var placedCalled1 = 0
        var placedCalled2 = 0
        val node =
            object : DelegatingNode() {
                val a =
                    delegate(
                        object : LayoutAwareModifierNode, Modifier.Node() {
                            override fun onPlaced(coordinates: LayoutCoordinates) {
                                placedCalled1++
                            }
                        }
                    )
                val b =
                    delegate(
                        object : LayoutAwareModifierNode, Modifier.Node() {
                            override fun onPlaced(coordinates: LayoutCoordinates) {
                                placedCalled2++
                            }
                        }
                    )
            }

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.padding(paddingDp.toDp()).elementFor(node)) {
                    Box(Modifier.requiredSize(10.dp))
                }
            }
        }

        // Initial setting will call onSizeChanged
        rule.waitForIdle()
        assertEquals(1, placedCalled1)
        assertEquals(1, placedCalled2)

        placedCalled1 = 0
        placedCalled2 = 0
        paddingDp = 20

        // We've changed the size of the contents, so we should receive a onSizeChanged call
        rule.waitForIdle()
        assertEquals(1, placedCalled1)
        assertEquals(1, placedCalled2)
    }
}
