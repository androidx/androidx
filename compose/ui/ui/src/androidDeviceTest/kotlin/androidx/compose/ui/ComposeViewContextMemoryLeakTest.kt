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

package androidx.compose.ui

import android.R
import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ComposeViewContext
import androidx.compose.ui.platform.ExperimentalComposeViewContextApi
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@OptIn(ExperimentalComposeViewContextApi::class)
@RunWith(AndroidJUnit4::class)
class ComposeViewContextMemoryLeakTest {

    @get:Rule val activityScenarioRule = ActivityScenarioRule(ComponentActivity::class.java)

    @Test
    fun composeViewContext_assertNoLeak() {
        lateinit var activity: Activity
        activityScenarioRule.scenario.onActivity { activity = it }
        runBlocking(AndroidUiDispatcher.Main) {
            val emptyView = View(activity)
            activity.setContentView(emptyView)
            MemoryLeakTest.loopAndVerifyMemory(
                iterations = 400,
                gcFrequency = 40,
                ignoreFirstRun = true,
            ) {
                val composeViewContext = ComposeViewContext(activity.findViewById(R.id.content))
                val composeView = ComposeView(activity)
                composeView.setContent { Column { repeat(3) { Box { BasicText("Hello") } } } }
                composeView.createComposition(composeViewContext)
                activity.setContentView(composeView)

                // After composing, we clear it.
                activity.setContentView(emptyView)
            }
        }
    }

    @Test
    fun composeViewContextRemovedView_assertNoLeak() {
        lateinit var activity: Activity
        lateinit var parentView: FrameLayout
        activityScenarioRule.scenario.onActivity {
            activity = it
            parentView = FrameLayout(activity)
            activity.setContentView(parentView)
        }
        runBlocking(AndroidUiDispatcher.Main) {
            MemoryLeakTest.loopAndVerifyMemory(
                iterations = 400,
                gcFrequency = 40,
                ignoreFirstRun = true,
            ) {
                val composeView = ComposeView(activity)
                parentView.addView(composeView)
                composeView.setContent { Column { repeat(3) { Box { BasicText("Hello") } } } }
                parentView.removeAllViews()
            }
        }
    }

    @Test
    fun composeViewContext_multipleViews_noLeak() {
        lateinit var activity: Activity
        lateinit var parentView: FrameLayout
        activityScenarioRule.scenario.onActivity {
            activity = it
            parentView = FrameLayout(activity)
            activity.setContentView(parentView)
        }
        runBlocking(AndroidUiDispatcher.Main) {
            MemoryLeakTest.loopAndVerifyMemory(
                iterations = 400,
                gcFrequency = 40,
                ignoreFirstRun = true,
            ) {
                repeat(2) {
                    val composeView = ComposeView(activity)
                    parentView.addView(composeView)
                    composeView.setContent { Column { repeat(3) { Box { BasicText("Hello") } } } }
                }
                parentView.removeAllViews()
            }
        }
    }
}
