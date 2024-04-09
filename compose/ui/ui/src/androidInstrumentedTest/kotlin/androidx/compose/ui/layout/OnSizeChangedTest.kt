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

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.elementFor
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
    }

    @Test
    @SmallTest
    fun normalSizeChange() {
        var latch = CountDownLatch(1)
        var changedSize = IntSize.Zero
        var sizePx by mutableStateOf(10)

        rule.runOnUiThread {
            activity.setContent {
                with(LocalDensity.current) {
                    Box(
                        Modifier.padding(10.toDp()).onSizeChanged {
                            changedSize = it
                            latch.countDown()
                        }
                    ) {
                        Box(Modifier.requiredSize(sizePx.toDp()))
                    }
                }
            }
        }

        // Initial setting will call onSizeChanged
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(10, changedSize.height)
        assertEquals(10, changedSize.width)

        latch = CountDownLatch(1)
        sizePx = 20

        // We've changed the size of the contents, so we should receive a onSizeChanged call
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(20, changedSize.height)
        assertEquals(20, changedSize.width)
    }

    @Test
    @SmallTest
    fun internalSizeChange() {
        var latch = CountDownLatch(1)
        var changedSize = IntSize.Zero
        var sizePx by mutableStateOf(10)

        rule.runOnUiThread {
            activity.setContent {
                with(LocalDensity.current) {
                    Box(
                        Modifier.padding(10.toDp())
                            .onSizeChanged {
                                changedSize = it
                                latch.countDown()
                            }.padding(sizePx.toDp())
                    ) {
                        Box(Modifier.requiredSize(10.toDp()))
                    }
                }
            }
        }

        // Initial setting will call onSizeChanged
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(30, changedSize.height)
        assertEquals(30, changedSize.width)

        latch = CountDownLatch(1)
        sizePx = 20

        // We've changed the size of the contents, so we should receive a onSizeChanged call
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(50, changedSize.height)
        assertEquals(50, changedSize.width)
    }

    @Test
    fun onlyInnerSizeChange() {
        var latch = CountDownLatch(1)
        var changedSize = IntSize.Zero
        var sizePx by mutableStateOf(10)

        rule.runOnUiThread {
            activity.setContent {
                with(LocalDensity.current) {
                    Box(
                        Modifier.padding(sizePx.toDp()).onSizeChanged {
                            changedSize = it
                            latch.countDown()
                        }
                    ) {
                        Box(Modifier.requiredSize(10.toDp()))
                    }
                }
            }
        }

        // Initial setting will call onSizeChanged
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(10, changedSize.height)
        assertEquals(10, changedSize.width)

        latch = CountDownLatch(1)
        sizePx = 5

        assertTrue(latch.await(500, TimeUnit.MILLISECONDS))
        // We've changed the padding, but the size of the contents didn't change
        assertEquals(10, changedSize.height)
        assertEquals(10, changedSize.width)
    }

    @Test
    fun layoutButNoSizeChange() {
        var latch = CountDownLatch(1)
        var changedSize = IntSize.Zero
        var sizePx by mutableStateOf(10)

        rule.runOnUiThread {
            activity.setContent {
                with(LocalDensity.current) {
                    Box(
                        Modifier.padding(10.toDp()).onSizeChanged {
                            changedSize = it
                            latch.countDown()
                        }
                    ) {
                        Box(
                            Modifier.layout { measurable, _ ->
                                val placeable =
                                    measurable.measure(Constraints.fixed(sizePx, sizePx))
                                layout(placeable.width, placeable.height) {
                                    placeable.place(0, 0)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Initial setting will call onSizeChanged
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(10, changedSize.height)
        assertEquals(10, changedSize.width)

        latch = CountDownLatch(1)
        rule.runOnUiThread {
            sizePx = 20
            sizePx = 10
        }

        // We've triggered a layout, but the size didn't change.
        assertFalse(latch.await(500, TimeUnit.MILLISECONDS))
    }

    @Test
    @SmallTest
    fun addedModifier() {
        val latch1 = CountDownLatch(1)
        val latch2 = CountDownLatch(1)
        var changedSize1 = IntSize.Zero
        var changedSize2 = IntSize.Zero
        var addModifier by mutableStateOf(false)

        rule.runOnUiThread {
            activity.setContent {
                with(LocalDensity.current) {
                    val mod = if (addModifier) Modifier.onSizeChanged {
                        changedSize2 = it
                        latch2.countDown()
                    } else Modifier
                    Box(
                        Modifier.padding(10.toDp()).onSizeChanged {
                            changedSize1 = it
                            latch1.countDown()
                        }.then(mod)
                    ) {
                        Box(Modifier.requiredSize(10.toDp()))
                    }
                }
            }
        }

        // Initial setting will call onSizeChanged
        assertTrue(latch1.await(1, TimeUnit.SECONDS))
        assertEquals(10, changedSize1.height)
        assertEquals(10, changedSize1.width)

        addModifier = true

        // We've added an onSizeChanged modifier, so it must trigger another size change
        assertTrue(latch2.await(1, TimeUnit.SECONDS))
        assertEquals(10, changedSize2.height)
        assertEquals(10, changedSize2.width)
    }

    @Test
    @SmallTest
    fun addedModifierNode() {
        val sizeLatch1 = CountDownLatch(1)
        val sizeLatch2 = CountDownLatch(1)
        val placedLatch1 = CountDownLatch(1)
        val placedLatch2 = CountDownLatch(1)
        var changedSize1 = IntSize.Zero
        var changedSize2 = IntSize.Zero
        var addModifier by mutableStateOf(false)

        val node = object : LayoutAwareModifierNode, Modifier.Node() {
            override fun onRemeasured(size: IntSize) {
                changedSize1 = size
                sizeLatch1.countDown()
            }
            override fun onPlaced(coordinates: LayoutCoordinates) {
                placedLatch1.countDown()
            }
        }

        val node2 = object : LayoutAwareModifierNode, Modifier.Node() {
            override fun onRemeasured(size: IntSize) {
                changedSize2 = size
                sizeLatch2.countDown()
            }
            override fun onPlaced(coordinates: LayoutCoordinates) {
                placedLatch2.countDown()
            }
        }

        rule.runOnUiThread {
            activity.setContent {
                with(LocalDensity.current) {
                    val mod = if (addModifier) Modifier.elementFor(node2) else Modifier
                    Box(
                        Modifier.padding(10.toDp()).elementFor(node).then(mod)
                    ) {
                        Box(Modifier.requiredSize(10.toDp()))
                    }
                }
            }
        }

        // Initial setting will call onRemeasured and onPlaced
        assertTrue(sizeLatch1.await(1, TimeUnit.SECONDS))
        assertTrue(placedLatch1.await(1, TimeUnit.SECONDS))
        assertEquals(10, changedSize1.height)
        assertEquals(10, changedSize1.width)

        addModifier = true

        // We've added a node, so it must trigger onRemeasured and onPlaced on the new node
        assertTrue(sizeLatch2.await(1, TimeUnit.SECONDS))
        assertTrue(placedLatch2.await(1, TimeUnit.SECONDS))
        assertEquals(10, changedSize2.height)
        assertEquals(10, changedSize2.width)
    }

    @Test
    @SmallTest
    fun lazilyDelegatedModifierNode() {
        val sizeLatch1 = CountDownLatch(1)
        val sizeLatch2 = CountDownLatch(1)
        val placedLatch1 = CountDownLatch(1)
        val placedLatch2 = CountDownLatch(1)
        var changedSize1 = IntSize.Zero
        var changedSize2 = IntSize.Zero

        val node = object : LayoutAwareModifierNode, Modifier.Node() {
            override fun onRemeasured(size: IntSize) {
                changedSize1 = size
                sizeLatch1.countDown()
            }

            override fun onPlaced(coordinates: LayoutCoordinates) {
                placedLatch1.countDown()
            }
        }

        val node2 = object : DelegatingNode() {
            fun addDelegate() {
                delegate(
                    object : LayoutAwareModifierNode, Modifier.Node() {
                        override fun onRemeasured(size: IntSize) {
                            changedSize2 = size
                            sizeLatch2.countDown()
                        }

                        override fun onPlaced(coordinates: LayoutCoordinates) {
                            placedLatch2.countDown()
                        }
                    }
                )
            }
        }

        rule.runOnUiThread {
            activity.setContent {
                with(LocalDensity.current) {
                    val mod = Modifier.elementFor(node2)
                    Box(
                        Modifier.padding(10.toDp()).elementFor(node).then(mod)
                    ) {
                        Box(Modifier.requiredSize(10.toDp()))
                    }
                }
            }
        }

        // Initial setting will call onRemeasured and onPlaced
        assertTrue(sizeLatch1.await(1, TimeUnit.SECONDS))
        assertTrue(placedLatch1.await(1, TimeUnit.SECONDS))
        assertEquals(10, changedSize1.height)
        assertEquals(10, changedSize1.width)

        rule.runOnUiThread {
            node2.addDelegate()
        }

        // We've delegated to a node, so it must trigger onRemeasured and onPlaced on the new node
        assertTrue(sizeLatch2.await(1, TimeUnit.SECONDS))
        assertTrue(placedLatch2.await(1, TimeUnit.SECONDS))
        assertEquals(10, changedSize2.height)
        assertEquals(10, changedSize2.width)
    }

    @Test
    @SmallTest
    fun modifierIsReturningEqualObjectForTheSameLambda() {
        val lambda: (IntSize) -> Unit = { }
        assertEquals(Modifier.onSizeChanged(lambda), Modifier.onSizeChanged(lambda))
    }

    @Test
    @SmallTest
    fun modifierIsReturningNotEqualObjectForDifferentLambdas() {
        val lambda1: (IntSize) -> Unit = {
            it.height
        }
        val lambda2: (IntSize) -> Unit = {
            it.width
        }
        assertNotEquals(Modifier.onSizeChanged(lambda1), Modifier.onSizeChanged(lambda2))
    }

    @Test
    @SmallTest
    fun delegatedSizeChanged() {
        var latch = CountDownLatch(1)
        var changedSize = IntSize.Zero
        var sizePx by mutableStateOf(10)
        val node = object : DelegatingNode() {
            val osc = delegate(
                object : LayoutAwareModifierNode, Modifier.Node() {
                    override fun onRemeasured(size: IntSize) {
                        changedSize = size
                        latch.countDown()
                    }
                }
            )
        }

        rule.runOnUiThread {
            activity.setContent {
                with(LocalDensity.current) {
                    Box(
                        Modifier.padding(10.toDp()).elementFor(node)
                    ) {
                        Box(Modifier.requiredSize(sizePx.toDp()))
                    }
                }
            }
        }

        // Initial setting will call onSizeChanged
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(10, changedSize.height)
        assertEquals(10, changedSize.width)

        latch = CountDownLatch(1)
        sizePx = 20

        // We've changed the size of the contents, so we should receive a onSizeChanged call
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(20, changedSize.height)
        assertEquals(20, changedSize.width)
    }

    @Test
    @SmallTest
    fun multipleDelegatedSizeChanged() {
        var latch = CountDownLatch(2)
        var changedSize1 = IntSize.Zero
        var changedSize2 = IntSize.Zero
        var sizePx by mutableStateOf(10)
        val node = object : DelegatingNode() {
            val a = delegate(
                object : LayoutAwareModifierNode, Modifier.Node() {
                    override fun onRemeasured(size: IntSize) {
                        changedSize1 = size
                        latch.countDown()
                    }
                }
            )
            val b = delegate(
                object : LayoutAwareModifierNode, Modifier.Node() {
                    override fun onRemeasured(size: IntSize) {
                        changedSize2 = size
                        latch.countDown()
                    }
                }
            )
        }

        rule.runOnUiThread {
            activity.setContent {
                with(LocalDensity.current) {
                    Box(
                        Modifier.padding(10.toDp()).elementFor(node)
                    ) {
                        Box(Modifier.requiredSize(sizePx.toDp()))
                    }
                }
            }
        }

        // Initial setting will call onSizeChanged
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(10, changedSize1.height)
        assertEquals(10, changedSize1.width)
        assertEquals(10, changedSize2.height)
        assertEquals(10, changedSize2.width)

        latch = CountDownLatch(2)
        sizePx = 20

        // We've changed the size of the contents, so we should receive a onSizeChanged call
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(20, changedSize1.height)
        assertEquals(20, changedSize1.width)
        assertEquals(20, changedSize2.height)
        assertEquals(20, changedSize2.width)
    }

    @Test
    @SmallTest
    fun multipleDelegatedOnPlaced() {
        var latch = CountDownLatch(2)
        var paddingDp by mutableStateOf(10)
        val node = object : DelegatingNode() {
            val a = delegate(
                object : LayoutAwareModifierNode, Modifier.Node() {
                    override fun onPlaced(coordinates: LayoutCoordinates) {
                        latch.countDown()
                    }
                }
            )
            val b = delegate(
                object : LayoutAwareModifierNode, Modifier.Node() {
                    override fun onPlaced(coordinates: LayoutCoordinates) {
                        latch.countDown()
                    }
                }
            )
        }

        rule.runOnUiThread {
            activity.setContent {
                with(LocalDensity.current) {
                    Box(
                        Modifier.padding(paddingDp.toDp()).elementFor(node)
                    ) {
                        Box(Modifier.requiredSize(10.dp))
                    }
                }
            }
        }

        // Initial setting will call onSizeChanged
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(2)
        paddingDp = 20

        // We've changed the size of the contents, so we should receive a onSizeChanged call
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }
}
