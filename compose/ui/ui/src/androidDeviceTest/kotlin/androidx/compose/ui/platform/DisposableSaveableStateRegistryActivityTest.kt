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

package androidx.compose.ui.platform

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DisposableSaveableStateRegistryActivityTest {

    @get:Rule val activityScenarioRule = activityScenarioRule<ComponentActivity>()
    @get:Rule val composeTestRule = createEmptyComposeRule()

    private val activityScenario
        get() = activityScenarioRule.scenario

    @Test
    fun duplicateComposeViewIds_firstSavesAndRestores_secondResetsOnRecreate() {
        activityScenario.onActivity { activity ->
            activity.setContentView(createContentView(activity))
        }

        composeTestRule.onNodeWithText("Count: 0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expanded content").assertDoesNotExist()

        composeTestRule.onNodeWithText("Increment").performClick()
        composeTestRule.onNodeWithText("Expand").performClick()

        composeTestRule.onNodeWithText("Count: 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expanded content").assertIsDisplayed()

        activityScenario.recreate()
        activityScenario.onActivity { activity ->
            activity.setContentView(createContentView(activity))
        }

        composeTestRule.onNodeWithText("Count: 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expanded content").assertDoesNotExist()
    }
}

private fun createContentView(context: Context): View {
    return FrameLayout(context).apply {
        addView(
            ComposeView(context).apply {
                setContent {
                    var count by rememberSaveable { mutableIntStateOf(0) }
                    Column {
                        Text("Count: $count")
                        Button(onClick = { count++ }) { Text("Increment") }
                    }
                }
            }
        )
        addView(
            ComposeView(context).apply {
                setContent {
                    var expanded by rememberSaveable { mutableStateOf(false) }
                    Column {
                        Button(onClick = { expanded = !expanded }) { Text("Expand") }
                        if (expanded) {
                            Text("Expanded content")
                        }
                    }
                }
            }
        )
    }
}
