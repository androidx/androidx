/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.paging.multicast

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharedFlowProducerTest {
    private val scope = TestCoroutineScope()
    private val upstreamMessages = mutableListOf<String>()
    private fun createProducer(flow: Flow<String>) = SharedFlowProducer<String>(
        scope = scope,
        src = flow,
        sendUpsteamMessage = {
            if (it is ChannelManager.Message.Dispatch.Value) {
                upstreamMessages.add(it.value)
                it.delivered.complete(Unit)
            }
        }
    )

    @Test
    fun closeFlowBeforeItStarts() {
        val producer = createProducer(flowOf("a", "b", "c"))
        scope.pauseDispatcher()
        producer.start()
        producer.cancel()
        assertThat(upstreamMessages).isEmpty()
    }

    @Test
    fun forwardValuesDownstreamWhenStarted() {
        val producer = createProducer(flowOf("a", "b", "c"))
        assertThat(upstreamMessages).isEmpty()
        producer.start()
        assertThat(upstreamMessages).containsExactly("a", "b", "c")
    }

    @Test
    fun startIsIdempotent() {
        val producer = createProducer(flowOf("a", "b", "c"))
        assertThat(upstreamMessages).isEmpty()
        producer.start()
        producer.start()
        producer.start()
        assertThat(upstreamMessages).containsExactly("a", "b", "c")
    }
}
