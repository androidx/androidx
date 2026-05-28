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

package androidx.navigation3.runtime.result

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.kruth.assertThat
import kotlin.test.Test
import org.junit.Rule

class ResultEventBusAndroidTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testConflateAsStateDefault() {
        val resultEventBus = ResultEventBus()
        val expectedResult = "testResult"

        var result: String? = null
        composeTestRule.setContent { result = resultEventBus.conflateAsState(expectedResult).value }

        composeTestRule.waitForIdle()

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun testConflateAsState() {
        val resultEventBus = ResultEventBus()
        val resultKey = "testKey"
        val expectedResult = "testResult"

        var result: String? = null
        composeTestRule.setContent {
            result = resultEventBus.conflateAsState(resultKey, "no result").value
        }

        resultEventBus.sendResult(resultKey, expectedResult)

        composeTestRule.waitForIdle()

        assertThat(result).isEqualTo(expectedResult)
    }
}
