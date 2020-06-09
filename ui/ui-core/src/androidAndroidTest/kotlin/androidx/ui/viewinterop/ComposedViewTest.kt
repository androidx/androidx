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

import android.util.TypedValue
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.filters.SmallTest
import androidx.ui.core.test.R
import androidx.ui.test.android.AndroidComposeTestRule
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.runOnUiThread
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
        val frameLayout = FrameLayout(composeTestRule.activityTestRule.activity).apply {
            layoutParams = ViewGroup.LayoutParams(300, 300)
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
                if (view.layoutParams.width !=
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        300f,
                        view.context.resources.displayMetrics
                    ).roundToInt()
                ) {
                    throw exception
                }
                if (view.layoutParams.height != WRAP_CONTENT) {
                    throw exception
                }
            }
    }

    @Test
    fun androidViewProperlyDetached() {
        val frameLayout = FrameLayout(composeTestRule.activityTestRule.activity).apply {
            layoutParams = ViewGroup.LayoutParams(300, 300)
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

        runOnIdleCompose {
            assertThat(frameLayout.parent).isNull()
        }
    }
}
