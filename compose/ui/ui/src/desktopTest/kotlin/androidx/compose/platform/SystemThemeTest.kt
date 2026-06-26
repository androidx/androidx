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

package androidx.compose.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ProvideSystemTheme
import androidx.compose.ui.pollSystemTheme
import androidx.compose.ui.systemThemePollingJob
import androidx.compose.ui.systemThemeSubscriberCount
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SystemThemeTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testSystemThemePollingState() {
        val prevValue = ComposeUiFlags.pollSystemTheme
        ComposeUiFlags.pollSystemTheme = true
        try {
            runComposeUiTest {
                var provideSystemTheme1 by mutableStateOf(false)
                var provideSystemTheme2 by mutableStateOf(false)
                setContent {
                    if (provideSystemTheme1) {
                        ProvideSystemTheme { }
                    }
                    if (provideSystemTheme2) {
                        ProvideSystemTheme { }
                    }
                }

                assertEquals(0, systemThemeSubscriberCount())
                assertNull(systemThemePollingJob())

                provideSystemTheme1 = true
                waitForIdle()
                assertEquals(1, systemThemeSubscriberCount())
                assertNotNull(systemThemePollingJob())

                provideSystemTheme2 = true
                waitForIdle()
                assertEquals(2, systemThemeSubscriberCount())
                assertNotNull(systemThemePollingJob())

                provideSystemTheme1 = false
                provideSystemTheme2 = false
                waitForIdle()
                assertEquals(0, systemThemeSubscriberCount())
                assertNull(systemThemePollingJob())
            }
        } finally {
            ComposeUiFlags.pollSystemTheme = prevValue
        }
    }
}