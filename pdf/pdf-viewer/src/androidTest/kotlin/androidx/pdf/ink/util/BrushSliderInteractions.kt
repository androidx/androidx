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

package androidx.pdf.ink.util

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import com.google.android.material.slider.Slider
import org.hamcrest.Matcher

/** Helper function to perform set slider value to a specific index. */
internal fun setSliderValue(value: Float): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return ViewMatchers.isAssignableFrom(Slider::class.java)
        }

        override fun getDescription(): String {
            return "Set slider value to $value"
        }

        override fun perform(uiController: UiController, view: View) {
            val slider = view as Slider
            // Ensure the value is within the slider's range to prevent crashes
            if (value >= slider.valueFrom && value <= slider.valueTo) {
                slider.value = value
            }
        }
    }
}

/** Helper function to assert slider index value. */
internal fun withSliderValue(expectedValue: Float): Matcher<View> {
    return object : org.hamcrest.TypeSafeMatcher<View>() {
        override fun describeTo(description: org.hamcrest.Description) {
            description.appendText("with slider value: $expectedValue")
        }

        override fun matchesSafely(view: View): Boolean {
            return view is Slider && view.value == expectedValue
        }
    }
}
