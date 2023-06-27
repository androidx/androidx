/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class GlobalAssertionsTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Before fun setUp() {}

  @Test
  fun performClick_withGlobalAssertion_triggersGlobalAssertion() {
    composeTestRule.setContent { CountingButton() }
    var capturedSni: SemanticsNodeInteraction? = null

    addGlobalAssertion(/* name= */ "Fred") { sni -> capturedSni = sni }
    composeTestRule.onNodeWithText("Increment counter").performClick()

    composeTestRule.onNodeWithText("Clicks: 1").assertExists()
    capturedSni!!.assertTextContains("Increment counter")
  }

  @Test
  fun performClick_withGlobalAssertionRemoved_doesNotTriggersGlobalAssertion() {
    composeTestRule.setContent { CountingButton() }
    var capturedSni: SemanticsNodeInteraction? = null

    addGlobalAssertion(/* name= */ "Fred") { sni -> capturedSni = sni }
    removeGlobalAssertion(/* name= */ "Fred")
    composeTestRule.onNodeWithText("Increment counter").performClick()

    composeTestRule.onNodeWithText("Clicks: 1").assertExists()
    assertThat(capturedSni).isNull()
  }
}

@Composable
internal fun CountingButton() {
  var counter by remember { mutableStateOf(0) }
  Column {
    Button(onClick = { counter++ }) { Text("Increment counter") }
    Text(text = "Clicks: $counter")
  }
}
