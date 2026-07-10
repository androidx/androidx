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

import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.widget.addTextChangedListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RunOnUiThreadTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun updateAndroidView_assertComposeNode_onMainThread() {
        var composeTextState by mutableStateOf("Initial")
        lateinit var editText: EditText

        composeTestRule.runOnUiThread {
            val activity = composeTestRule.activity

            val rootLayout = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }

            editText =
                EditText(activity).apply {
                    addTextChangedListener { editable -> composeTextState = editable.toString() }
                }

            val composeView =
                ComposeView(activity).apply {
                    setContent { Text(text = "Mirror: $composeTextState") }
                }

            rootLayout.addView(editText)
            rootLayout.addView(composeView)
            activity.setContentView(rootLayout)
        }

        composeTestRule.waitForIdle()

        // Verify synchronous UI updates by simulating an Android View interaction
        // (user input) and immediately asserting the resulting Compose state,
        // entirely on the main thread.
        composeTestRule.runOnUiThread {
            editText.setText("Updated via View")

            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Mirror: Updated via View").assertIsDisplayed()
        }
    }
}
