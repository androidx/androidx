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

package androidx.compose.ui.focus

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusRestorerConfigurationChangeTest {
    @get:Rule val rule = createAndroidComposeRule<OrientationChangeActivity>()

    @Test
    fun restoreFocus_activityRecreation() {
        // Arrange.
        rule.onNodeWithTag("item2").requestFocus()

        // Act.
        rule.activityRule.scenario.recreate()
        println("Recreate")

        // Assert.
        rule.onNodeWithTag("item2").assertIsFocused()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Before
    fun setup() {
        ComposeUiFlags.isInitialFocusOnFocusableAvailable = true
        ComposeUiFlags.isFocusRestorationEnabled = true
        InstrumentationRegistry.getInstrumentation().setInTouchModeCompat(false)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @After
    fun teardown() {
        ComposeUiFlags.isInitialFocusOnFocusableAvailable = false
        ComposeUiFlags.isFocusRestorationEnabled = false
        InstrumentationRegistry.getInstrumentation().resetInTouchModeCompat()
    }
}

// We can't use rule.setContent to setup this test because rule.setContent is not called again
// after orientation change.
class OrientationChangeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(Modifier.focusRestorer().focusGroup()) {
                Box(Modifier.testTag("item1").size(10.dp).focusable())
                Box(Modifier.testTag("item2").size(10.dp).focusable())
                Box(Modifier.testTag("item3").size(10.dp).focusable())
            }
        }
    }
}
