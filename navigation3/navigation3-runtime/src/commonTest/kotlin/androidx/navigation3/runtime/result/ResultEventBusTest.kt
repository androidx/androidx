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

    @Test
    fun testRemoveAllResults() {
        val resultEventBus = ResultEventBus()

        resultEventBus.sendResult("testKey1", "test1")
        resultEventBus.sendResult("testKey2", "test2")
        resultEventBus.sendResult(42)
        assertThat(resultEventBus.channelMap).hasSize(3)

        resultEventBus.removeAllResults()
        assertThat(resultEventBus.channelMap).isEmpty()
    }

    @Test
    fun testRemoveAllResultsClosesChannels() =
        runTest(UnconfinedTestDispatcher()) {
            val resultEventBus = ResultEventBus()
            val key1 = "activeKey1"
            val key2 = "activeKey2"

            val flow1 = resultEventBus.getResultFlow(key1)
            val flow2 = resultEventBus.getResultFlow(key2)
            val receivedResults1 = mutableListOf<Any?>()
            val receivedResults2 = mutableListOf<Any?>()
            var flow1Completed = false
            var flow2Completed = false

            backgroundScope.launch {
                flow1.collect { receivedResults1.add(it) }
                flow1Completed = true
            }
            backgroundScope.launch {
                flow2.collect { receivedResults2.add(it) }
                flow2Completed = true
            }

            resultEventBus.sendResult(key1, "first1")
            resultEventBus.sendResult(key2, "first2")

            assertThat(receivedResults1).containsExactly("first1")
            assertThat(receivedResults2).containsExactly("first2")
            assertThat(flow1Completed).isFalse()
            assertThat(flow2Completed).isFalse()

            // this should close all channels, allowing the collects to complete
            resultEventBus.removeAllResults()

            assertThat(flow1Completed).isTrue()
            assertThat(flow2Completed).isTrue()
            assertThat(resultEventBus.channelMap).isEmpty()

            // Closed flows should not receive subsequent results
            resultEventBus.sendResult(key1, "second1")
            resultEventBus.sendResult(key2, "second2")
            assertThat(receivedResults1).containsExactly("first1")
            assertThat(receivedResults2).containsExactly("first2")
        }

    @Test
    fun testRemoveAllResultsDiscardsBufferedResults() =
        runTest(UnconfinedTestDispatcher()) {
            val resultEventBus = ResultEventBus()
            val bufferedKey1 = "bufferedKey1"
            val bufferedKey2 = "bufferedKey2"

            resultEventBus.sendResult(bufferedKey1, "stale1")
            resultEventBus.sendResult(bufferedKey2, "stale2")
            assertThat(resultEventBus.channelMap).hasSize(2)

            resultEventBus.removeAllResults()
            assertThat(resultEventBus.channelMap).isEmpty()

            // Verify stale buffered results were discarded and bus can still be reused
            val postRemoveResults1 = mutableListOf<Any?>()
            val postRemoveResults2 = mutableListOf<Any?>()
            backgroundScope.launch {
                resultEventBus.getResultFlow(bufferedKey1).collect { postRemoveResults1.add(it) }
            }
            backgroundScope.launch {
                resultEventBus.getResultFlow(bufferedKey2).collect { postRemoveResults2.add(it) }
            }
            assertThat(postRemoveResults1).isEmpty()
            assertThat(postRemoveResults2).isEmpty()

            resultEventBus.sendResult(bufferedKey1, "fresh1")
            resultEventBus.sendResult(bufferedKey2, "fresh2")
            assertThat(postRemoveResults1).containsExactly("fresh1")
            assertThat(postRemoveResults2).containsExactly("fresh2")
        }
}
