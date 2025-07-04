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

package androidx.compose.animation.core

import androidx.collection.mutableLongListOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.VectorConverter
import androidx.compose.animation.animateColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class TransitionTest {
    private val rule = createComposeRule()

    // Detect leaks BEFORE and AFTER compose rule work
    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(DetectLeaksAfterTestSuccess()).around(rule)

    private enum class AnimStates {
        From,
        To,
    }

    @OptIn(InternalAnimationApi::class)
    @Test
    fun transitionTest() {
        val target = mutableStateOf(AnimStates.From)
        val floatAnim1 =
            TargetBasedAnimation(
                spring(dampingRatio = Spring.DampingRatioHighBouncy),
                Float.VectorConverter,
                0f,
                1f,
            )
        val floatAnim2 =
            TargetBasedAnimation(
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
                Float.VectorConverter,
                1f,
                0f,
            )

        val colorAnim1 =
            TargetBasedAnimation(
                tween(1000),
                Color.VectorConverter(Color.Red.colorSpace),
                Color.Red,
                Color.Green,
            )
        val colorAnim2 =
            TargetBasedAnimation(
                tween(1000),
                Color.VectorConverter(Color.Red.colorSpace),
                Color.Green,
                Color.Red,
            )

        // Animate from 0f to 0f for 1000ms
        val keyframes1 =
            keyframes<Float> {
                durationMillis = 1000
                0f at 0
                200f at 400
                1000f at 1000
            }

        val keyframes2 =
            keyframes<Float> {
                durationMillis = 800
                0f at 0
                -500f at 400
                -1000f at 800
            }

        val keyframesAnim1 = TargetBasedAnimation(keyframes1, Float.VectorConverter, 0f, 0f)
        val keyframesAnim2 = TargetBasedAnimation(keyframes2, Float.VectorConverter, 0f, 0f)
        val animFloat = mutableStateOf(-1f)
        val animColor = mutableStateOf(Color.Gray)
        val animFloatWithKeyframes = mutableStateOf(-1f)
        rule.setContent {
            val transition = updateTransition(target.value)
            animFloat.value =
                transition
                    .animateFloat(
                        transitionSpec = {
                            if (AnimStates.From isTransitioningTo AnimStates.To) {
                                spring(dampingRatio = Spring.DampingRatioHighBouncy)
                            } else {
                                spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow,
                                )
                            }
                        }
                    ) {
                        when (it) {
                            AnimStates.From -> 0f
                            AnimStates.To -> 1f
                        }
                    }
                    .value

            animColor.value =
                transition
                    .animateColor(transitionSpec = { tween(durationMillis = 1000) }) {
                        when (it) {
                            AnimStates.From -> Color.Red
                            AnimStates.To -> Color.Green
                        }
                    }
                    .value

            animFloatWithKeyframes.value =
                transition
                    .animateFloat(
                        transitionSpec = {
                            if (AnimStates.From isTransitioningTo AnimStates.To) {
                                keyframes1
                            } else {
                                keyframes2
                            }
                        }
                    ) {
                        // Same values for all states, but different transitions from state to
                        // state.
                        0f
                    }
                    .value

            if (transition.isRunning) {
                if (transition.targetState == AnimStates.To) {
                    assertEquals(
                        floatAnim1.getValueFromNanos(transition.playTimeNanos),
                        animFloat.value,
                        0.00001f,
                    )
                    assertEquals(
                        colorAnim1.getValueFromNanos(transition.playTimeNanos),
                        animColor.value,
                    )
                    assertEquals(
                        keyframesAnim1.getValueFromNanos(transition.playTimeNanos),
                        animFloatWithKeyframes.value,
                        0.00001f,
                    )

                    assertEquals(AnimStates.To, transition.segment.targetState)
                    assertEquals(AnimStates.From, transition.segment.initialState)
                } else {
                    assertEquals(
                        floatAnim2.getValueFromNanos(transition.playTimeNanos),
                        animFloat.value,
                        0.00001f,
                    )
                    assertEquals(
                        colorAnim2.getValueFromNanos(transition.playTimeNanos),
                        animColor.value,
                    )
                    assertEquals(
                        keyframesAnim2.getValueFromNanos(transition.playTimeNanos),
                        animFloatWithKeyframes.value,
                        0.00001f,
                    )
                    assertEquals(AnimStates.From, transition.segment.targetState)
                    assertEquals(AnimStates.To, transition.segment.initialState)
                }
            }
        }

        assertEquals(0f, animFloat.value)
        assertEquals(Color.Red, animColor.value)
        rule.runOnIdle { target.value = AnimStates.To }
        rule.waitForIdle()

        assertEquals(1f, animFloat.value)
        assertEquals(Color.Green, animColor.value)

        // Animate back to the `from` state
        rule.runOnIdle { target.value = AnimStates.From }
        rule.waitForIdle()

        assertEquals(0f, animFloat.value)
        assertEquals(Color.Red, animColor.value)
    }

    @Test
    fun startPulsingNextFrameTest() {
        val target = mutableStateOf(AnimStates.From)
        var playTime by mutableStateOf(0L)
        rule.setContent {
            val transition = updateTransition(target.value)
            val actual =
                transition.animateFloat(transitionSpec = { tween(200) }) {
                    if (it == AnimStates.From) 0f else 1000f
                }

            val anim = TargetBasedAnimation(tween(200), Float.VectorConverter, 0f, 1000f)

            if (target.value == AnimStates.To) {
                LaunchedEffect(transition) {
                    val startTime = withFrameNanos { it }

                    assertEquals(0f, actual.value)
                    do {
                        playTime = withFrameNanos { it } - startTime
                        assertEquals(anim.getValueFromNanos(playTime), actual.value)
                    } while (playTime <= 200 * MillisToNanos)
                }
            }
        }

        rule.runOnIdle { target.value = AnimStates.To }
        rule.waitForIdle()
        assertTrue(playTime > 200 * MillisToNanos)
    }

    @OptIn(InternalAnimationApi::class)
    @Test
    fun addNewAnimationInFlightTest() {
        val target = mutableStateOf(AnimStates.From)
        var playTime by mutableStateOf(0L)
        rule.setContent {
            val transition = updateTransition(target.value)

            transition.animateFloat(transitionSpec = { tween(1000) }) {
                if (it == AnimStates.From) -100f else 0f
            }

            if (transition.playTimeNanos > 0) {
                val startTime = remember { transition.playTimeNanos }
                val laterAdded =
                    transition.animateFloat(transitionSpec = { tween(800) }) {
                        if (it == AnimStates.From) 0f else 1000f
                    }
                val anim = TargetBasedAnimation(tween(800), Float.VectorConverter, 0f, 1000f)
                playTime = transition.playTimeNanos - startTime
                assertEquals(anim.getValueFromNanos(playTime), laterAdded.value)
            }
        }

        rule.runOnIdle { target.value = AnimStates.To }
        rule.waitForIdle()
        assertTrue(playTime > 800 * MillisToNanos)
    }

    @Test
    fun initialStateTest() {
        val target = MutableTransitionState(AnimStates.From)
        target.targetState = AnimStates.To
        var playTime by mutableStateOf(0L)
        var floatAnim: State<Float>? = null
        rule.setContent {
            val transition = rememberTransition(target)
            floatAnim =
                transition.animateFloat(transitionSpec = { tween(800) }) {
                    if (it == AnimStates.From) 0f else 1000f
                }
            // Verify that animation starts right away
            LaunchedEffect(transition) {
                val startTime = withFrameNanos { it }
                val anim = TargetBasedAnimation(tween(800), Float.VectorConverter, 0f, 1000f)
                while (!anim.isFinishedFromNanos(playTime)) {
                    playTime = withFrameNanos { it } - startTime
                    assertEquals(anim.getValueFromNanos(playTime), floatAnim?.value)
                }
            }
        }
        rule.waitForIdle()
        assertTrue(playTime >= 800 * MillisToNanos)
        assertEquals(1000f, floatAnim?.value)
    }

    @Test
    fun recreatingMutableStatesAmidTransition() {
        var playTime by mutableStateOf(0L)
        var targetRecreated by mutableStateOf(false)
        rule.setContent {
            var target by remember { mutableStateOf(MutableTransitionState(AnimStates.From)) }
            target.targetState = AnimStates.To
            val transition = rememberTransition(target)
            val floatAnim =
                transition.animateFloat(transitionSpec = { tween(800) }) {
                    if (it == AnimStates.From) 0f else 1000f
                }
            LaunchedEffect(Unit) {
                delay(100)
                target = MutableTransitionState(AnimStates.From)
                target.targetState = AnimStates.To
                targetRecreated = true
            }

            if (targetRecreated) {
                LaunchedEffect(transition) {
                    // Verify that animation restarted
                    assertEquals(0f, floatAnim.value)

                    val startTime = withFrameNanos { it }
                    val anim = TargetBasedAnimation(tween(800), Float.VectorConverter, 0f, 1000f)
                    while (!anim.isFinishedFromNanos(playTime)) {
                        playTime = withFrameNanos { it } - startTime
                        assertEquals(anim.getValueFromNanos(playTime), floatAnim.value)
                    }
                }
            }
        }

        rule.waitForIdle()
        assertTrue(targetRecreated)
        assertTrue(playTime >= 800 * MillisToNanos)
    }

    @OptIn(ExperimentalTransitionApi::class)
    @Test
    fun testMutableTransitionStateIsIdle() {
        val mutableTransitionState = MutableTransitionState(false)
        var transition: Transition<Boolean>? = null
        rule.setContent {
            transition =
                rememberTransition(mutableTransitionState).apply {
                    animateFloat { if (it) 1f else 0f }
                }
        }
        rule.mainClock.autoAdvance = false
        rule.runOnIdle {
            assertTrue(mutableTransitionState.isIdle)
            mutableTransitionState.targetState = true
            assertFalse(mutableTransitionState.isIdle)
        }

        while (transition?.currentState != true) {
            // Animation has not finished or even started from Transition's perspective
            assertFalse(mutableTransitionState.isIdle)
            rule.mainClock.advanceTimeByFrame()
        }
        assertTrue(mutableTransitionState.isIdle)

        // Now that transition false -> true finished, go back to false
        rule.runOnIdle {
            assertTrue(mutableTransitionState.isIdle)
            mutableTransitionState.targetState = false
            assertFalse(mutableTransitionState.isIdle)
        }

        while (transition?.currentState == true) {
            // Animation has not finished or even started from Transition's perspective
            assertFalse(mutableTransitionState.isIdle)
            rule.mainClock.advanceTimeByFrame()
        }
        assertTrue(mutableTransitionState.isIdle)
    }

    @OptIn(ExperimentalTransitionApi::class, InternalAnimationApi::class)
    @Test
    fun testCreateChildTransition() {
        val intState = mutableStateOf(1)
        val parentTransitionFloat = mutableStateOf(1f)
        val childTransitionFloat = mutableStateOf(1f)
        rule.setContent {
            val transition = updateTransition(intState.value)
            parentTransitionFloat.value =
                transition
                    .animateFloat({ tween(100) }) {
                        when (it) {
                            0 -> 0f
                            1 -> 1f
                            else -> 2f
                        }
                    }
                    .value
            val booleanTransition = transition.createChildTransition { it == 1 }
            childTransitionFloat.value =
                booleanTransition.animateFloat({ tween(500) }) { if (it) 1f else 0f }.value
            LaunchedEffect(intState.value) {
                while (true) {
                    if (transition.targetState == transition.currentState) {
                        break
                    }
                    withFrameNanos { it }
                    if (intState.value == 0) {
                        // 1 -> 0
                        if (
                            transition.playTimeNanos > 0 && transition.playTimeNanos <= 500_000_000L
                        ) {
                            assertTrue(transition.isRunning)
                            assertTrue(booleanTransition.isRunning)
                        }
                    } else if (intState.value == 2) {
                        // 0 -> 2
                        assertFalse(booleanTransition.isRunning)
                        if (transition.playTimeNanos > 120_000_000L) {
                            assertFalse(transition.isRunning)
                        } else if (transition.playTimeNanos > 0) {
                            assertTrue(transition.isRunning)
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1f, parentTransitionFloat.value)
            assertEquals(1f, childTransitionFloat.value)
            intState.value = 0
        }
        rule.runOnIdle {
            assertEquals(0f, parentTransitionFloat.value)
            assertEquals(0f, childTransitionFloat.value)
            intState.value = 2
        }
        rule.runOnIdle {
            assertEquals(2f, parentTransitionFloat.value)
            assertEquals(0f, childTransitionFloat.value)
        }
    }

    @OptIn(ExperimentalTransitionApi::class)
    @Test
    fun addAnimationToCompletedChildTransition() {
        rule.mainClock.autoAdvance = false
        var value1 = 0f
        var value2 = 0f
        var value3 = 0f
        lateinit var coroutineScope: CoroutineScope
        val state = MutableTransitionState(false)

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val parent = rememberTransition(state)
            value1 =
                parent
                    .animateFloat({ tween(1600, easing = LinearEasing) }) { if (it) 1000f else 0f }
                    .value

            val child = parent.createChildTransition { it }
            value2 =
                child
                    .animateFloat({ tween(160, easing = LinearEasing) }) { if (it) 1000f else 0f }
                    .value

            value3 =
                if (!parent.targetState) {
                    child
                        .animateFloat({ tween(160, easing = LinearEasing) }) {
                            if (it) 0f else 1000f
                        }
                        .value
                } else {
                    0f
                }
        }
        coroutineScope.launch { state.targetState = true }
        rule.mainClock.advanceTimeByFrame() // wait for composition
        rule.runOnIdle {
            assertEquals(0f, value1, 0f)
            assertEquals(0f, value2, 0f)
            assertEquals(0f, value3, 0f)
        }
        rule.mainClock.advanceTimeByFrame() // latch the animation start value
        rule.runOnIdle {
            assertEquals(0f, value1, 0f)
            assertEquals(0f, value2, 0f)
            assertEquals(0f, value3, 0f)
        }
        rule.mainClock.advanceTimeByFrame() // first frame of animation
        rule.runOnIdle {
            assertEquals(10f, value1, 0.1f)
            assertEquals(100f, value2, 0.1f)
            assertEquals(0f, value3, 0f) // hasn't started yet
        }
        rule.mainClock.advanceTimeBy(160)
        rule.runOnIdle {
            assertEquals(110f, value1, 0.1f)
            assertEquals(1000f, value2, 0f)
            assertEquals(0f, value3, 0f) // hasn't started yet
        }
        coroutineScope.launch { state.targetState = false }
        rule.mainClock.advanceTimeByFrame() // compose the change
        rule.runOnIdle {
            assertEquals(120f, value1, 0.1f)
            assertEquals(1000f, value2, 0f)
            assertEquals(0f, value3, 0f)
        }
        rule.mainClock.advanceTimeByFrame()
        var prevValue1 = 120f
        var prevValue2 = 1000f
        rule.runOnIdle {
            // value1 and value2 have spring interrupted values, so we can't
            // easily know their exact values
            assertTrue(value1 < prevValue1)
            prevValue1 = value1
            assertTrue(value2 < prevValue2)
            prevValue2 = value2
            assertEquals(100f, value3, 0.1f)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertTrue(value1 < prevValue1)
            assertTrue(value2 < prevValue2)
            assertEquals(200f, value3, 0.1f)
        }
    }

    @Test
    fun snapshotTotalDurationNanos() {
        val durations = mutableLongListOf()
        rule.mainClock.autoAdvance = false
        rule.setContent {
            var targetState by remember { mutableStateOf(false) }
            val transition = updateTransition(targetState, label = "")

            transition.AnimatedContent { _ -> }

            LaunchedEffect(Unit) {
                delay(200)
                targetState = true

                snapshotFlow { transition.totalDurationNanos }.collect { durations += it }
            }
        }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle { assertThat(durations.size).isEqualTo(0) }

        rule.mainClock.advanceTimeBy(200)
        rule.runOnIdle { assertThat(durations.size).isGreaterThan(0) }
    }

    @Test
    fun testRecompositionCountWhenPassingTransition() {
        // Verify that when passing Transition object to the composable (in contrast to
        // TransitionState), there is no extra recomposition. More specifically, rememberTransition
        // and Transition.animateFloat/animateValue only triggers one recomposition per
        // transition (i.e. per target state change).
        var showContent by mutableStateOf(false)
        var transitionState by mutableStateOf(MutableTransitionState(false))
        val recompositionCount = arrayOf(0, 0)
        rule.setContent {
            @Composable
            fun animateContentAlpha(transition: Transition<Boolean>): State<Float> {
                val animationSpec = tween<Float>(durationMillis = 2000)
                return transition.animateFloat(
                    transitionSpec = { animationSpec },
                    label = "background-scrim-alpha",
                ) { stage ->
                    if (stage) 1f else 0f
                }
            }
            val transition = rememberTransition(transitionState)

            val shouldShow by remember {
                derivedStateOf { showContent || transitionState.currentState }
            }

            Box(Modifier.fillMaxSize()) {}
            recompositionCount[if (shouldShow) 1 else 0] += 1
            if (shouldShow) {
                val contentAlpha by animateContentAlpha(transition)
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .graphicsLayer { alpha = contentAlpha }
                            .background(Color.Red)
                ) {}
            }
        }
        rule.runOnIdle {
            showContent = true
            transitionState.targetState = true
        }
        rule.runOnIdle {
            // Verify that there is exactly one recomposition for each target state
            assertEquals(1, recompositionCount[0])
            assertEquals(1, recompositionCount[1])
        }
    }

    @Test
    fun animateFloatCallerRecompositionCount() {
        @Composable
        fun TestAnimatedContent(
            transitionState: MutableTransitionState<Boolean>,
            onRecomposition: () -> Unit,
        ) {
            onRecomposition()
            val transition = rememberTransition(transitionState)
            transition.animateFloat { state -> if (state) 1f else 0f }
        }

        var recompositionCount = 0
        val transitionState = MutableTransitionState(false)
        rule.setContent {
            if (transitionState.targetState) {
                TestAnimatedContent(transitionState, { recompositionCount++ })
            }
        }

        rule.runOnIdle { transitionState.targetState = true }

        rule.runOnIdle {
            assertEquals(1, recompositionCount)
            assertTrue(transitionState.currentState)
        }
    }

    @OptIn(ExperimentalTransitionApi::class)
    @Test
    fun childTransitionStartsUninterrupted_usingTransitionState() {

        val transitionState = MutableTransitionState(0)

        rule.setContent {
            val transition = rememberTransition(transitionState)
            val childTransition =
                transition.createChildTransition(transformToChildState = { it > 1 })
            val color by
                childTransition.animateColor(
                    transitionSpec = {
                        // Use a keyframe overriding the color at the start of the animation to make
                        // it
                        // easy to distinguish from the interrupted AnimationSpec
                        keyframes { Color.Yellow atFraction 0f }
                    },
                    targetValueByState = { if (it) Color.Red else Color.Blue },
                )

            Column {
                Text(
                    // Presenting the Color as text to avoid capturing into images
                    text = color.toString(),
                    modifier = Modifier.testTag("animatedColor"),
                )
                // Presents the currentState of the TransitionState, used as an indicator of the
                // Transition animation as it will be updated to the target state when the
                // animation finishes, or immediately if there's no animation
                Text(
                    text = transitionState.currentState.toString(),
                    modifier = Modifier.testTag("currentStateText"),
                )
            }
        }
        rule.waitForIdle()

        // Check initial values
        rule.onNodeWithTag("animatedColor").assertTextEquals(Color.Blue.toString())
        rule.onNodeWithTag("currentStateText").assertTextEquals("0")

        rule.mainClock.autoAdvance = false

        // In this case, state changes that does NOT update the child transition shouldn't trigger
        // an animation
        transitionState.targetState = 1
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        // If there was any animation we'd see the keyframe yellow color and/or the old currentState
        rule.onNodeWithTag("animatedColor").assertTextEquals(Color.Blue.toString())
        rule.onNodeWithTag("currentStateText").assertTextEquals("1")

        // Move to the first animated target state change (changes to True in the child transition)
        transitionState.targetState = 2

        // Move to first animated frame
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        // Box should be yellow, text should still have its "old" value as the transition hasn't
        // finished
        rule.onNodeWithTag("animatedColor").assertTextEquals(Color.Yellow.toString())
        rule.onNodeWithTag("currentStateText").assertTextEquals("1")

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        // Wait until it finishes. We should see the final Red Color and the Text representing the
        // updated currentState
        rule.onNodeWithTag("animatedColor").assertTextEquals(Color.Red.toString())
        rule.onNodeWithTag("currentStateText").assertTextEquals("2")

        rule.mainClock.autoAdvance = false

        // We've observed that going from a non-animation change to an animated state change would
        // also trigger interruption
        transitionState.targetState = 3
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        // No animation
        rule.onNodeWithTag("animatedColor").assertTextEquals(Color.Red.toString())
        rule.onNodeWithTag("currentStateText").assertTextEquals("3")

        // This change should trigger an animation, no interruption expected
        transitionState.targetState = 1
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        // Animation with keyframe Yellow Color, currentState is "3" due to pending animation
        rule.onNodeWithTag("animatedColor").assertTextEquals(Color.Yellow.toString())
        rule.onNodeWithTag("currentStateText").assertTextEquals("3")
    }

    @OptIn(ExperimentalTransitionApi::class)
    @Test
    fun childTransitionStartsUninterrupted_usingSeekableTransition() {
        val transitionState = SeekableTransitionState(0)
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            val transition = rememberTransition(transitionState)
            val childTransition =
                transition.createChildTransition(transformToChildState = { it > 1 })
            val color by
                childTransition.animateColor(
                    transitionSpec = {
                        // Use a keyframe overriding the color at the start of the animation to make
                        // it
                        // easy to distinguish from the interrupted AnimationSpec
                        keyframes { Color.Yellow atFraction 0f }
                    },
                    targetValueByState = { if (it) Color.Red else Color.Blue },
                )

            Column {
                Text(
                    // Presenting the Color as text to avoid capturing into images
                    text = color.toString(),
                    modifier = Modifier.testTag("animatedColor"),
                )
                // Presents the currentState of the TransitionState, used as an indicator of the
                // Transition animation as it will be updated to the target state when the
                // animation finishes, or immediately if there's no animation
                Text(
                    text = transitionState.currentState.toString(),
                    modifier = Modifier.testTag("currentStateText"),
                )
            }
        }
        rule.waitForIdle()

        // Check initial values
        rule.onNodeWithTag("animatedColor").assertTextEquals(Color.Blue.toString())
        rule.onNodeWithTag("currentStateText").assertTextEquals("0")

        rule.mainClock.autoAdvance = false

        // In this case, state changes that does NOT update the child transition shouldn't trigger
        // an animation
        rule.runOnUiThread { coroutineScope.launch { transitionState.animateTo(1) } }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        // If there was any animation we'd see the keyframe yellow color and/or the old currentState
        rule.onNodeWithTag("animatedColor").assertTextEquals(Color.Blue.toString())
        rule.onNodeWithTag("currentStateText").assertTextEquals("1")

        // Move to the first animated target state change (changes to True in the child transition)
        rule.runOnUiThread {
            coroutineScope.launch {
                transitionState.seekTo(0f, 2)
                transitionState.animateTo(2)
            }
        }

        // Move to first animated frame
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        // Box should be yellow, text should still have its "old" value as the transition hasn't
        // finished
        rule.onNodeWithTag("animatedColor").assertTextEquals(Color.Yellow.toString())
        rule.onNodeWithTag("currentStateText").assertTextEquals("1")

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        // Wait until it finishes. We should see the final Red Color and the Text representing the
        // updated currentState
        rule.onNodeWithTag("animatedColor").assertTextEquals(Color.Red.toString())
        rule.onNodeWithTag("currentStateText").assertTextEquals("2")
    }

    @Test
    fun recreatedTransition_animatesAsExpected() {
        val animDuration = 10 * 16 // have to assume frame time duration

        // Generic number to change calculations between test cases
        var baseValue = 0

        var transitionState by mutableStateOf(MutableTransitionState(AnimStates.From))

        fun recreateTransitionState(initialState: AnimStates) {
            val state = MutableTransitionState(initialState)

            if (baseValue == 0) {
                transitionState = state
            } else {
                // Double check
                assert(transitionState != state)
                transitionState = state
            }
        }

        /**
         * Convenient method to generate distinct animation values from the Transition targetState
         * and a `baseValue`
         */
        @Suppress("REDUNDANT_ELSE_IN_WHEN") // Easier to change when using 'else'
        fun animationValueFor(animState: AnimStates): Float =
            when (animState) {
                AnimStates.From -> {
                    // values: 0, 2
                    baseValue * 2f
                }
                AnimStates.To -> {
                    // values: 1, 3
                    (baseValue * 2f) + 1f
                }
                else -> {
                    throw Exception("Unexpected target value")
                }
            }

        rule.setContent {
            val transition = rememberTransition(transitionState)

            val animatedFloat =
                transition.animateFloat(
                    transitionSpec = {
                        when {
                            AnimStates.From isTransitioningTo AnimStates.To -> {
                                // If segment is updated properly across recomposition, this
                                // AnimationSpec should be used
                                tween(animDuration, easing = LinearEasing)
                            }
                            else -> {
                                // If we end up using stale `segment` information it will be
                                // evident since it'll use this animationSpec from the initial setup
                                // meaning initial and target state will be the same value
                                tween(animDuration * 2, easing = LinearEasing)
                            }
                        }
                    }
                ) { targetState ->
                    // Here we'll generate distinct animation targets for each state and `baseValue`
                    animationValueFor(targetState)
                }
            Text(text = animatedFloat.value.toString(), modifier = Modifier.testTag("text"))
        }
        rule.waitForIdle()
        rule.onNodeWithTag("text").assertTextEquals("0.0")
        assertEquals(AnimStates.From, transitionState.currentState)
        assertEquals(AnimStates.From, transitionState.targetState)

        // Here we effectively recreate the Transition with the same values, however if we don't
        // invalidate `segment` properly, the rest of the animations will always calculate using
        // AnimStates.From for current and target state
        recreateTransitionState(AnimStates.From)

        rule.mainClock.autoAdvance = false
        transitionState.targetState = AnimStates.To
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        // Test at half duration
        rule.onNodeWithTag("text").assertTextEquals("0.0")
        rule.mainClock.advanceTimeBy(animDuration / 2L)
        rule.onNodeWithTag("text").assertTextEquals("0.5")

        // Advance until animations are finished
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertFalse(transitionState.isRunning)
        assertEquals(AnimStates.To, transitionState.targetState)
        assertEquals(AnimStates.To, transitionState.currentState)

        rule.mainClock.autoAdvance = false
        // Update base value for calculations and force a recompostion by recreating the
        // TransitionState
        baseValue = 1
        recreateTransitionState(AnimStates.From)

        // Reset Transition, despite moving to AnimState.From, there should be no animation, visible
        // on the target/current state and text with animated value
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(AnimStates.From, transitionState.targetState)
        assertEquals(AnimStates.From, transitionState.currentState)
        rule.onNodeWithTag("text").assertTextEquals("2.0")

        // Trigger another animation, this time it should reflect with the new values from 2f to 3f
        transitionState.targetState = AnimStates.To
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        rule.mainClock.advanceTimeBy(animDuration / 2L)
        assertEquals(AnimStates.To, transitionState.targetState)
        assertEquals(AnimStates.From, transitionState.currentState)
        rule.onNodeWithTag("text").assertTextEquals("2.5")
    }
}
