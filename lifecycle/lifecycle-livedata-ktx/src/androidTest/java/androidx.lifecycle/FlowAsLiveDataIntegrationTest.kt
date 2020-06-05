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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@RunWith(AndroidJUnit4::class)
class FlowAsLiveDataIntegrationTest {
    @Test
    @SmallTest
    fun startStopImmediately() {
        runBlocking {
            val mediator = withContext(Dispatchers.Main) {
                val mediator = MediatorLiveData<Int>()
                val liveData = channelFlow {
                    send(1)
                    delay(30000) // prevent block from ending
                }.asLiveData()
                mediator.addSource(liveData) {
                    mediator.removeSource(liveData)
                    mediator.value = -it
                }
                mediator
            }
            val read = mediator.asFlow().first()
            Truth.assertThat(read).isEqualTo(-1)
        }
    }
}