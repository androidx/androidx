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

import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.composer
import androidx.compose.Model
import androidx.compose.compose
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.baseui.selection.ToggleableState
import androidx.ui.core.CraneWrapper
import androidx.ui.core.TestTag
import androidx.ui.material.MaterialTheme
import androidx.ui.material.TriStateCheckbox
import androidx.ui.material.surface.Surface
import androidx.ui.test.android.DefaultTestActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// TODO: Make this inner class once @Model gets fixed.
@Model
class CheckboxState(var value: ToggleableState = ToggleableState.Unchecked) {
    fun toggle() {
        value =
            if (value == ToggleableState.Checked) {
                ToggleableState.Unchecked
            } else {
                ToggleableState.Checked
            }
    }
}

/**
 * These are tests but also demonstration of our capability to test Compose as part of legacy
 * Android hierarchy. This also includes showcase of multiple Compose roots.
 */
@MediumTest
@RunWith(JUnit4::class)
class MultipleComposeRootsTest {

    @get:Rule
    val activityTestRule = ActivityTestRule<DefaultTestActivity>(
        DefaultTestActivity::class.java
    )

    @get:Rule
    val disableTransitions = DisableTransitions()

    /**
     * In this setup we have the following configuration:
     *
     * Title 1            < Android TextView
     * [ ] Checkbox 1     < Compose root
     * Title 2            < Android TextView
     * [ ] Checkbox 2     < Compose root
     *
     * Both checkboxes and titles share the same data model. However the titles are regular Android
     * Text Views updated imperatively via listeners on the checkboxes. This test seamlessly
     * modifies and asserts state of the checkboxes and titles using a mix of Espresso and Compose
     * testing APIs.
     */
    @Test
    fun twoHierarchiesSharingTheSameModel() {
        val activity = activityTestRule.activity

        activity.runOnUiThread(object : Runnable { // Workaround for lambda bug in IR
            override fun run() {
                val state1 = CheckboxState(value = ToggleableState.Unchecked)
                val state2 = CheckboxState(value = ToggleableState.Checked)

                val linearLayout = LinearLayout(activity)
                    .apply { orientation = LinearLayout.VERTICAL }

                val textView1 = TextView(activity).apply { text = "Compose 1" }
                val frameLayout1 = FrameLayout(activity)

                val textView2 = TextView(activity).apply { text = "Compose 2" }
                val frameLayout2 = FrameLayout(activity)

                activity.setContentView(linearLayout)
                linearLayout.addView(textView1)
                linearLayout.addView(frameLayout1)
                linearLayout.addView(textView2)
                linearLayout.addView(frameLayout2)

                fun updateTitle1() {
                    textView1.text = "Compose 1 - ${state1.value}"
                }

                fun updateTitle2() {
                    textView2.text = "Compose 2 - ${state2.value}"
                }

                frameLayout1.compose {
                    CraneWrapper {
                        MaterialTheme {
                            Surface {
                                TestTag(tag = "checkbox1") {
                                    TriStateCheckbox(
                                        value = state1.value,
                                        onClick = {
                                            state1.toggle()
                                            state2.toggle()
                                            updateTitle1()
                                            updateTitle2()
                                        })
                                }
                            }
                        }
                    }
                }

                frameLayout2.compose {
                    CraneWrapper {
                        MaterialTheme {
                            Surface {
                                TestTag(tag = "checkbox2") {
                                    TriStateCheckbox(
                                        value = state2.value,
                                        onClick = {
                                            state1.toggle()
                                            state2.toggle()
                                            updateTitle1()
                                            updateTitle2()
                                        })
                                }
                            }
                        }
                    }
                }
            }
        })

        Espresso.onView(withText("Compose 1")).check(matches(isDisplayed()))
        Espresso.onView(withText("Compose 2")).check(matches(isDisplayed()))

        findByTag("checkbox1")
            .doClick()
            .assertIsChecked()

        findByTag("checkbox2")
            .assertIsNotChecked()

        Espresso.onView(withText("Compose 1 - Checked")).check(matches(isDisplayed()))
        Espresso.onView(withText("Compose 2 - Unchecked")).check(matches(isDisplayed()))

        findByTag("checkbox2")
            .doClick()
            .assertIsNotChecked()

        findByTag("checkbox1")
            .assertIsChecked()

        Espresso.onView(withText("Compose 1 - Checked")).check(matches(isDisplayed()))
        Espresso.onView(withText("Compose 2 - Unchecked")).check(matches(isDisplayed()))
    }
}
