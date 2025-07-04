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

package androidx.wear.compose.material3

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.wear.compose.material3.RevealDirection.Companion.RightToLeft
import androidx.wear.compose.material3.RevealState.SingleSwipeCoordinator
import androidx.wear.compose.material3.RevealValue.Companion.Covered
import androidx.wear.compose.material3.RevealValue.Companion.LeftRevealed
import androidx.wear.compose.material3.RevealValue.Companion.LeftRevealing
import androidx.wear.compose.material3.RevealValue.Companion.RightRevealed
import androidx.wear.compose.material3.RevealValue.Companion.RightRevealing
import androidx.wear.compose.material3.SwipeToRevealDefaults.DoubleActionAnchorWidth
import androidx.wear.compose.material3.SwipeToRevealDefaults.SingleActionAnchorWidth
import androidx.wear.compose.material3.SwipeToRevealDefaults.bidirectionalGestureInclusion
import androidx.wear.compose.materialcore.CustomTouchSlopProvider
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests to check the behavior of the anchors of [SwipeToReveal].
 *
 * Gesture inclusion parameter is always set to [bidirectionalGestureInclusion] in order to allow
 * all the gestures. Behavior of gesture inclusion parameter is checked in [SwipeToRevealTest].
 */
@RunWith(Parameterized::class)
class SwipeToRevealAnchorTest(val testParams: TestParams) {
    @get:Rule val rule = createComposeRule()

    @Before
    fun setUp() {
        SingleSwipeCoordinator.lastUpdatedState.set(null)
    }

    @Test
    fun performSwipe_settlesOnCorrectAnchor() {
        lateinit var revealState: RevealState
        var density = 0f

        rule.setContent {
            ScreenConfiguration(screenSizeDp = SCREEN_SIZE_LARGE) {
                revealState = rememberRevealState(initialValue = testParams.initialRevealValue)
                with(LocalDensity.current) { density = this.density }
                Content(revealState)
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput(block = gesture(testParams, density))

        rule.runOnIdle { assertEquals(testParams.expectedRevealValue, revealState.currentValue) }
    }

    @Composable
    private fun Content(revealState: RevealState) {
        CustomTouchSlopProvider(newTouchSlop = 0f) {
            SwipeToReveal(
                primaryAction = {
                    PrimaryActionButton(
                        onClick = {},
                        icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                        text = { Text("Delete") },
                    )
                },
                onSwipePrimaryAction = {},
                modifier = Modifier.testTag(TEST_TAG),
                secondaryAction =
                    if (testParams.actions == Actions.Two) {
                        {
                            SecondaryActionButton(
                                onClick = {},
                                icon = {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                                },
                            )
                        }
                    } else null,
                revealState = revealState,
                revealDirection = testParams.revealDirection,
                hasPartiallyRevealedState =
                    if (testParams.actions is Actions.One) {
                        testParams.actions.hasPartiallyRevealedState
                    } else true,
                gestureInclusion = bidirectionalGestureInclusion,
            ) {
                Button({}, Modifier.fillMaxWidth()) { Text("Swipe me!") }
            }
        }
    }

    private fun gesture(testParams: TestParams, density: Float): TouchInjectionScope.() -> Unit {
        val anchor =
            when (testParams.actions) {
                is Actions.One -> SingleActionAnchorWidth.value
                Actions.Two -> DoubleActionAnchorWidth.value
            }
        val endPosition = testParams.swipeDirection.anchorFractionDistance * anchor * density
        return when (testParams.swipeDirection) {
            is SwipeDirection.Left -> {
                { swipeLeft(startX = endPosition, durationMillis = SWIPE_DURATION) }
            }
            is SwipeDirection.Right -> {
                { swipeRight(endX = endPosition, durationMillis = SWIPE_DURATION) }
            }
        }
    }

    sealed class TestParams(
        val revealDirection: RevealDirection,
        open val actions: Actions,
        open val swipeDirection: SwipeDirection,
        open val initialRevealValue: RevealValue,
        open val expectedRevealValue: RevealValue,
    ) {
        data class RTL(
            override val actions: Actions,
            override val initialRevealValue: RevealValue,
            override val swipeDirection: SwipeDirection,
            override val expectedRevealValue: RevealValue,
        ) :
            TestParams(
                revealDirection = RightToLeft,
                actions = actions,
                initialRevealValue = initialRevealValue,
                swipeDirection = swipeDirection,
                expectedRevealValue = expectedRevealValue,
            )

        data class Bidirectional(
            override val actions: Actions,
            override val initialRevealValue: RevealValue,
            override val swipeDirection: SwipeDirection,
            override val expectedRevealValue: RevealValue,
        ) :
            TestParams(
                revealDirection = RevealDirection.Bidirectional,
                actions = actions,
                initialRevealValue = initialRevealValue,
                swipeDirection = swipeDirection,
                expectedRevealValue = expectedRevealValue,
            )
    }

    sealed class Actions(open val hasPartiallyRevealedState: Boolean) {
        data class One(override val hasPartiallyRevealedState: Boolean) :
            Actions(hasPartiallyRevealedState)

        object Two : Actions(true) {
            override fun toString(): String = this::class.simpleName!!
        }
    }

    sealed class SwipeDirection(open val anchorFractionDistance: Float) {
        data class Left(override val anchorFractionDistance: Float) :
            SwipeDirection(anchorFractionDistance)

        data class Right(override val anchorFractionDistance: Float) :
            SwipeDirection(anchorFractionDistance)
    }

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun data(): Iterable<Array<Any>> {
            return listOf(
                    TestParams.RTL(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.RTL(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_BEFORE_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = RightRevealing,
                    ),
                    TestParams.RTL(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = Covered,
                        swipeDirection = SwipeDirection.Left(COVERED_TO_AFTER_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = RightRevealing,
                    ),
                    TestParams.RTL(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_ONE),
                        expectedRevealValue = RightRevealed,
                    ),
                    TestParams.RTL(
                        actions = Actions.One(hasPartiallyRevealedState = false),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.RTL(
                        actions = Actions.One(hasPartiallyRevealedState = false),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_BEFORE_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.RTL(
                        actions = Actions.One(hasPartiallyRevealedState = false),
                        initialRevealValue = Covered,
                        swipeDirection = SwipeDirection.Left(COVERED_TO_AFTER_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.RTL(
                        actions = Actions.One(hasPartiallyRevealedState = false),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_ONE),
                        expectedRevealValue = RightRevealed,
                    ),
                    TestParams.RTL(
                        actions = Actions.Two,
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.RTL(
                        actions = Actions.Two,
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_BEFORE_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = RightRevealing,
                    ),
                    TestParams.RTL(
                        actions = Actions.Two,
                        initialRevealValue = Covered,
                        swipeDirection = SwipeDirection.Left(COVERED_TO_AFTER_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = RightRevealing,
                    ),
                    TestParams.RTL(
                        actions = Actions.Two,
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_TWO),
                        expectedRevealValue = RightRevealed,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_BEFORE_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = RightRevealing,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = Covered,
                        swipeDirection = SwipeDirection.Left(COVERED_TO_AFTER_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = RightRevealing,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_ONE),
                        expectedRevealValue = RightRevealed,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = false),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = false),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_BEFORE_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = false),
                        initialRevealValue = Covered,
                        swipeDirection = SwipeDirection.Left(COVERED_TO_AFTER_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = false),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_ONE),
                        expectedRevealValue = RightRevealed,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.Two,
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.Two,
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_BEFORE_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = RightRevealing,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.Two,
                        initialRevealValue = Covered,
                        swipeDirection = SwipeDirection.Left(COVERED_TO_AFTER_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = RightRevealing,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.Two,
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Left(COVERED_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_TWO),
                        expectedRevealValue = RightRevealed,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Right(COVERED_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Right(COVERED_TO_BEFORE_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = LeftRevealing,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Right(COVERED_TO_AFTER_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = LeftRevealing,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Right(COVERED_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_ONE),
                        expectedRevealValue = LeftRevealed,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = false),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Right(COVERED_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = false),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Right(COVERED_TO_BEFORE_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = false),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Right(COVERED_TO_AFTER_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = false),
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Right(COVERED_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_ONE),
                        expectedRevealValue = LeftRevealed,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.Two,
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Right(COVERED_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.Two,
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Right(COVERED_TO_BEFORE_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = LeftRevealing,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.Two,
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Right(COVERED_TO_AFTER_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = LeftRevealing,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.Two,
                        initialRevealValue = Covered,
                        swipeDirection =
                            SwipeDirection.Right(COVERED_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_TWO),
                        expectedRevealValue = LeftRevealed,
                    ),
                    TestParams.RTL(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = RightRevealing,
                        swipeDirection =
                            SwipeDirection.Right(REVEALING_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.RTL(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = RightRevealing,
                        swipeDirection =
                            SwipeDirection.Right(REVEALING_TO_BEFORE_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = RightRevealing,
                    ),
                    TestParams.RTL(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = RightRevealing,
                        swipeDirection =
                            SwipeDirection.Left(REVEALING_TO_AFTER_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = RightRevealing,
                    ),
                    TestParams.RTL(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = RightRevealing,
                        swipeDirection =
                            SwipeDirection.Left(REVEALING_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_ONE),
                        expectedRevealValue = RightRevealed,
                    ),
                    TestParams.RTL(
                        actions = Actions.Two,
                        initialRevealValue = RightRevealing,
                        swipeDirection =
                            SwipeDirection.Right(REVEALING_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.RTL(
                        actions = Actions.Two,
                        initialRevealValue = RightRevealing,
                        swipeDirection =
                            SwipeDirection.Right(REVEALING_TO_BEFORE_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = RightRevealing,
                    ),
                    TestParams.RTL(
                        actions = Actions.Two,
                        initialRevealValue = RightRevealing,
                        swipeDirection =
                            SwipeDirection.Left(REVEALING_TO_AFTER_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = RightRevealing,
                    ),
                    TestParams.RTL(
                        actions = Actions.Two,
                        initialRevealValue = RightRevealing,
                        swipeDirection =
                            SwipeDirection.Left(REVEALING_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_ONE),
                        expectedRevealValue = RightRevealed,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = RightRevealing,
                        swipeDirection =
                            SwipeDirection.Right(REVEALING_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = RightRevealing,
                        swipeDirection =
                            SwipeDirection.Right(REVEALING_TO_BEFORE_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = RightRevealing,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = RightRevealing,
                        swipeDirection =
                            SwipeDirection.Left(REVEALING_TO_AFTER_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = RightRevealing,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = RightRevealing,
                        swipeDirection =
                            SwipeDirection.Left(REVEALING_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_ONE),
                        expectedRevealValue = RightRevealed,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.Two,
                        initialRevealValue = RightRevealing,
                        swipeDirection =
                            SwipeDirection.Right(REVEALING_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.Two,
                        initialRevealValue = RightRevealing,
                        swipeDirection =
                            SwipeDirection.Right(REVEALING_TO_BEFORE_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = RightRevealing,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.Two,
                        initialRevealValue = RightRevealing,
                        swipeDirection =
                            SwipeDirection.Left(REVEALING_TO_AFTER_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = RightRevealing,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.Two,
                        initialRevealValue = RightRevealing,
                        swipeDirection =
                            SwipeDirection.Left(REVEALING_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_TWO),
                        expectedRevealValue = RightRevealed,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = LeftRevealing,
                        swipeDirection =
                            SwipeDirection.Left(REVEALING_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = LeftRevealing,
                        swipeDirection =
                            SwipeDirection.Left(REVEALING_TO_BEFORE_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = LeftRevealing,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = LeftRevealing,
                        swipeDirection =
                            SwipeDirection.Right(REVEALING_TO_AFTER_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = LeftRevealing,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.One(hasPartiallyRevealedState = true),
                        initialRevealValue = LeftRevealing,
                        swipeDirection =
                            SwipeDirection.Right(REVEALING_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_ONE),
                        expectedRevealValue = LeftRevealed,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.Two,
                        initialRevealValue = LeftRevealing,
                        swipeDirection =
                            SwipeDirection.Left(REVEALING_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER),
                        expectedRevealValue = Covered,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.Two,
                        initialRevealValue = LeftRevealing,
                        swipeDirection =
                            SwipeDirection.Left(REVEALING_TO_BEFORE_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = LeftRevealing,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.Two,
                        initialRevealValue = LeftRevealing,
                        swipeDirection =
                            SwipeDirection.Right(REVEALING_TO_AFTER_ANCHOR_WITHIN_BUFFER),
                        expectedRevealValue = LeftRevealing,
                    ),
                    TestParams.Bidirectional(
                        actions = Actions.Two,
                        initialRevealValue = LeftRevealing,
                        swipeDirection =
                            SwipeDirection.Right(REVEALING_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_TWO),
                        expectedRevealValue = LeftRevealed,
                    ),
                )
                .map { arrayOf(it) }
        }

        /**
         * Long duration in order to not perform faster than the velocity threshold defined in
         * [androidx.wear.compose.materialcore.SwipeableV2Defaults.VelocityThreshold].
         */
        private const val SWIPE_DURATION = 2000L

        /**
         * [Anchor fraction distance][SwipeDirection.anchorFractionDistance] to perform a swipe
         * gesture to a position before the revealing anchor ([RightRevealing] or [LeftRevealing]),
         * outside the anchor's buffer. This assumes the component's [state][RevealState] has
         * [Covered] as [current value][RevealState.currentValue].
         */
        private const val COVERED_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER = 0.25f
        /**
         * [Anchor fraction distance][SwipeDirection.anchorFractionDistance] to perform a swipe
         * gesture to a position before the revealing anchor ([RightRevealing] or [LeftRevealing]),
         * within the anchor's buffer. This assumes the component's [state][RevealState] has
         * [Covered] as [current value][RevealState.currentValue].
         */
        private const val COVERED_TO_BEFORE_ANCHOR_WITHIN_BUFFER = 0.75f
        /**
         * [Anchor fraction distance][SwipeDirection.anchorFractionDistance] to perform a swipe
         * gesture to a position after the revealing anchor ([RightRevealing] or [LeftRevealing]),
         * within the anchor's buffer. This assumes the component's [state][RevealState] has
         * [Covered] as [current value][RevealState.currentValue].
         */
        private const val COVERED_TO_AFTER_ANCHOR_WITHIN_BUFFER = 1.25f
        /**
         * [Anchor fraction distance][SwipeDirection.anchorFractionDistance] to perform a swipe
         * gesture to a position after the revealing anchor ([RightRevealing] or [LeftRevealing]),
         * outside the anchor's buffer. This assumes the component's [state][RevealState] has
         * [Covered] as [current value][RevealState.currentValue] and single action. When using the
         * SCREEN_SIZE_LARGE configuration, the gesture targets a position 58% along the path from
         * its Revealing to its Revealed state.
         */
        private const val COVERED_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_ONE = 2.5f
        /**
         * [Anchor fraction distance][SwipeDirection.anchorFractionDistance] to perform a swipe
         * gesture to a position after the revealing anchor ([RightRevealing] or [LeftRevealing]),
         * outside the anchor's buffer. This assumes the component's [state][RevealState] has
         * [Covered] as [current value][RevealState.currentValue] and two actions. When using the
         * SCREEN_SIZE_LARGE configuration, the gesture targets a position 66% along the path from
         * its Revealing to its Revealed state.
         */
        private const val COVERED_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_TWO = 1.5f
        /**
         * [Anchor fraction distance][SwipeDirection.anchorFractionDistance] to perform a swipe
         * gesture to a position before the revealing anchor ([RightRevealing] or [LeftRevealing]),
         * outside the anchor's buffer. This assumes the component's [state][RevealState] has
         * [RightRevealing] or [LeftRevealing] as [current value][RevealState.currentValue].
         */
        private const val REVEALING_TO_BEFORE_ANCHOR_OUTSIDE_BUFFER = 0.75f
        /**
         * [Anchor fraction distance][SwipeDirection.anchorFractionDistance] to perform a swipe
         * gesture to a position before the revealing anchor ([RightRevealing] or [LeftRevealing]),
         * within the anchor's buffer. This assumes the component's [state][RevealState] has
         * [RightRevealing] or [LeftRevealing] as [current value][RevealState.currentValue].
         */
        private const val REVEALING_TO_BEFORE_ANCHOR_WITHIN_BUFFER = 0.25f
        /**
         * [Anchor fraction distance][SwipeDirection.anchorFractionDistance] to perform a swipe
         * gesture to a position after the revealing anchor ([RightRevealing] or [LeftRevealing]),
         * within the anchor's buffer. This assumes the component's [state][RevealState] has
         * [RightRevealing] or [LeftRevealing] as [current value][RevealState.currentValue].
         */
        private const val REVEALING_TO_AFTER_ANCHOR_WITHIN_BUFFER = 0.25f
        /**
         * [Anchor fraction distance][SwipeDirection.anchorFractionDistance] to perform a swipe
         * gesture to a position after the revealing anchor ([RightRevealing] or [LeftRevealing]),
         * outside the anchor's buffer. This assumes the component's [state][RevealState] has
         * [RightRevealing] or [LeftRevealing] as [current value][RevealState.currentValue] and
         * single actions. The swipe distance will be 58% of distance between Revealing and Revealed
         * state, when using SCREEN_SIZE_LARGE configuration.
         */
        private const val REVEALING_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_ONE = 1.5f
        /**
         * [Anchor fraction distance][SwipeDirection.anchorFractionDistance] to perform a swipe
         * gesture to a position after the revealing anchor ([RightRevealing] or [LeftRevealing]),
         * outside the anchor's buffer. This assumes the component's [state][RevealState] has
         * [RightRevealing] or [LeftRevealing] as [current value][RevealState.currentValue] and two
         * actions. The swipe distance will be 66% of distance between Revealing and Revealed state,
         * when using SCREEN_SIZE_LARGE configuration.
         */
        private const val REVEALING_TO_AFTER_ANCHOR_OUTSIDE_BUFFER_TWO = 0.5f
    }
}
