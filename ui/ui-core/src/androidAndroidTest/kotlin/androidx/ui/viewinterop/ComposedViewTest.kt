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

package androidx.ui.viewinterop

import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.compose.Recomposer
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.core.test.R
import androidx.ui.core.testTag
import androidx.compose.foundation.Box
import androidx.compose.foundation.background
import androidx.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.ui.test.android.AndroidComposeTestRule
import androidx.ui.test.assertPixels
import androidx.ui.test.captureToBitmap
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.runOnIdle
import androidx.ui.test.runOnUiThread
import androidx.ui.unit.Dp
import androidx.ui.unit.IntSize
import androidx.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.endsWith
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.roundToInt

@SmallTest
@RunWith(JUnit4::class)
class ComposedViewTest {
    @get:Rule
    val composeTestRule = AndroidComposeTestRule<ComponentActivity>()

    @Test
    fun androidViewWithResourceTest() {
        composeTestRule.setContent {
            AndroidView(R.layout.test_layout)
        }
        Espresso
            .onView(instanceOf(RelativeLayout::class.java))
            .check(matches(isDisplayed()))
    }

    @Test
    fun androidViewWithViewTest() {
        lateinit var frameLayout: FrameLayout
        composeTestRule.activityRule.scenario.onActivity { activity ->
            frameLayout = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(300, 300)
            }
        }
        composeTestRule.setContent {
            AndroidView(frameLayout)
        }
        Espresso
            .onView(equalTo(frameLayout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun androidViewWithResourceTest_preservesLayoutParams() {
        composeTestRule.setContent {
            AndroidView(R.layout.test_layout)
        }
        Espresso
            .onView(withClassName(endsWith("RelativeLayout")))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                if (view.layoutParams.width != 300.dp.toPx(view.context.resources.displayMetrics)) {
                    throw exception
                }
                if (view.layoutParams.height != WRAP_CONTENT) {
                    throw exception
                }
            }
    }

    @Test
    fun androidViewProperlyDetached() {
        lateinit var frameLayout: FrameLayout
        composeTestRule.activityRule.scenario.onActivity { activity ->
            frameLayout = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(300, 300)
            }
        }
        var emit by mutableStateOf(true)
        composeTestRule.setContent {
            if (emit) {
                AndroidView(frameLayout)
            }
        }

        runOnUiThread {
            assertThat(frameLayout.parent).isNotNull()
            emit = false
        }

        runOnIdle {
            assertThat(frameLayout.parent).isNull()
        }
    }

    @Test
    fun androidViewWithResource_modifierIsApplied() {
        val size = 20.dp
        composeTestRule.setContent {
            AndroidView(R.layout.test_layout, Modifier.size(size))
        }
        Espresso
            .onView(instanceOf(RelativeLayout::class.java))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                val expectedSize = size.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
    }

    @Test
    fun androidViewWithView_modifierIsApplied() {
        val size = 20.dp
        lateinit var frameLayout: FrameLayout
        composeTestRule.activityRule.scenario.onActivity { activity ->
            frameLayout = FrameLayout(activity)
        }
        composeTestRule.setContent {
            AndroidView(frameLayout, Modifier.size(size))
        }

        Espresso
            .onView(equalTo(frameLayout))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                val expectedSize = size.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun androidViewWithView_drawModifierIsApplied() {
        val size = 300
        lateinit var frameLayout: FrameLayout
        composeTestRule.activityRule.scenario.onActivity { activity ->
            frameLayout = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(size, size)
            }
        }
        composeTestRule.setContent {
            AndroidView(frameLayout, Modifier.testTag("view").background(color = Color.Blue))
        }

        onNodeWithTag("view").captureToBitmap().assertPixels(IntSize(size, size)) {
            Color.Blue
        }
    }

    @Test
    fun androidViewWithResource_modifierIsCorrectlyChanged() {
        val size = mutableStateOf(20.dp)
        composeTestRule.setContent {
            AndroidView(R.layout.test_layout, Modifier.size(size.value))
        }
        Espresso
            .onView(instanceOf(RelativeLayout::class.java))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                val expectedSize = size.value.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
        runOnIdle { size.value = 30.dp }
        Espresso
            .onView(instanceOf(RelativeLayout::class.java))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                val expectedSize = size.value.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
    }

    @Test
    fun androidView_notDetachedFromWindowTwice() {
        // Should not crash.
        composeTestRule.setContent {
            Box {
                emitView(::FrameLayout) {
                    it.setContent(Recomposer()) {
                        Box()
                    }
                }
            }
        }
    }

    private fun Dp.toPx(displayMetrics: DisplayMetrics) =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            displayMetrics
        ).roundToInt()
}
