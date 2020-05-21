/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.Composable
import androidx.compose.Recomposer
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.filters.MediumTest
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.foundation.Box
import androidx.ui.foundation.VerticalScroller
import androidx.ui.graphics.Color
import androidx.ui.layout.Row
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxHeight
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.height
import androidx.ui.layout.size
import androidx.ui.layout.width
import androidx.ui.test.android.AndroidComposeTestRule
import androidx.ui.test.util.BoundaryNode
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class IsDisplayedTest {

    @get:Rule
    val composeTestRule = AndroidComposeTestRule<ComponentActivity>(disableTransitions = true)

    private val colors = listOf(Color.Red, Color.Green, Color.Blue)

    @Composable
    private fun Item(i: Int, width: Dp? = null, height: Dp? = null) {
        BoundaryNode("item$i") {
            Box(
                modifier = with(Modifier) { width?.let { width(it) } ?: fillMaxWidth() } +
                        with(Modifier) { height?.let { height(it) } ?: fillMaxHeight() },
                backgroundColor = colors[i % colors.size]
            )
        }
    }

    @Composable
    fun PlaceConditionally(place: Boolean, child: @Composable () -> Unit) {
        Layout(children = child) { measurables, constraints, _ ->
            if (place) {
                val placeable = measurables[0].measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(0.ipx, 0.ipx)
                }
            } else {
                layout(0.ipx, 0.ipx) {}
            }
        }
    }

    @Test
    fun componentInScrollable_isDisplayed() {
        composeTestRule.setContent {
            VerticalScroller(modifier = Modifier.size(100.dp)) {
                repeat(10) { Item(it, height = 30.dp) }
            }
        }

        findByTag("item0")
            .assertIsDisplayed()
    }

    @Test
    fun componentInScrollable_isNotDisplayed() {
        composeTestRule.setContent {
            VerticalScroller(modifier = Modifier.size(100.dp)) {
                repeat(10) { Item(it, height = 30.dp) }
            }
        }

        findByTag("item4")
            .assertIsNotDisplayed()
    }

    @Test
    fun togglePlacement() {
        var place by mutableStateOf(true)

        composeTestRule.setContent {
            PlaceConditionally(place) {
                // Item instead of BoundaryNode because we need non-zero size
                Item(0)
            }
        }

        findByTag("item0")
            .assertIsDisplayed()

        runOnIdleCompose {
            place = false
        }

        findByTag("item0")
            .assertIsNotDisplayed()
    }

    @Test
    fun toggleParentPlacement() {
        var place by mutableStateOf(true)

        composeTestRule.setContent {
            PlaceConditionally(place) {
                Stack {
                    // Item instead of BoundaryNode because we need non-zero size
                    Item(0)
                }
            }
        }

        findByTag("item0")
            .assertIsDisplayed()

        runOnIdleCompose {
            place = false
        }

        findByTag("item0")
            .assertIsNotDisplayed()
    }

    @Test
    fun rowTooSmall() {
        composeTestRule.setContent {
            Row(modifier = Modifier.size(100.dp)) {
                repeat(10) { Item(it, width = 30.dp) }
            }
        }

        findByTag("item9")
            .assertIsNotDisplayed()
    }

    @Test
    fun viewVisibility_androidComposeView() {
        val activity = composeTestRule.activityTestRule.activity
        val androidComposeView = runOnUiThread {
            // FrameLayout(id=100, w=100, h=100)
            // '- AndroidComposeView
            FrameLayout(activity).apply {
                id = 100
                layoutParams = ViewGroup.MarginLayoutParams(100, 100)
                activity.setContentView(this)
                setContent(Recomposer.current()) {
                    Item(0)
                }
            }.getChildAt(0)
        }

        fun onComposeView(): ViewInteraction {
            return onView(allOf(withParent(withId(100))))
        }

        onComposeView().check(matches(isDisplayed()))
        findByTag("item0").assertIsDisplayed()

        runOnIdleCompose {
            androidComposeView.visibility = View.GONE
        }

        onComposeView().check(matches(not(isDisplayed())))
        findByTag("item0").assertIsNotDisplayed()
    }

    @Test
    fun viewVisibility_parentView() {
        val activity = composeTestRule.activityTestRule.activity
        val composeContainer = runOnUiThread {
            // FrameLayout
            // '- FrameLayout(id=100, w=100, h=100) -> composeContainer
            //    '- AndroidComposeView
            FrameLayout(activity).apply {
                id = 100
                layoutParams = ViewGroup.MarginLayoutParams(100, 100)
                activity.setContentView(FrameLayout(activity).also { it.addView(this) })
                setContent(Recomposer.current()) {
                    Item(0)
                }
            }
        }

        fun onComposeView(): ViewInteraction {
            return onView(allOf(withParent(withId(100))))
        }

        onComposeView().check(matches(isDisplayed()))
        findByTag("item0").assertIsDisplayed()

        runOnIdleCompose {
            composeContainer.visibility = View.GONE
        }

        onComposeView().check(matches(not(isDisplayed())))
        findByTag("item0").assertIsNotDisplayed()
    }
}
