/*
 * Copyright 2023 The Android Open Source Project
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

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import androidx.activity.ComponentActivity
import androidx.annotation.LayoutRes
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.util.VelocityTrackerAddPointsFix
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.tests.R
import androidx.compose.ui.unit.Velocity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class VelocityTrackingParityTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val draggableView: VelocityTrackingView
        get() = rule.activity.findViewById(R.id.draggable_view)

    private val composeView: ComposeView
        get() = rule.activity.findViewById(R.id.compose_view)

    private var latestComposeVelocity = 0f

    @OptIn(ExperimentalComposeUiApi::class)
    @Before
    fun setUp() {
        latestComposeVelocity = 0f
        VelocityTrackerAddPointsFix = true
    }

    fun tearDown() {
        draggableView.tearDown()
    }

    @Test
    fun equalDraggable_withEqualSwipes_shouldProduceSimilarVelocity() {
        // Arrange
        createActivity()
        checkVisibility(composeView, View.VISIBLE)
        checkVisibility(draggableView, View.GONE)

        // Act
        swipeView(R.id.compose_view)

        val currentTopInCompose = latestComposeVelocity

        // switch visibility
        rule.runOnUiThread {
            composeView.visibility = View.GONE
            draggableView.visibility = View.VISIBLE
        }

        checkVisibility(composeView, View.GONE)
        checkVisibility(draggableView, View.VISIBLE)

        swipeView(R.id.draggable_view)
        val childAtTheTopOfView = draggableView.latestVelocity.y

        // assert
        assertThat(childAtTheTopOfView).isWithin(VelocityDifferenceTolerance)
            .of(currentTopInCompose)
    }

    private fun createActivity() {
        rule
            .activityRule
            .scenario
            .createActivityWithComposeContent(
                R.layout.velocity_tracker_compose_vs_view
            ) {
                TestComposeDraggable {
                    latestComposeVelocity = it
                }
            }
    }

    private fun checkVisibility(view: View, visibility: Int) = assertTrue {
        view.visibility == visibility
    }

    private fun swipeView(id: Int) {
        Espresso.onView(withId(id)).perform(swipeUp())
        rule.waitForIdle()
    }
}

@Composable
fun TestComposeDraggable(onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .draggable(
                rememberDraggableState(onDelta = { }),
                onDragStopped = onDragStopped,
                orientation = Orientation.Vertical
            )
    )
}

private fun ActivityScenario<*>.createActivityWithComposeContent(
    @LayoutRes layout: Int,
    content: @Composable () -> Unit,
) {
    onActivity { activity ->
        activity.setTheme(R.style.Theme_MaterialComponents_Light)
        activity.setContentView(layout)
        with(activity.findViewById<ComposeView>(R.id.compose_view)) {
            setContent(content)
        }

        activity.findViewById<VelocityTrackingView>(R.id.draggable_view)?.visibility =
            View.GONE
    }
    moveToState(Lifecycle.State.RESUMED)
}

/**
 * A view that adds data to a VelocityTracker.
 */
private class VelocityTrackingView(context: Context, attributeSet: AttributeSet) :
    View(context, attributeSet) {
    private val tracker = VelocityTracker.obtain()
    var latestVelocity: Velocity = Velocity.Zero
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_UP -> {
                tracker.computeCurrentVelocity(1000)
                latestVelocity = Velocity(tracker.xVelocity, tracker.yVelocity)
                tracker.clear()
            }

            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> tracker.addMovement(
                event
            )

            else -> {
                tracker.clear()
                latestVelocity = Velocity.Zero
            }
        }
        return true
    }

    fun tearDown() {
        tracker.recycle()
    }
}

private const val VelocityDifferenceTolerance = 10f