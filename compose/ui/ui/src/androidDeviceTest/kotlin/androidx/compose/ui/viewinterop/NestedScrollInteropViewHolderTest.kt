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

package androidx.compose.ui.viewinterop

import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.tests.R
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class NestedScrollInteropViewHolderTest {
    @get:Rule val rule = createComposeRule()

    private val connection = InspectableNestedScrollConnection()
    private val recyclerViewConsumptionTracker = RecyclerViewConsumptionTracker()

    @Before
    fun setUp() {
        connection.reset()
        recyclerViewConsumptionTracker.reset()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun nestedScrollInteropFixDisabled_rootViewNestedScrollingDisabled_shouldNotPropagateDeltas() {
        try {
            AndroidComposeUiFlags.isNestedScrollInteropViewHolderFixEnabled = false
            rule.setContent {
                NestedScrollInteropWithView(
                    modifier = Modifier.nestedScroll(connection),
                    recyclerViewConsumptionTracker = recyclerViewConsumptionTracker,
                )
            }

            onView(withId(R.id.main_list)).perform(swipeUp())

            rule.runOnIdle { assertThat(connection.offeredFromChild).isEqualTo(Offset.Zero) }
        } finally {
            AndroidComposeUiFlags.isNestedScrollInteropViewHolderFixEnabled = true
        }
    }

    @Test
    fun nestedScrollInteropIsOn_shouldPropagateDeltas() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                recyclerViewConsumptionTracker = recyclerViewConsumptionTracker,
            )
        }

        // act
        onView(withId(R.id.main_layout)).perform(swipeUp())

        // assert
        rule.runOnIdle { assertThat(connection.offeredFromChild).isNotEqualTo(Offset.Zero) }
    }

    @Test
    fun nestedScrollInteropIsOn_checkDeltasCorrectlyPropagate() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                recyclerViewConsumptionTracker = recyclerViewConsumptionTracker,
            )
        }

        // act
        onView(withId(R.id.main_list)).perform(swipeUp())

        // assert
        rule.runOnIdle {
            assertThat(connection.offeredFromChild)
                .isEqualTo(recyclerViewConsumptionTracker.deltaConsumed)
        }
    }

    @Test
    fun nestedScrollInteropIsOn_checkDeltasCorrectlyPropagatePostScroll() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                recyclerViewConsumptionTracker = recyclerViewConsumptionTracker,
            )
        }

        // act
        onView(withId(R.id.main_list)).perform(swipeUp())

        // assert
        rule.runOnIdle {
            assertThat(connection.notConsumedByChild).isEqualTo(Offset.Zero)
            assertThat(connection.consumedDownChain)
                .isEqualTo(recyclerViewConsumptionTracker.deltaConsumed)
        }
    }

    @Test
    fun nestedScrollInteropIsOn_consumedUpChain_checkDeltasCorrectlyPropagatePostScroll() {
        // arrange
        // Hierarchy is:
        // Vertical Compose Scrollable
        // >>> AndroidView
        // >>>>>> Vertical Recycler View.
        rule.setContent {
            val controller = rememberScrollableState { it }

            Box(modifier = Modifier.scrollable(controller, Orientation.Vertical)) {
                NestedScrollInteropWithView(
                    modifier = Modifier.nestedScroll(connection),
                    recyclerViewConsumptionTracker = recyclerViewConsumptionTracker,
                )
            }
        }

        // act
        Espresso.onView(withId(R.id.main_list)).perform(swipeUp())

        // assert
        rule.runOnIdle {
            // Recycler View Consumed
            assertThat(recyclerViewConsumptionTracker.deltaConsumed)
                .isEqualTo(connection.consumedDownChain)
        }
    }

    @Test
    fun nestedScrollInteropOn_viewRequestsDisallow_shouldNotScrollCompose() {
        val state = ScrollState(0)
        val scrollViewId = Int.MAX_VALUE
        rule.setContent {
            val densiy = LocalDensity.current
            Column(modifier = Modifier.fillMaxSize().verticalScroll(state)) {
                AndroidView(
                    modifier =
                        Modifier.height(100.dp)
                            .fillMaxWidth()
                            .background(ComposeColor.Green)
                            .testTag("androidView"),
                    factory = { context ->
                        ScrollView(context).also { scrollView ->
                            scrollView.id = scrollViewId
                            LinearLayout(context).also { linearLayout ->
                                scrollView.addView(linearLayout)
                                linearLayout.orientation = LinearLayout.VERTICAL
                                linearLayout.gravity = Gravity.CENTER
                                repeat(10) {
                                    FrameLayout(context).also { iv ->
                                        linearLayout.addView(iv)
                                        iv.setBackgroundColor(Color.BLACK)
                                        iv.layoutParams =
                                            LinearLayout.LayoutParams(
                                                with(densiy) { 400.dp.roundToPx() },
                                                with(densiy) { 100.dp.roundToPx() },
                                            )
                                        val params = iv.layoutParams
                                        (params as LinearLayout.LayoutParams).setMargins(
                                            20,
                                            20,
                                            20,
                                            20,
                                        )
                                        iv.layoutParams = params
                                    }
                                }
                            }
                        }
                    },
                )

                repeat(10) {
                    Box(
                        Modifier.height(200.dp)
                            .fillMaxWidth()
                            .background(color = ComposeColor.Yellow)
                    )
                    Box(
                        Modifier.height(200.dp)
                            .fillMaxWidth()
                            .background(color = ComposeColor.White)
                    )
                    Box(
                        Modifier.height(200.dp).fillMaxWidth().background(color = ComposeColor.Blue)
                    )
                }
            }
        }

        onView(withId(scrollViewId)).perform(swipeUp())

        rule.runOnIdle { assertThat(state.value).isEqualTo(0) }
    }

    @Test
    fun nestedScrollInteropIsOn_checkDeltasCorrectlyPropagatePreFling() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                recyclerViewConsumptionTracker = recyclerViewConsumptionTracker,
            )
        }

        // act
        onView(withId(R.id.main_list)).perform(swipeUp())
        rule.waitForIdle()
        // assert
        rule.runOnIdle {
            assertThat(abs(connection.velocityOfferedFromChild))
                .isEqualTo(abs(recyclerViewConsumptionTracker.velocityConsumed))
        }
    }

    @Test
    fun nestedScrollInteropIsOn_checkDeltasCorrectlyPropagatePostFling() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                recyclerViewConsumptionTracker = recyclerViewConsumptionTracker,
            )
        }

        // act
        onView(withId(R.id.main_list)).perform(swipeUp())

        // assert
        rule.runOnIdle {
            assertThat(connection.velocityNotConsumedByChild).isEqualTo(Velocity.Zero)
            assertThat(connection.velocityConsumedDownChain)
                .isEqualTo(recyclerViewConsumptionTracker.velocityConsumed)
        }
    }

    @Test
    fun nestedScrollInteropIsOn_dynamicPaddingParent_updatesViewWindowOffsetSynchronously() {
        var heightOffsetPx by mutableFloatStateOf(0f)
        var maxOffsetPx = 0f
        val preScrollDeltasY = mutableListOf<Float>()
        val collapsingConnection =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    preScrollDeltasY.add(available.y)
                    val previousOffset = heightOffsetPx
                    heightOffsetPx = (heightOffsetPx + available.y).coerceIn(-maxOffsetPx, 0f)
                    return Offset(0f, heightOffsetPx - previousOffset)
                }
            }
        rule.setContent {
            maxOffsetPx = with(LocalDensity.current) { CollapsingHeaderHeight.toPx() }
            DynamicTopPaddingBox(
                topPaddingPxProvider = { (maxOffsetPx + heightOffsetPx).fastRoundToInt() },
                modifier = Modifier.fillMaxSize().nestedScroll(collapsingConnection),
            ) {
                NestedScrollInteropWithView(
                    recyclerViewConsumptionTracker = recyclerViewConsumptionTracker
                )
            }
        }

        onView(withId(R.id.main_list)).perform(swipeUp())

        rule.runOnIdle {
            assertThat(preScrollDeltasY).isNotEmpty()
            assertThat(preScrollDeltasY.all { it <= 0f }).isTrue()
            assertThat(heightOffsetPx).isEqualTo(-maxOffsetPx)
        }
    }

    @Test
    fun nestedScrollInteropIsOn_dynamicPaddingParent_postScroll_updatesViewWindowOffsetSynchronously() {
        var heightOffsetPx by mutableFloatStateOf(0f)
        var maxOffsetPx = 0f
        val postScrollDeltasY = mutableListOf<Float>()
        val expandingConnection =
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    postScrollDeltasY.add(available.y)
                    val previousOffset = heightOffsetPx
                    heightOffsetPx = (heightOffsetPx + available.y).coerceIn(-maxOffsetPx, 0f)
                    return Offset(0f, heightOffsetPx - previousOffset)
                }
            }
        rule.setContent {
            maxOffsetPx = with(LocalDensity.current) { CollapsingHeaderHeight.toPx() }
            heightOffsetPx = -maxOffsetPx
            DynamicTopPaddingBox(
                topPaddingPxProvider = { (maxOffsetPx + heightOffsetPx).fastRoundToInt() },
                modifier = Modifier.fillMaxSize().nestedScroll(expandingConnection),
            ) {
                NestedScrollInteropWithView(
                    recyclerViewConsumptionTracker = recyclerViewConsumptionTracker
                )
            }
        }

        onView(withId(R.id.main_list)).perform(swipeDown())

        rule.runOnIdle {
            assertThat(postScrollDeltasY).isNotEmpty()
            assertThat(postScrollDeltasY.all { it >= 0f }).isTrue()
            assertThat(heightOffsetPx).isEqualTo(0f)
        }
    }

    @Test
    fun nestedScrollInteropIsOn_offsetParent_updatesViewWindowOffsetSynchronously() {
        var offsetPx by mutableFloatStateOf(0f)
        var maxOffsetPx = 0f
        val preScrollDeltasY = mutableListOf<Float>()
        val offsettingConnection =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    preScrollDeltasY.add(available.y)
                    val previousOffset = offsetPx
                    offsetPx = (offsetPx + available.y).coerceIn(-maxOffsetPx, 0f)
                    return Offset(0f, offsetPx - previousOffset)
                }
            }
        rule.setContent {
            maxOffsetPx = with(LocalDensity.current) { CollapsingHeaderHeight.toPx() }
            Box(
                modifier =
                    Modifier.fillMaxSize().nestedScroll(offsettingConnection).offset {
                        IntOffset(0, offsetPx.fastRoundToInt())
                    }
            ) {
                NestedScrollInteropWithView(
                    recyclerViewConsumptionTracker = recyclerViewConsumptionTracker
                )
            }
        }

        onView(withId(R.id.main_list)).perform(swipeUp())

        rule.runOnIdle {
            assertThat(preScrollDeltasY).isNotEmpty()
            assertThat(preScrollDeltasY.all { it <= 0f }).isTrue()
            assertThat(offsetPx).isEqualTo(-maxOffsetPx)
        }
    }

    @Composable
    private fun DynamicTopPaddingBox(
        topPaddingPxProvider: () -> Int,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        Box(
            modifier.layout { measurable, constraints ->
                val topPaddingPx = topPaddingPxProvider().coerceAtLeast(0)
                val height = (constraints.maxHeight - topPaddingPx).coerceAtLeast(0)
                val placeable =
                    measurable.measure(constraints.copy(minHeight = 0, maxHeight = height))
                layout(placeable.width, constraints.maxHeight) {
                    placeable.place(0, topPaddingPx)
                }
            }
        ) {
            content()
        }
    }

    private companion object {
        val CollapsingHeaderHeight = 100.dp
    }
}
