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
package androidx.compose.ui.layout

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class OnVisibilityChangedTest(private val useDelegation: Boolean) {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>(StandardTestDispatcher())

    @Test
    fun testOneMinFraction() {
        var top by mutableStateOf(-1000)
        var called = 0
        var isVisible = false
        val callback = { visible: Boolean ->
            called++
            isVisible = visible
        }
        rule.setContent {
            Column {
                Column {
                    Box(
                        Modifier.offset(y = top.dp)
                            .onVisibilityChangedTestImpl(
                                minFractionVisible = 1.0f,
                                callback = callback,
                            )
                            .size(100.dp)
                    )
                }
            }
        }
        rule.runOnIdle {
            // initially it is off-screen and not visible
            assertEquals(0, called)
            top = -20
        }
        rule.runOnIdle {
            // it still isn't completely in the viewport, so still shouldn't be called
            assertEquals(0, called)
            top = 0
        }
        rule.runOnIdle {
            // completely in the viewport now, so got called with true
            assertEquals(1, called)
            assertEquals(true, isVisible)
            top = -20
        }
        rule.runOnIdle {
            // partially in the viewport, but minFraction = 1 means the whole thing needs to be.
            // so now called with false
            assertEquals(2, called)
            assertEquals(false, isVisible)
            top = -1000
        }
        rule.runOnIdle {
            // outside the viewport completely now, but not called any additional amount since it
            // was already marked as out
            assertEquals(2, called)
        }
    }

    @Test
    fun testZeroMinFraction() {
        var top by mutableStateOf(-1000)
        var called = 0
        var isVisible = false
        val callback = { visible: Boolean ->
            called++
            isVisible = visible
        }
        rule.setContent {
            Column {
                Column {
                    Box(
                        Modifier.offset(y = top.dp)
                            .onVisibilityChangedTestImpl(
                                minFractionVisible = 0f,
                                callback = callback,
                            )
                            .size(100.dp)
                    )
                }
            }
        }
        rule.runOnIdle {
            // initially outside the viewport
            assertEquals(0, called)
            top = -101
        }
        rule.runOnIdle {
            // it should be just bordering the viewport, but still 100% outside of it, so still no
            // call
            assertEquals(0, called)
            top = -99
        }
        rule.runOnIdle {
            // should be 1dp inside the viewport, so callback should trigger
            assertEquals(1, called)
            assertEquals(true, isVisible)
            top = 10
        }
        rule.runOnIdle {
            // completely inside the viewport, but no extra calls made
            assertEquals(1, called)
            top = -99
        }
        rule.runOnIdle {
            // still 1dp inside the viewport
            assertEquals(1, called)
            assertEquals(true, isVisible)
            top = -101
        }
        rule.runOnIdle {
            // completely outside the viewport
            assertEquals(2, called)
            assertEquals(false, isVisible)
        }
    }

    @Test
    fun testHalfMinFraction() {
        var top by mutableStateOf(-1000)
        var called = 0
        var isVisible = false
        val callback = { visible: Boolean ->
            called++
            isVisible = visible
        }
        rule.setContent {
            Column {
                Column {
                    Box(
                        Modifier.offset(y = top.dp)
                            .onVisibilityChangedTestImpl(
                                minFractionVisible = 0.5f,
                                callback = callback,
                            )
                            .size(100.dp)
                    )
                }
            }
        }
        rule.runOnIdle {
            // initially outside the viewport
            assertEquals(0, called)
            top = -99
        }
        rule.runOnIdle {
            // it should be just barely inside the viewport, but since minFraction is 0.5, it still
            // should not have triggered
            assertEquals(0, called)
            top = -40
        }
        rule.runOnIdle {
            // should be inside inside the viewport, so callback should trigger
            assertEquals(1, called)
            assertEquals(true, isVisible)
            top = 10
        }
        rule.runOnIdle {
            // completely inside the viewport, but no extra calls made
            assertEquals(1, called)
            top = -40
        }
        rule.runOnIdle {
            // still 1dp inside the viewport
            assertEquals(1, called)
            assertEquals(true, isVisible)
            top = -60
        }
        rule.runOnIdle {
            // more than half is outside the viewport, callback should be triggered
            assertEquals(2, called)
            assertEquals(false, isVisible)
            top = -1000
        }
        rule.runOnIdle {
            // completely outside the viewport, no additional calls
            assertEquals(2, called)
        }
    }

    @Test
    fun testVisibleCalledOnFirstLoadZeroFraction() {
        var called = 0
        var isVisible = false
        rule.setContent {
            Column {
                Column {
                    Box(
                        Modifier.onVisibilityChangedTestImpl(minFractionVisible = 0f) { visible ->
                                called++
                                isVisible = visible
                            }
                            .size(100.dp)
                    )
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, called)
            assertEquals(true, isVisible)
        }
    }

    @Test
    fun testVisibleCalledOnFirstLoadOneFraction() {
        var called = 0
        var isVisible = false
        rule.setContent {
            Column {
                Column {
                    Box(
                        Modifier.onVisibilityChangedTestImpl(minFractionVisible = 1f) { visible ->
                                called++
                                isVisible = visible
                            }
                            .size(100.dp)
                    )
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, called)
            assertEquals(true, isVisible)
        }
    }

    @Test
    fun testVisibleNotCalledWhenNotVisibleInitially() {
        var called = 0
        var isVisible = false
        var parentSize by mutableStateOf(50.dp)
        val callback = { visible: Boolean ->
            called++
            isVisible = visible
        }
        rule.setContent {
            Column {
                Column(Modifier.size(parentSize)) {
                    Box(
                        Modifier.requiredSize(100.dp)
                            .onVisibilityChangedTestImpl(
                                minFractionVisible = 1f,
                                callback = callback,
                            )
                    )
                }
            }
        }
        rule.runOnIdle {
            assertEquals(0, called)
            parentSize = 100.dp
        }
        rule.runOnIdle {
            assertEquals(1, called)
            assertEquals(true, isVisible)
        }
    }

    @Test
    fun testOneFractionWithNodeMuchLargerThanViewport() {
        var called = 0
        var isVisible = false
        rule.setContent {
            Column {
                Column {
                    Box(
                        Modifier.onVisibilityChangedTestImpl(minFractionVisible = 1f) { visible ->
                                called++
                                isVisible = visible
                            }
                            .size(5000.dp)
                    )
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, called)
            assertEquals(true, isVisible)
        }
    }

    @Test
    fun testVisibleCalledWhenRemovedFromComposition() {
        var called = 0
        val shouldCompose = mutableStateOf(true)
        var isVisible = false
        rule.setContent {
            Column {
                Column {
                    if (shouldCompose.value) {
                        Box(
                            Modifier.onVisibilityChangedTestImpl(minFractionVisible = 1f) { visible
                                    ->
                                    called++
                                    isVisible = visible
                                }
                                .size(100.dp)
                        )
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, called)
            assertEquals(true, isVisible)

            shouldCompose.value = false
        }
        rule.runOnIdle {
            assertEquals(2, called)
            assertEquals(false, isVisible)
        }
    }

    @Test
    fun testVisibleCalledWhenRemovedFromModifierChain() {
        var called = 0
        val shouldCompose = mutableStateOf(true)
        var isVisible = false
        rule.setContent {
            val modifier =
                if (shouldCompose.value)
                    Modifier.onVisibilityChangedTestImpl(minFractionVisible = 1f) { visible ->
                        called++
                        isVisible = visible
                    }
                else Modifier
            Column { Column { Box(modifier.size(100.dp)) } }
        }
        rule.runOnIdle {
            assertEquals(1, called)
            assertEquals(true, isVisible)

            shouldCompose.value = false
        }
        rule.runOnIdle {
            assertEquals(2, called)
            assertEquals(false, isVisible)
        }
    }

    @Test
    fun testPassingParentsBounds_parentAndChildMovesTogether_noExtraCallbacks() {
        val parentBounds = LayoutBoundsHolder()
        val calls = mutableListOf<Boolean>()
        val callback: (Boolean) -> Unit = { visible -> calls.add(visible) }
        var offset by mutableStateOf(0.dp)
        rule.setContent {
            Column(Modifier.size(200.dp).offset(offset)) {
                Column(Modifier.size(100.dp).layoutBounds(parentBounds)) {
                    Box(
                        Modifier.size(100.dp)
                            .onVisibilityChanged(viewportBounds = parentBounds, callback = callback)
                    )
                }
            }
        }
        rule.runOnIdle {
            assertThat(calls).isEqualTo(listOf(true))
            offset = 1.dp
        }
        rule.runOnIdle {
            // as parent and child moves together there are no changes in visibility expected
            assertThat(calls).isEqualTo(listOf(true))
        }
    }

    @Test
    fun testVisibleCalledWhenAddedToModifierChain() {
        var called = 0
        val shouldCompose = mutableStateOf(false)
        var isVisible = false
        rule.setContent {
            val modifier =
                if (shouldCompose.value)
                    Modifier.onVisibilityChangedTestImpl(minFractionVisible = 1f) { visible ->
                        called++
                        isVisible = visible
                    }
                else Modifier
            Column { Column { Box(modifier.size(100.dp)) } }
        }
        rule.runOnIdle {
            assertEquals(0, called)
            assertEquals(false, isVisible)

            shouldCompose.value = true
        }
        rule.runOnIdle {
            assertEquals(1, called)
            assertEquals(true, isVisible)
        }
    }

    @Test
    fun testVisibleCalledWhenUnplacedAndThenPlacedAgain() {
        var called = 0
        val shouldPlace = mutableStateOf(true)
        var isVisible = false
        rule.setContent {
            Layout(
                content = {
                    Box(
                        Modifier.onVisibilityChangedTestImpl(minFractionVisible = 1f) { visible ->
                                called++
                                isVisible = visible
                            }
                            .size(100.dp)
                    )
                }
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) {
                    if (shouldPlace.value) {
                        placeable.place(0, 0)
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, called)
            assertEquals(true, isVisible)

            shouldPlace.value = false
        }
        rule.runOnIdle {
            assertEquals(2, called)
            assertEquals(false, isVisible)
            shouldPlace.value = true
            shouldPlace.value = true
        }
        rule.runOnIdle {
            assertEquals(3, called)
            assertEquals(true, isVisible)
        }
    }

    @Test
    fun testNotVisibleCalledWhenDisposed_nonZeroMinDurationMs() {
        val calls = mutableListOf<Boolean>()
        var shouldCompose by mutableStateOf(true)
        rule.setContent {
            Box {
                if (shouldCompose) {
                    Box(
                        Modifier.onVisibilityChangedTestImpl(minDurationMs = 500) { visible ->
                                calls.add(visible)
                            }
                            .size(100.dp)
                    )
                }
            }
        }
        rule.waitUntil(1000) { !calls.isEmpty() }
        rule.runOnIdle {
            assertThat(calls).isEqualTo(listOf(true))
            shouldCompose = false
        }
        rule.runOnIdle { assertThat(calls).isEqualTo(listOf(true, false)) }
    }

    @Test
    fun testNoCallbacksCalledWhenDisposedBeforeMinDurationMsPassed() {
        val calls = mutableListOf<Boolean>()
        var shouldCompose by mutableStateOf(true)
        rule.setContent {
            Box {
                if (shouldCompose) {
                    Box(
                        Modifier.onVisibilityChangedTestImpl(minDurationMs = 1_000_000) { visible ->
                                calls.add(visible)
                            }
                            .size(100.dp)
                    )
                }
            }
        }
        rule.runOnIdle {
            assertThat(calls).isEmpty()
            shouldCompose = false
        }
        rule.runOnIdle { assertThat(calls).isEmpty() }
    }

    @Test
    fun testNotVisibleNotCalledWhenWasVisibleForLessThanMinDuration() {
        var called = 0
        var parentSize by mutableStateOf(100.dp)
        val callback: (Boolean) -> Unit = { visible: Boolean -> called++ }
        rule.setContent {
            Column {
                Column(Modifier.size(parentSize)) {
                    Box(
                        Modifier.requiredSize(100.dp)
                            .onVisibilityChangedTestImpl(
                                minDurationMs = 1_000_000,
                                callback = callback,
                            )
                    )
                }
            }
        }
        rule.runOnIdle {
            assertEquals(0, called)
            parentSize = 50.dp
        }
        rule.runOnIdle { assertEquals(0, called) }
    }

    @Test
    fun visibilityTriggeredOnViewDetach() {
        var called = 0
        var present by mutableStateOf(true)
        val callback: (Boolean) -> Unit = { visible: Boolean -> called++ }
        val view =
            ComposeView(rule.activity).apply {
                setContent {
                    Box(
                        Modifier.requiredSize(100.dp)
                            .onVisibilityChangedTestImpl(callback = callback)
                    )
                }
            }
        rule.setContent {
            Column {
                if (present) {
                    AndroidView(factory = { view })
                }
            }
        }
        assertEquals(1, called)

        present = false
        rule.runOnIdle { assertEquals(2, called) }

        present = true
        rule.runOnIdle { assertEquals(3, called) }
    }

    fun Modifier.onVisibilityChangedTestImpl(
        @IntRange(from = 0) minDurationMs: Long = 0,
        @FloatRange(from = 0.0, to = 1.0) minFractionVisible: Float = 1f,
        viewportBounds: LayoutBoundsHolder? = null,
        callback: (Boolean) -> Unit,
    ): Modifier =
        if (useDelegation) {
            then(DelegatingImplElement(minDurationMs, minFractionVisible, viewportBounds, callback))
        } else {
            onVisibilityChanged(minDurationMs, minFractionVisible, viewportBounds, callback)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "useDelegation={0}")
        fun params() = arrayOf(false, true)
    }
}

private data class DelegatingImplElement(
    val minDurationMs: Long,
    val minFractionVisible: Float,
    val viewportBounds: LayoutBoundsHolder?,
    val callback: (Boolean) -> Unit,
) : ModifierNodeElement<DelegatingImplNode>() {
    override fun create() =
        DelegatingImplNode(minDurationMs, minFractionVisible, viewportBounds, callback)

    override fun update(node: DelegatingImplNode) {
        throw IllegalStateException("this delegating impl doesn't support updating params")
    }
}

private class DelegatingImplNode(
    minDurationMs: Long,
    minFractionVisible: Float,
    viewportBounds: LayoutBoundsHolder?,
    callback: (Boolean) -> Unit,
) : DelegatingNode() {
    private val onVisibilityNode =
        delegate(
            onVisibilityChangedNode(minDurationMs, minFractionVisible, viewportBounds, callback)
        )
}
