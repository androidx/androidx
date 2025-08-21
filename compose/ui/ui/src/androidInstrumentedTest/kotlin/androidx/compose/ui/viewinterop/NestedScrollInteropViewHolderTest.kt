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
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.tests.R
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
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

    @Test
    fun nestedScrollInteropIsOff_shouldNotPropagateDeltas() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                enabled = false,
                recyclerViewConsumptionTracker = recyclerViewConsumptionTracker,
            )
        }

        // act
        onView(withId(R.id.main_list))
            .perform(scrollToPosition<NestedScrollInteropAdapter.SimpleTextViewHolder>(20))

        // assert
        rule.runOnIdle { assertThat(connection.offeredFromChild).isEqualTo(Offset.Zero) }
    }

    @Test
    fun nestedScrollInteropIsOn_shouldPropagateDeltas() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                enabled = true,
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
                enabled = true,
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
                enabled = true,
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
                    enabled = true,
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
                enabled = true,
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
                enabled = true,
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
}
