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

package androidx.compose.remote.creation.compose.action

import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemoteState
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.test.uiautomator.uiAutomator
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class HostActionTest {

    @get:Rule
    val remoteComposeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)
    }

    @Test
    fun hostActionWithConstantName() {
        val value = 1.ri

        runActionTest(value = value, expected = 1)
    }

    @Test
    fun hostActionWithVariableName() {
        val value = 1.ri
        val name = "a".rs + createNamedRemoteFloat("abc", 3f).toRemoteString(0, 0)

        runActionTest(value = value, expected = 1)
    }

    @Test
    fun hostActionWithStringValue() {
        val value = "1".rs

        runActionTest(value = value, expected = "1")
    }

    @Test
    fun hostActionWithIntValue() {
        val value = 1.ri

        runActionTest(value = value, expected = 1)
    }

    @Test
    fun hostActionWithFloatValue() {
        val value = 1f.rf

        runActionTest(value = value, expected = 1.0f)
    }

    @Test
    fun hostActionWithNoValue() {
        runActionTest(value = null, expected = null)
    }

    private fun runActionTest(value: RemoteState<*>?, expected: Any?) {

        remoteComposeTestRule.runTest {
            val valueString =
                when (value) {
                    is RemoteInt -> value.toRemoteString(2)
                    is RemoteFloat -> value.toRemoteString(2, 2)
                    is RemoteString -> value
                    else -> "null".rs
                }

            val mutableValue =
                when (value) {
                    is RemoteInt -> rememberMutableRemoteInt(value.constantValue)
                    is RemoteFloat -> value
                    is RemoteString -> rememberMutableRemoteString(value.constantValue)
                    else -> "null".rs
                }

            val action =
                when (mutableValue) {
                    is RemoteInt -> HostAction("a".rs, mutableValue)
                    is RemoteFloat -> HostAction("a".rs, mutableValue)
                    is RemoteString ->
                        if (value == null) HostAction("a".rs) else HostAction("a".rs, mutableValue)
                    else -> HostAction("a".rs)
                }

            RemoteColumn(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .semantics(mergeDescendants = true) {}
                        .clickable(action),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
                verticalArrangement = RemoteArrangement.Center,
            ) {
                RemoteText("Hello World")
                RemoteText("Value: ".rs + valueString)
            }
        }

        uiAutomator { onElement { isClickable }.click() }

        assertThat(remoteComposeTestRule.clickEvents).contains(Pair("a", expected))
    }
}
