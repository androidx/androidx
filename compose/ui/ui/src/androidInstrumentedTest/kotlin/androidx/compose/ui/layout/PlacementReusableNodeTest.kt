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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.OnUnplacedModifierNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PlacementReusableNodeTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun onPlacedCalledOnReuseInsideLazyColumn() {
        lateinit var density: Density
        val items = 200
        val visibleItems = 2
        val itemSize = 50.dp
        val invocations = arrayOf(0, 0)

        // It's important to share lambda across all iterations
        val placedCallback0: (LayoutCoordinates) -> Unit = { invocations[0] = invocations[0] + 1 }
        val placedCallback1: (LayoutCoordinates) -> Unit = { invocations[1] = invocations[1] + 1 }
        val scrollState = LazyListState()
        rule.setContent {
            density = LocalDensity.current
            LazyColumn(Modifier.size(itemSize, itemSize * visibleItems), scrollState) {
                items(items) {
                    Box(Modifier.size(itemSize).onPlaced(placedCallback0)) {
                        Box(Modifier.size(itemSize).onPlaced(placedCallback1))
                    }
                }
            }
        }

        var expectedInvocations = visibleItems
        val delta = with(density) { (itemSize * visibleItems).toPx() }
        repeat(items / visibleItems) {
            rule.runOnIdle {
                assertThat(invocations[0]).isAtLeast(expectedInvocations)
                assertThat(invocations[1]).isAtLeast(expectedInvocations)

                scrollState.dispatchRawDelta(delta)
                expectedInvocations += visibleItems
            }
        }
    }

    @Test
    fun placeMultiplatformInteropView() {
        val showPlatformInterop = mutableStateOf(true)
        var currentlyVisible = false
        rule.setContent {
            if (showPlatformInterop.value) {
                TestMultiplatformInteropView(
                    onAddedToPlatformHierarchy = { currentlyVisible = true },
                    onRemovedFromPlatformHierarchy = { currentlyVisible = false },
                    modifier = Modifier.size(100.dp),
                )
            }
        }

        rule.runOnIdle { assertThat(currentlyVisible).isTrue() }

        showPlatformInterop.value = false
        rule.runOnIdle { assertThat(currentlyVisible).isFalse() }
    }

    @Test
    fun placeMultiplatformInteropViewInsideLazyColumn() {
        lateinit var density: Density

        val currentlyVisible = mutableSetOf<Int>()
        val scrollState = LazyListState()
        rule.setContent {
            density = LocalDensity.current
            LazyColumn(Modifier.size(100.dp, 250.dp), scrollState) {
                items(100) { index ->
                    TestMultiplatformInteropView(
                        onAddedToPlatformHierarchy = { currentlyVisible += index },
                        onRemovedFromPlatformHierarchy = { currentlyVisible -= index },
                        modifier = Modifier.size(100.dp),
                    )
                }
            }
        }

        rule.runOnIdle { assertThat(currentlyVisible).containsExactly(0, 1, 2) }

        rule.runOnIdle { scrollState.dispatchRawDelta(with(density) { 1010.dp.toPx() }) }
        rule.runOnIdle { assertThat(currentlyVisible).containsExactly(10, 11, 12) }

        rule.runOnIdle { scrollState.dispatchRawDelta(with(density) { -1010.dp.toPx() }) }
        rule.runOnIdle { assertThat(currentlyVisible).containsExactly(0, 1, 2) }
    }
}

/**
 * This emulates multiplatform interop element that placed and drawn outside of Compose.
 * onPlaced/onUnplaced callbacks are used to control their lifecycle in platform's views hierarchy.
 */
@Composable
private fun TestMultiplatformInteropView(
    onAddedToPlatformHierarchy: () -> Unit,
    onRemovedFromPlatformHierarchy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier then
            TrackInteropPlacementModifierElement(
                onAddedToPlatformHierarchy = onAddedToPlatformHierarchy,
                onRemovedFromPlatformHierarchy = onRemovedFromPlatformHierarchy,
            )
    )
}

private data class TrackInteropPlacementModifierElement(
    var onAddedToPlatformHierarchy: () -> Unit,
    var onRemovedFromPlatformHierarchy: () -> Unit,
) : ModifierNodeElement<TrackInteropPlacementModifierNode>() {
    override fun create() =
        TrackInteropPlacementModifierNode(
            onAddedToPlatformHierarchy = onAddedToPlatformHierarchy,
            onRemovedFromPlatformHierarchy = onRemovedFromPlatformHierarchy,
        )

    override fun update(node: TrackInteropPlacementModifierNode) {
        node.onAddedToPlatformHierarchy = onAddedToPlatformHierarchy
        node.onRemovedFromPlatformHierarchy = onRemovedFromPlatformHierarchy
    }
}

private class TrackInteropPlacementModifierNode(
    var onAddedToPlatformHierarchy: () -> Unit,
    var onRemovedFromPlatformHierarchy: () -> Unit,
) : Modifier.Node(), LayoutAwareModifierNode, OnUnplacedModifierNode {
    private var isPlaced = false

    override fun onPlaced(coordinates: LayoutCoordinates) {
        onAddedToPlatformHierarchy()
        isPlaced = true
    }

    override fun onUnplaced() {
        onRemovedFromPlatformHierarchy()
        isPlaced = false
    }

    override fun onDetach() {
        // TODO(b/309776096): Remove workaround for missing [onUnplaced]
        //  once it will be reliable implemented
        if (isPlaced) {
            onUnplaced()
        }
        super.onDetach()
    }
}
