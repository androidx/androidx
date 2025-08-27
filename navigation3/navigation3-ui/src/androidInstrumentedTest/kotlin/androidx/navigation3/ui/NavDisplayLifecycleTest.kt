/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation3.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.kruth.assertThat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation3.runtime.NavEntry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavDisplayLifecycleTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun testNavigateForwardFiresLifecycleEventsInOrder() {
        val backStack = mutableStateListOf("A")

        val actualEvents = mutableListOf<Pair<String, String>>()
        rule.setContent {
            NavDisplay(backStack = backStack, onBack = { /* no-op */ }) { key ->
                NavEntry(key) {
                    LifecycleResumeEffect(key1 = Unit) {
                        actualEvents += key to "ON_RESUME"
                        onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                    }
                }
            }
        }
        rule.runOnIdle { backStack += "B" }
        rule.waitForIdle()

        assertThat(actualEvents)
            .containsExactly("A" to "ON_RESUME", "A" to "ON_PAUSE", "B" to "ON_RESUME")
    }

    @Test
    fun testNavigateBackFiresLifecycleEventsInOrder() {
        val backStack = mutableStateListOf("A", "B")

        val actualEvents = mutableListOf<Pair<String, String>>()
        rule.setContent {
            NavDisplay(backStack = backStack, onBack = { /* no-op */ }) { key ->
                NavEntry(key) {
                    LifecycleResumeEffect(key1 = Unit) {
                        actualEvents += key to "ON_RESUME"
                        onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                    }
                }
            }
        }
        rule.runOnIdle { backStack -= "B" }
        rule.waitForIdle()

        assertThat(actualEvents)
            .containsExactly("B" to "ON_RESUME", "B" to "ON_PAUSE", "A" to "ON_RESUME")
    }
}
