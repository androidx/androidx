/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material.swipeable

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.SwipeableV2State.AnchorChangedCallback
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableV2Defaults.ReconcileAnimationOnAnchorChangedCallback
import androidx.compose.material.SwipeableV2State
import androidx.compose.material.swipeable.SwipeableTestValue.A
import androidx.compose.material.swipeable.SwipeableTestValue.B
import androidx.compose.material.swipeable.SwipeableTestValue.C
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalMaterialApi::class)
class SwipeableV2AnchorTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun swipeable_reconcileAnchorChangeHandler_retargetsAnimationWhenOffsetChanged() {
        val animationDurationMillis = 2000
        lateinit var state: SwipeableV2State<SwipeableTestValue>
        lateinit var scope: CoroutineScope

        val firstAnchors = mapOf(A to 0f, B to 100f, C to 200f)
        val secondAnchors = mapOf(A to 300f, B to 400f, C to 600f)
        var anchors = firstAnchors
        var size by mutableStateOf(100)

        val animationTarget = B

        rule.setContent {
            state = remember {
                SwipeableV2State(
                    initialValue = A,
                    animationSpec = tween(animationDurationMillis, easing = LinearEasing),
                    positionalThreshold = defaultPositionalThreshold(),
                    velocityThreshold = defaultVelocityThreshold()
                )
            }
            scope = rememberCoroutineScope()
            val anchorChangeHandler = remember(state, scope) {
                ReconcileAnimationOnAnchorChangedCallback(
                    state,
                    scope
                )
            }
            Box(
                Modifier
                    .size(size.dp) // Trigger anchor recalculation when size changes
                    .onSizeChanged { state.updateAnchors(anchors, anchorChangeHandler) }
            )
        }

        assertThat(state.currentValue == A)
        rule.mainClock.autoAdvance = false

        scope.launch { state.animateTo(animationTarget) }

        rule.mainClock.advanceTimeByFrame()
        anchors = secondAnchors
        size = 200
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertThat(state.offset).isEqualTo(secondAnchors.getValue(animationTarget))
    }

    @Test
    fun swipeable_reconcileAnchorChangeHandler_snapsWhenPreviousAnchorRemoved() {
        val state = SwipeableV2State(
            initialValue = A,
            positionalThreshold = defaultPositionalThreshold(),
            velocityThreshold = defaultVelocityThreshold()
        )

        val firstAnchors = mapOf(A to 0f, B to 100f, C to 200f)
        val secondAnchors = mapOf(B to 400f, C to 600f)
        var anchors = firstAnchors
        var size by mutableStateOf(100)

        rule.setContent {
            val scope = rememberCoroutineScope()
            val anchorChangeHandler = remember(state, scope) {
                ReconcileAnimationOnAnchorChangedCallback(state, scope)
            }
            Box(
                Modifier
                    .size(size.dp) // Trigger anchor recalculation when size changes
                    .onSizeChanged { state.updateAnchors(anchors, anchorChangeHandler) }
            )
        }

        assertThat(state.currentValue == A)

        anchors = secondAnchors
        size = 200
        rule.waitForIdle()

        assertThat(state.currentValue).isEqualTo(B)
    }

    @Test
    fun swipeable_anchorChangeHandler_calledWithUpdatedAnchorsWhenChanged() {
        var anchorChangeHandlerInvocationCount = 0
        var actualPreviousAnchors: Map<SwipeableTestValue, Float>? = null
        var actualNewAnchors: Map<SwipeableTestValue, Float>? = null
        val testChangeHandler = AnchorChangedCallback { _, previousAnchors, newAnchors ->
            anchorChangeHandlerInvocationCount++
            actualPreviousAnchors = previousAnchors
            actualNewAnchors = newAnchors
        }
        val state = SwipeableV2State(
            initialValue = A,
            positionalThreshold = defaultPositionalThreshold(),
            velocityThreshold = defaultVelocityThreshold()
        )
        val initialSize = 100.dp
        var size: Dp by mutableStateOf(initialSize)
        rule.setContent {
            Box(
                Modifier
                    .requiredSize(size) // Trigger anchor recalculation when size changes
                    .onSizeChanged { layoutSize ->
                        state.updateAnchors(
                            mapOf(
                                A to 0f,
                                B to layoutSize.height / 2f,
                                C to layoutSize.height.toFloat()
                            ),
                            testChangeHandler
                        )
                    }
            )
        }

        // The change handler should not get invoked when the anchors are first set
        assertThat(anchorChangeHandlerInvocationCount).isEqualTo(0)

        val expectedPreviousAnchors = state.anchors
        size = 200.dp // Recompose with new size so anchors change
        val sizePx = with(rule.density) { size.roundToPx() }
        val layoutSize = IntSize(sizePx, sizePx)
        val expectedNewAnchors = mapOf(
            A to 0f,
            B to layoutSize.height / 2f,
            C to layoutSize.height.toFloat()
        )
        rule.waitForIdle()

        assertThat(anchorChangeHandlerInvocationCount).isEqualTo(1)
        assertThat(actualPreviousAnchors).isEqualTo(expectedPreviousAnchors)
        assertThat(actualNewAnchors).isEqualTo(expectedNewAnchors)
    }

    @Test
    fun swipeable_anchorChangeHandler_invokedWithPreviousTarget() {
        var recordedPreviousTargetValue: SwipeableTestValue? = null
        val testChangeHandler =
            AnchorChangedCallback<SwipeableTestValue> { previousTarget, _, _ ->
                recordedPreviousTargetValue = previousTarget
            }
        val state = SwipeableV2State(
            initialValue = A,
            positionalThreshold = { totalDistance -> totalDistance * 0.5f },
            velocityThreshold = defaultVelocityThreshold()
        )
        var anchors = mapOf(
            A to 0f,
            B to 100f,
            C to 200f
        )
        state.updateAnchors(anchors, testChangeHandler)

        assertThat(state.targetValue).isEqualTo(A)
        anchors = mapOf(B to 500f)
        state.updateAnchors(anchors, testChangeHandler)
        assertThat(recordedPreviousTargetValue).isEqualTo(A) // A is not in the anchors anymore, so
        // we can be sure that is not the targetValue calculated from the new anchors
    }

    @Test
    fun swipeable_anchorChangeHandler_invokedIfInitialValueNotInInitialAnchors() {
        var anchorChangeHandlerInvocationCount = 0
        val testChangeHandler = AnchorChangedCallback<SwipeableTestValue> { _, _, _ ->
            anchorChangeHandlerInvocationCount++
        }
        val state = SwipeableV2State(
            initialValue = A,
            positionalThreshold = defaultPositionalThreshold(),
            velocityThreshold = defaultVelocityThreshold()
        )
        state.updateAnchors(mapOf(B to 100f, C to 200f), testChangeHandler)

        assertThat(anchorChangeHandlerInvocationCount).isEqualTo(1)
    }

    private fun defaultPositionalThreshold(): (totalDistance: Float) -> Float = with(rule.density) {
        { 56.dp.toPx() }
    }
    private fun defaultVelocityThreshold(): () -> Float = with(rule.density) {
        { 125.dp.toPx() }
    }
}