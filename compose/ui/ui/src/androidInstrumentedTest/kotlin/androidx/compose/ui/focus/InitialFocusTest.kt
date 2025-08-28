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

import android.os.Build.VERSION.SDK_INT
import android.view.View
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
class InitialFocusTest(private val initialFocusEnabled: Boolean, private val touchMode: Boolean) {
    @get:Rule val rule = createComposeRule()

    lateinit var owner: View
    lateinit var layoutDirection: LayoutDirection
    var previousFlagValue: Boolean = false

    @Before
    fun setTouchMode() {
        @OptIn(ExperimentalComposeUiApi::class)
        previousFlagValue = ComposeUiFlags.isInitialFocusOnFocusableAvailable
        @OptIn(ExperimentalComposeUiApi::class)
        ComposeUiFlags.isInitialFocusOnFocusableAvailable = initialFocusEnabled
        InstrumentationRegistry.getInstrumentation().setInTouchModeCompat(touchMode)
    }

    @After
    fun resetTouchMode() {
        InstrumentationRegistry.getInstrumentation().resetInTouchModeCompat()
        @OptIn(ExperimentalComposeUiApi::class)
        ComposeUiFlags.isInitialFocusOnFocusableAvailable = previousFlagValue
    }

    @Test
    fun noFocusable() {
        // Arrange.
        rule.setTestContent { Box(Modifier.size(100.dp)) }

        // Assert.
        rule.runOnIdle { assertThat(owner.hasFocus()).isFalse() }
    }

    @Test
    fun singleFocusedItem() {
        // Arrange.
        rule.setTestContent { Box(Modifier.size(100.dp).testTag("box").focusable()) }

        // Assert.
        // Since API 28, we don't assign initial focus in touch mode.
        // https://developer.android.com/about/versions/pie/android-9.0-changes-28#focus
        if (!initialFocusEnabled || touchMode && SDK_INT >= 28) {
            assertThat(owner.hasFocus()).isFalse()
        } else {
            rule.onNodeWithTag("box").assertIsFocused()
        }
    }

    @Test
    fun topItemIsInitiallyFocused() {
        // Arrange.
        rule.setTestContent {
            Column {
                Box(Modifier.size(100.dp).testTag("top").focusable())
                Box(Modifier.size(100.dp).focusable())
            }
        }

        // Assert.
        // Since API 28, we don't assign initial focus in touch mode.
        // https://developer.android.com/about/versions/pie/android-9.0-changes-28#focus
        if (!initialFocusEnabled || touchMode && SDK_INT >= 28) {
            assertThat(owner.hasFocus()).isFalse()
        } else {
            rule.onNodeWithTag("top").assertIsFocused()
        }
    }

    @Test
    fun leftItemIsInitiallyFocused() {
        // Arrange.
        rule.setTestContent {
            Row {
                Box(Modifier.size(100.dp).testTag("left").focusable())
                Box(Modifier.size(100.dp).testTag("right").focusable())
            }
        }

        // Assert.
        // Since API 28, we don't assign initial focus in touch mode.
        // https://developer.android.com/about/versions/pie/android-9.0-changes-28#focus
        if (!initialFocusEnabled || touchMode && SDK_INT >= 28) {
            assertThat(owner.hasFocus()).isFalse()
        } else {
            when (layoutDirection) {
                LayoutDirection.Ltr -> rule.onNodeWithTag("left").assertIsFocused()
                LayoutDirection.Rtl -> rule.onNodeWithTag("right").assertIsFocused()
            }
        }
    }

    @Test
    fun itemFocusedOnAppearing() {
        // Arrange.
        var showItem by mutableStateOf(false)
        rule.setTestContent {
            if (showItem) {
                Box(Modifier.size(100.dp).testTag("box").focusable())
            }
        }

        // Act.
        rule.runOnIdle { showItem = true }

        // Assert.
        // Since API 28, we don't assign initial focus in touch mode.
        // https://developer.android.com/about/versions/pie/android-9.0-changes-28#focus
        if (!initialFocusEnabled || touchMode && SDK_INT >= 28) {
            assertThat(owner.hasFocus()).isFalse()
        } else {
            rule.onNodeWithTag("box").assertIsFocused()
        }
    }

    @Test
    fun topItemFocusedOnAppearing() {
        // Arrange.
        var showItem by mutableStateOf(false)
        rule.setTestContent {
            if (showItem) {
                Column {
                    Box(Modifier.size(100.dp).testTag("top").focusable())
                    Box(Modifier.size(100.dp).focusable())
                }
            }
        }

        // Act.
        rule.runOnIdle { showItem = true }

        // Assert.
        // Since API 28, we don't assign initial focus in touch mode.
        // https://developer.android.com/about/versions/pie/android-9.0-changes-28#focus
        if (!initialFocusEnabled || touchMode && SDK_INT >= 28) {
            assertThat(owner.hasFocus()).isFalse()
        } else {
            rule.onNodeWithTag("top").assertIsFocused()
        }
    }

    @Test
    fun leftItemFocusedOnAppearing() {
        // Arrange.
        var showItem by mutableStateOf(false)
        rule.setTestContent {
            if (showItem) {
                Row {
                    Box(Modifier.size(100.dp).testTag("left").focusable())
                    Box(Modifier.size(100.dp).testTag("right").focusable())
                }
            }
        }

        // Act.
        rule.runOnIdle { showItem = true }

        // Assert.
        // Since API 28, we don't assign initial focus in touch mode.
        // https://developer.android.com/about/versions/pie/android-9.0-changes-28#focus
        if (!initialFocusEnabled || touchMode && SDK_INT >= 28) {
            assertThat(owner.hasFocus()).isFalse()
        } else {
            when (layoutDirection) {
                LayoutDirection.Ltr -> rule.onNodeWithTag("left").assertIsFocused()
                LayoutDirection.Rtl -> rule.onNodeWithTag("right").assertIsFocused()
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "initialFocusEnabled = {0}, touchMode = {1}")
        fun initParameters() =
            listOf(
                arrayOf(false, false),
                arrayOf(false, true),
                arrayOf(true, false),
                arrayOf(true, true),
            )
    }

    private fun ComposeContentTestRule.setTestContent(composable: @Composable () -> Unit) {
        setContent {
            owner = LocalView.current
            layoutDirection = LocalLayoutDirection.current
            composable()
        }
    }
}
