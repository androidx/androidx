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

package androidx.compose.material3

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class SheetStateTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>(StandardTestDispatcher())

    private fun createSheetState(
        skipPartiallyExpanded: Boolean,
        skipHiddenState: Boolean,
        initialValue: SheetValue,
    ): SheetState {
        return SheetState(
            skipPartiallyExpanded = skipPartiallyExpanded,
            positionalThreshold = { 56f },
            velocityThreshold = { 125f },
            initialValue = initialValue,
            skipHiddenState = skipHiddenState,
        )
    }

    private class PseudoFrameClock : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            // We use the system time, but in a real test environment with StandardTestDispatcher,
            // this simply allows the coroutine to proceed.
            return onFrame(System.nanoTime())
        }
    }

    @Test
    fun state_constructor_initialValueContracts() {
        // Conflicting anchor flags and initial values throw IllegalArgumentException
        assertThrows(IllegalArgumentException::class.java) {
            createSheetState(
                skipPartiallyExpanded = true,
                skipHiddenState = false,
                initialValue = SheetValue.PartiallyExpanded,
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            createSheetState(
                skipPartiallyExpanded = false,
                skipHiddenState = true,
                initialValue = SheetValue.Hidden,
            )
        }
    }

    @Test
    fun state_currentValue_mapsToSettledValue() = runTest {
        val state =
            createSheetState(
                skipPartiallyExpanded = false,
                skipHiddenState = false,
                initialValue = SheetValue.Hidden,
            )

        val newAnchors =
            DraggableAnchors<SheetValue> {
                SheetValue.Hidden at 1000f
                SheetValue.Expanded at 0f
            }
        state.anchoredDraggableState.updateAnchors(newAnchors, SheetValue.Hidden)

        assertThat(state.currentValue).isEqualTo(SheetValue.Hidden)

        state.snapTo(SheetValue.Expanded)
        assertThat(state.currentValue).isEqualTo(SheetValue.Expanded)
        assertThat(state.requireOffset()).isEqualTo(0f)
    }

    @Test
    fun state_targetValue_mapsToCurrentValue_whenSettled() = runTest {
        val state =
            createSheetState(
                skipPartiallyExpanded = false,
                skipHiddenState = false,
                initialValue = SheetValue.Expanded,
            )
        val newAnchors =
            DraggableAnchors<SheetValue> {
                SheetValue.Hidden at 1000f
                SheetValue.Expanded at 0f
            }

        state.anchoredDraggableState.updateAnchors(newAnchors, SheetValue.Expanded)
        assertThat(state.currentValue).isEqualTo(SheetValue.Expanded)
        assertThat(state.targetValue).isEqualTo(SheetValue.Expanded)
    }

    @Test
    fun state_targetValue_fixLogic_handlesNonExistentAnchor() = runTest {
        val state =
            createSheetState(
                skipPartiallyExpanded = false,
                skipHiddenState = false,
                initialValue = SheetValue.PartiallyExpanded,
            )

        // Setup initial anchors that include PartiallyExpanded
        val initialAnchors =
            DraggableAnchors<SheetValue> {
                SheetValue.PartiallyExpanded at 500f
                SheetValue.Expanded at 0f
            }
        state.anchoredDraggableState.updateAnchors(initialAnchors, SheetValue.PartiallyExpanded)
        assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        assertThat(state.targetValue).isEqualTo(SheetValue.PartiallyExpanded)

        // Now, update anchors to REMOVE PartiallyExpanded (e.g. layout change)
        val newAnchors = DraggableAnchors<SheetValue> { SheetValue.Expanded at 0f }
        // Update the state's anchors without changing the offset/value immediately
        state.anchoredDraggableState.updateAnchors(
            newAnchors,
            state.anchoredDraggableState.targetValue,
        )

        // The custom logic in targetValue should see that currentValue (PartiallyExpanded)
        // is no longer in the anchor set (offset is NaN).
        // It should return currentValue (PartiallyExpanded) rather than jumping to Expanded.
        assertThat(state.targetValue).isEqualTo(SheetValue.PartiallyExpanded)
        assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
    }

    @Test
    fun state_targetValue_fixLogic_handlesExactOffsetMatch() = runTest {
        val state =
            createSheetState(
                skipPartiallyExpanded = false,
                skipHiddenState = false,
                initialValue = SheetValue.PartiallyExpanded,
            )
        val anchors =
            DraggableAnchors<SheetValue> {
                SheetValue.PartiallyExpanded at 500f
                SheetValue.Expanded at 0f
            }
        state.anchoredDraggableState.updateAnchors(anchors, SheetValue.PartiallyExpanded)

        // Verify state
        assertThat(state.requireOffset()).isEqualTo(500f)
        assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

        // targetValue should be stable at PartiallyExpanded
        assertThat(state.targetValue).isEqualTo(SheetValue.PartiallyExpanded)
    }

    @Test
    fun state_zeroPeekHeight_partiallyExpandedMapsToHiddenOffset() =
        runTest(PseudoFrameClock()) {
            val state =
                createSheetState(
                    skipPartiallyExpanded = false,
                    skipHiddenState = false,
                    initialValue = SheetValue.PartiallyExpanded,
                )
            val screenHeight = 1000f

            val anchors =
                DraggableAnchors<SheetValue> {
                    SheetValue.Hidden at screenHeight
                    SheetValue.PartiallyExpanded at screenHeight // Same offset as Hidden
                    SheetValue.Expanded at 0f
                }
            state.anchoredDraggableState.updateAnchors(anchors, SheetValue.PartiallyExpanded)
            assertThat(state.requireOffset()).isEqualTo(screenHeight)
            assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

            // Verify isVisible is true (because state != Hidden), even though it looks hidden.
            assertThat(state.isVisible).isTrue()

            // Even though Hidden and PartiallyExpanded share the same offset (1000f),
            // targetValue should prefer the currentValue if we are exactly at that offset.
            assertThat(state.targetValue).isEqualTo(SheetValue.PartiallyExpanded)

            // Component can still be hidden
            state.hide()
            assertThat(state.isVisible).isFalse()
            assertThat(state.targetValue).isEqualTo(SheetValue.Hidden)
            assertThat(state.currentValue).isEqualTo(SheetValue.Hidden)
        }

    @Test
    fun state_zeroPeekHeight_partialExpandMethod() =
        runTest(PseudoFrameClock()) {
            val state =
                createSheetState(
                    skipPartiallyExpanded = false,
                    skipHiddenState = false,
                    initialValue = SheetValue.Expanded,
                )
            val screenHeight = 1000f

            val anchors =
                DraggableAnchors<SheetValue> {
                    SheetValue.Hidden at screenHeight
                    SheetValue.PartiallyExpanded at screenHeight // Same offset as Hidden
                    SheetValue.Expanded at 0f
                }
            state.anchoredDraggableState.updateAnchors(anchors, SheetValue.Expanded)
            assertThat(state.currentValue).isEqualTo(SheetValue.Expanded)
            state.partialExpand()

            // Should settle at PartiallyExpanded
            assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
            assertThat(state.requireOffset()).isEqualTo(screenHeight)

            // And targetValue should be correct
            assertThat(state.targetValue).isEqualTo(SheetValue.PartiallyExpanded)
        }

    @Test
    fun state_expand_hide_show_api() =
        runTest(PseudoFrameClock()) {
            val state =
                createSheetState(
                    skipPartiallyExpanded = false,
                    skipHiddenState = false,
                    initialValue = SheetValue.Hidden,
                )
            val anchors =
                DraggableAnchors<SheetValue> {
                    SheetValue.Hidden at 1000f
                    SheetValue.PartiallyExpanded at 500f
                    SheetValue.Expanded at 0f
                }
            state.anchoredDraggableState.updateAnchors(anchors, SheetValue.Hidden)

            // Test Show (defaults to PartiallyExpanded if available)
            state.show()
            assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

            // Test Expand
            state.expand()
            assertThat(state.currentValue).isEqualTo(SheetValue.Expanded)

            // Test Hide
            state.hide()
            assertThat(state.currentValue).isEqualTo(SheetValue.Hidden)
            assertThat(state.isVisible).isFalse()
        }

    @Test
    fun state_respectsConfirmValueChange() {
        lateinit var state: SheetState
        lateinit var scope: CoroutineScope
        val dragHandleTag = "dragHandleTag"
        val sheetTag = "sheetTag"

        rule.setContent {
            scope = rememberCoroutineScope()
            state =
                rememberModalBottomSheetState(
                    confirmValueChange = { newState -> newState != SheetValue.Hidden }
                )

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = state,
                dragHandle = { Box(Modifier.testTag(dragHandleTag).size(44.dp)) },
            ) {
                Box(Modifier.fillMaxSize().testTag(sheetTag))
            }
        }

        val currentOffset = state.requireOffset()
        // Respects on swipe gesture
        rule.runOnIdle { assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded) }
        rule.onNodeWithTag(sheetTag).performTouchInput { swipeDown() }
        rule.runOnIdle { assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded) }
        assertThat(state.requireOffset()).isEqualTo(currentOffset)

        // Respects on animation call
        rule.runOnIdle { scope.launch { state.hide() } }
        rule.runOnIdle { assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded) }
        assertThat(state.requireOffset()).isEqualTo(currentOffset)

        // Respects on semantic action
        rule
            .onNodeWithTag(dragHandleTag, useUnmergedTree = true)
            .onParent()
            .performSemanticsAction(SemanticsActions.Dismiss)

        rule.runOnIdle { assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded) }
        assertThat(state.requireOffset()).isEqualTo(currentOffset)
    }

    @Test
    fun state_anchorsChange_retainsCurrentValue() {
        lateinit var state: SheetState
        var amountOfItems by mutableStateOf(0)
        lateinit var scope: CoroutineScope
        val sheetTag = "sheetTag"
        rule.setContent {
            state = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = state,
                dragHandle = null,
                contentWindowInsets = { WindowInsets(0) },
            ) {
                scope = rememberCoroutineScope()
                LazyColumn(Modifier.testTag(sheetTag)) {
                    items(amountOfItems) { ListItem(headlineContent = { Text("$it") }) }
                }
            }
        }

        assertThat(state.currentValue).isEqualTo(SheetValue.Hidden)

        amountOfItems = 50
        rule.waitForIdle()
        scope.launch { state.show() }
        // The anchors should now be {Hidden, PartiallyExpanded, Expanded}

        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

        amountOfItems = 100 // The anchors should now be {Hidden, PartiallyExpanded, Expanded}

        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded) // We should
        // retain the current value if possible
        assertTrue(state.anchoredDraggableState.anchors.hasPositionFor(SheetValue.Hidden))
        assertTrue(
            state.anchoredDraggableState.anchors.hasPositionFor(SheetValue.PartiallyExpanded)
        )
        assertTrue(state.anchoredDraggableState.anchors.hasPositionFor(SheetValue.Expanded))

        scope.launch { state.expand() }
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(SheetValue.Expanded)

        amountOfItems = 50
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(SheetValue.Expanded)
        // We should retain the current value if possible
        assertTrue(state.anchoredDraggableState.anchors.hasPositionFor(SheetValue.Hidden))
        assertTrue(
            state.anchoredDraggableState.anchors.hasPositionFor(SheetValue.PartiallyExpanded)
        )
        assertTrue(state.anchoredDraggableState.anchors.hasPositionFor(SheetValue.Expanded))

        amountOfItems = 0 // When the sheet height is 0, we should only have a hidden anchor
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(SheetValue.Hidden)
        assertTrue(state.anchoredDraggableState.anchors.hasPositionFor(SheetValue.Hidden))
        assertFalse(
            state.anchoredDraggableState.anchors.hasPositionFor(SheetValue.PartiallyExpanded)
        )
        assertFalse(state.anchoredDraggableState.anchors.hasPositionFor(SheetValue.Expanded))
    }

    @Test
    fun state_missingAnchors_findsClosest() {
        val topTag = "BottomSheetLayout"
        var showShortContent by mutableStateOf(false)
        lateinit var state: SheetState
        lateinit var scope: CoroutineScope
        val sheetTag = "sheetTag"

        rule.setContent {
            val density = LocalDensity.current
            state =
                SheetState(
                    skipPartiallyExpanded = false,
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            scope = rememberCoroutineScope()
            ModalBottomSheet(
                onDismissRequest = {},
                modifier = Modifier.testTag(topTag),
                dragHandle = null,
                sheetState = state,
            ) {
                if (showShortContent) {
                    Box(Modifier.fillMaxWidth().height(1.dp))
                } else {
                    Box(Modifier.fillMaxSize().testTag(sheetTag))
                }
            }
        }

        scope.launch { state.hide() }
        rule.runOnIdle { assertThat(state.currentValue).isEqualTo(SheetValue.Hidden) }

        showShortContent = true
        scope.launch { state.show() }
        rule.runOnIdle { assertThat(state.currentValue).isEqualTo(SheetValue.Expanded) }
    }

    @Test
    fun state_shortSheet_anchorChangeHandler_previousTargetNotInAnchors_reconciles() {
        var hasSheetContent by mutableStateOf(false) // Start out with empty sheet content
        lateinit var scope: CoroutineScope
        lateinit var state: SheetState
        val sheetTag = "sheetTag"

        rule.setContent {
            val density = LocalDensity.current
            state =
                SheetState(
                    skipPartiallyExpanded = false,
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            scope = rememberCoroutineScope()

            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = state,
                dragHandle = null,
                contentWindowInsets = { WindowInsets(0) },
            ) {
                if (hasSheetContent) {
                    Box(Modifier.fillMaxHeight(0.4f).testTag(sheetTag))
                } else {
                    Box(Modifier.size(0.dp).testTag(sheetTag))
                }
            }
        }

        assertThat(state.currentValue).isEqualTo(SheetValue.Hidden)
        assertFalse(
            state.anchoredDraggableState.anchors.hasPositionFor(SheetValue.PartiallyExpanded)
        )
        assertFalse(state.anchoredDraggableState.anchors.hasPositionFor(SheetValue.Expanded))

        scope.launch { state.show() }
        rule.waitForIdle()

        assertThat(state.isVisible).isTrue()
        // animateTo with a non-existent target will force currentValue to the targetValue
        // (Expanded). This allows moving the sheet to an anchor that we anticipate will be
        // available in the next composition/layout pass, meaning the sheet will
        // reconcile to that anchor when the sheet is re-laid out.
        assertThat(state.currentValue).isEqualTo(SheetValue.Expanded)
        assertThat(state.targetValue).isEqualTo(SheetValue.Expanded)
        assertThat(state.requireOffset()).isEqualTo(rule.rootHeightPx())

        hasSheetContent = true // Recompose with sheet content
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(SheetValue.Expanded)
        assertThat(state.targetValue).isEqualTo(SheetValue.Expanded)
        assertThat(state.requireOffset()).isWithin(1f).of(rule.rootHeightPx() * 0.6f)
    }

    @Test
    fun state_tallSheet_anchorChangeHandler_previousTargetNotInAnchors_reconciles() {
        var hasSheetContent by mutableStateOf(false) // Start out with empty sheet content
        lateinit var scope: CoroutineScope
        lateinit var state: SheetState
        val sheetTag = "sheetTag"

        rule.setContent {
            val density = LocalDensity.current
            state =
                SheetState(
                    skipPartiallyExpanded = false,
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )
            scope = rememberCoroutineScope()
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = state,
                dragHandle = null,
                contentWindowInsets = { WindowInsets(0) },
            ) {
                if (hasSheetContent) {
                    Box(Modifier.fillMaxHeight(0.6f).testTag(sheetTag))
                } else {
                    Box(Modifier.size(0.dp).testTag(sheetTag))
                }
            }
        }

        assertThat(state.currentValue).isEqualTo(SheetValue.Hidden)
        assertFalse(
            state.anchoredDraggableState.anchors.hasPositionFor(SheetValue.PartiallyExpanded)
        )
        assertFalse(state.anchoredDraggableState.anchors.hasPositionFor(SheetValue.Expanded))

        scope.launch { state.show() }
        rule.waitForIdle()

        assertThat(state.isVisible).isTrue()
        assertThat(state.currentValue).isEqualTo(SheetValue.Expanded)
        assertThat(state.requireOffset()).isEqualTo(rule.rootHeightPx())

        hasSheetContent = true // Recompose with sheet content
        rule.waitForIdle()
        assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded)
        assertThat(state.requireOffset()).isWithin(1f).of(rule.rootHeightPx() * 0.5f)
    }

    @Test
    fun state_nestedScroll_consumesWithinBounds_scrollsOutsideBounds() {
        lateinit var state: SheetState
        lateinit var scrollState: ScrollState
        val sheetTag = "sheetTag"
        rule.setContent {
            state = rememberModalBottomSheetState()
            ModalBottomSheet(onDismissRequest = {}, sheetState = state) {
                scrollState = rememberScrollState()
                Column(Modifier.verticalScroll(scrollState).testTag(sheetTag)) {
                    repeat(100) { Text(it.toString(), Modifier.requiredHeight(50.dp)) }
                }
            }
        }

        rule.waitForIdle()

        assertThat(scrollState.value).isEqualTo(0)
        assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

        rule.onNodeWithTag(sheetTag).performTouchInput {
            swipeUp(startY = bottom, endY = bottom / 2)
        }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(state.currentValue).isEqualTo(SheetValue.Expanded)

        rule.onNodeWithTag(sheetTag).performTouchInput { swipeUp(startY = bottom, endY = top) }
        rule.waitForIdle()
        assertThat(scrollState.value).isGreaterThan(0)
        assertThat(state.currentValue).isEqualTo(SheetValue.Expanded)

        rule.onNodeWithTag(sheetTag).performTouchInput { swipeDown(startY = top, endY = bottom) }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(state.currentValue).isEqualTo(SheetValue.Expanded)

        rule.onNodeWithTag(sheetTag).performTouchInput {
            swipeDown(startY = top, endY = bottom / 2)
        }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(state.currentValue).isEqualTo(SheetValue.PartiallyExpanded)

        rule.onNodeWithTag(sheetTag).performTouchInput {
            swipeDown(startY = bottom / 2, endY = bottom)
        }
        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(state.currentValue).isEqualTo(SheetValue.Hidden)
    }

    private fun ComposeTestRule.rootHeightPx(): Float {
        val node = onAllNodes(isDialog(), useUnmergedTree = true).onFirst()
        val bounds = node.getUnclippedBoundsInRoot()
        return with(density) { bounds.bottom.toPx() - bounds.top.toPx() }
    }
}
