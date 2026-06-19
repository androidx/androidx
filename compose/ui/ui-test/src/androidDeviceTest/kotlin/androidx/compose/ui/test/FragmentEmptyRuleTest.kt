/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FragmentEmptyRuleTest {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    @Test
    fun interactWithFragment_usingEmptyRule_andMainThreadSync() {
        val scenario = launchFragmentInContainer<CounterFragment>()

        composeTestRule.waitForIdle()

        scenario.onFragment { fragment ->
            composeTestRule.waitForIdle()
            fragment.button.performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Compose Clicks: 1").assertIsDisplayed()
        }
    }

    class CounterFragment : Fragment() {
        var clickCount by mutableIntStateOf(0)
        lateinit var button: Button

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View {
            val context = requireContext()

            return LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL

                button =
                    Button(context).apply {
                        text = "Add Click"
                        setOnClickListener { clickCount++ }
                    }

                val composeView =
                    ComposeView(context).apply {
                        setContent { Text("Compose Clicks: $clickCount") }
                    }

                addView(button)
                addView(composeView)
            }
        }
    }
}
