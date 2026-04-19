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

import androidx.kruth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ResultEventBusTest {
    @Test
    fun testGetResultFlow() =
        runTest(UnconfinedTestDispatcher()) {
            val resultEventBus = ResultEventBus()
            val resultKey = "testKey"
            val expectedResult = "testResult"

            val flow = resultEventBus.getResultFlow(resultKey)
            resultEventBus.sendResult(resultKey, expectedResult)

            var result = "No result set"

            backgroundScope.launch { flow?.collect { result = it as String } }

            assertThat(result).isEqualTo(expectedResult)
        }

    @Test
    fun testRemoveResult() {
        val resultEventBus = ResultEventBus()
        val resultKey = "testKey"

        resultEventBus.sendResult(resultKey, "test")
        assertThat(resultEventBus.channelMap.containsKey(resultKey)).isTrue()

        resultEventBus.removeResult(resultKey)
        assertThat(resultEventBus.channelMap.containsKey(resultKey)).isFalse()
    }
}
