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

package androidx.compose.foundation.focus

import android.widget.EditText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LazyListFocusableInteractionTest {

    val testDispatcher = StandardTestDispatcher()
    @get:Rule val rule = createComposeRule(testDispatcher)

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun scrollingUpAndDownLazyColumnDoesntCrash() {
        rule.setContent {
            val focusRequester = remember { FocusRequester() }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().testTag("LazyColumn").padding(top = 16.dp)
                ) {
                    items(50) { index ->
                        BasicText("Item #$index", modifier = Modifier.padding(8.dp))
                    }

                    item {
                        AndroidView(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .testTag("EditText")
                                    .focusRequester(focusRequester),
                            factory = { context ->
                                EditText(context).apply { hint = "Focus me before I'm removed" }
                            },
                        )
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                    }

                    items(10) { index ->
                        BasicText("Item #${index + 200}", modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }

        rule.onNodeWithTag("LazyColumn").performTouchInput {
            repeat(20) {
                swipeWithVelocity(
                    start = this.center,
                    end = Offset(this.center.x, this.center.y - 200f),
                    durationMillis = 500,
                    endVelocity = 600f,
                )
            }
        }
        rule.onNodeWithTag("LazyColumn").performTouchInput {
            repeat(20) {
                swipeWithVelocity(
                    start = this.center,
                    end = Offset(this.center.x, this.center.y + 200f),
                    durationMillis = 500,
                    endVelocity = 600f,
                )
            }
        }
    }
}
