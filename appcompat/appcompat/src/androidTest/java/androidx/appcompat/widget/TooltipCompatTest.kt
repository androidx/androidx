/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appcompat.widget

import android.app.Activity
import android.widget.Button
import androidx.appcompat.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(maxSdkVersion = 27)
@RunWith(AndroidJUnit4::class)
@MediumTest
class TooltipCompatTest {

    /**
     * Regression test for b/162983241 where attempting to show a tooltip while it's already shown
     * causes a surfaceflinger crash on physical devices and emulators to become unresponsive.
     */
    @Test
    fun testDoubleShow() {
        ActivityScenario.launchActivityForResult(
            TooltipCompatTestActivity::class.java
        ).use { scenario ->
            scenario.onActivity { activity ->
                val button = activity.findViewById<Button>(R.id.button)
                button.setOnClickListener {
                    activity.setResult(Activity.RESULT_OK)
                    activity.finish()
                }
                TooltipCompat.setTooltipText(button, "Test")
            }
            onView(withId(R.id.button)).perform(longClick())
            onView(withId(R.id.button)).perform(longClick())
            onView(withId(R.id.button)).perform(click())
            assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        }
    }
}
