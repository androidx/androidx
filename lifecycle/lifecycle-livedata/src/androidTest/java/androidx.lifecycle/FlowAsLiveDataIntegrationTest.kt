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

package androidx.lifecycle

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@RunWith(AndroidJUnit4::class)
class FlowAsLiveDataIntegrationTest {

    @Before
    fun clearArchExecutors() {
        // make sure we don't receive a modified delegate. b/159212029
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    @Test
    @MediumTest
    fun startStopImmediately() {
        runBlocking {
            val stopChannelFlow = CompletableDeferred<Unit>()
            val (mediator, liveData) = withContext(Dispatchers.Main) {
                val mediator = MediatorLiveData<Int>()
                val liveData = channelFlow {
                    send(1)
                    // prevent block from ending
                    stopChannelFlow.await()
                }.asLiveData()
                mediator.addSource(liveData) {
                    mediator.removeSource(liveData)
                    mediator.value = -it
                }
                mediator to liveData
            }
            val read = mediator.asFlow().first()
            assertThat(read).isEqualTo(-1)
            assertThat(liveData.hasObservers()).isFalse()
            // make sure this test doesn't leak a running coroutine
            stopChannelFlow.complete(Unit)
        }
    }

    @UiThreadTest
    @Test
    @MediumTest
    fun asLiveData_preserveStateFlowInitialValue() {
        val value = "init"
        val result = MutableStateFlow(value)
            .asLiveData()
            .value
        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(value)
    }
}
