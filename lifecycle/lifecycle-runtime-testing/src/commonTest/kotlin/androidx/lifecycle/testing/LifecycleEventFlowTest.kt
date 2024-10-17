/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.lifecycle.testing

import androidx.kruth.assertThat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.eventFlow
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class LifecycleEventFlowTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val owner = TestLifecycleOwner(coroutineDispatcher = dispatcher)
    private val testScope = TestScope(dispatcher)

    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @Test
    fun testLifecycleEventFlow() =
        testScope.runTest {
            val collectedEvents = mutableListOf<Lifecycle.Event>()
            val lifecycleEventFlow = owner.lifecycle.eventFlow
            backgroundScope.launch { lifecycleEventFlow.collect { collectedEvents.add(it) } }
            owner.currentState = Lifecycle.State.CREATED
            assertThat(collectedEvents)
                .containsExactly(
                    Lifecycle.Event.ON_CREATE,
                    Lifecycle.Event.ON_START,
                    Lifecycle.Event.ON_STOP
                )
                .inOrder()
        }

    @Test
    fun testEventFlowStopsCollectingAfterDestroyed() =
        testScope.runTest {
            val collectedEvents = mutableListOf<Lifecycle.Event>()
            val lifecycleEventFlow = owner.lifecycle.eventFlow
            val job =
                backgroundScope.launch { lifecycleEventFlow.collect { collectedEvents.add(it) } }

            owner.currentState = Lifecycle.State.CREATED
            owner.currentState = Lifecycle.State.DESTROYED
            owner.currentState = Lifecycle.State.RESUMED

            assertThat(job.isCompleted).isTrue()
            assertThat(collectedEvents)
                .containsExactly(
                    Lifecycle.Event.ON_CREATE,
                    Lifecycle.Event.ON_START,
                    Lifecycle.Event.ON_STOP,
                    Lifecycle.Event.ON_DESTROY
                )
                .inOrder()
        }

    @Test
    fun testEventFlowStopsCollectingAfterJobCancelled() =
        testScope.runTest {
            val collectedEvents = mutableListOf<Lifecycle.Event>()
            val lifecycleEventFlow = owner.lifecycle.eventFlow
            val job =
                backgroundScope.launch { lifecycleEventFlow.collect { collectedEvents.add(it) } }
            owner.currentState = Lifecycle.State.CREATED
            assertThat(collectedEvents)
                .containsExactly(
                    Lifecycle.Event.ON_CREATE,
                    Lifecycle.Event.ON_START,
                    Lifecycle.Event.ON_STOP,
                )
                .inOrder()
            collectedEvents.clear()
            job.cancel()
            owner.currentState = Lifecycle.State.RESUMED
            assertThat(collectedEvents).isEmpty()
        }
}
