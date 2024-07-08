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

package androidx.lifecycle.testing

import androidx.kruth.assertThat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleRegistry
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
class LifecycleRegistryTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED, dispatcher)
    private val testScope = TestScope(dispatcher)

    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @Test
    fun getCurrentState() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        assertThat(lifecycleOwner.currentState).isEqualTo(Lifecycle.State.RESUMED)

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(lifecycleOwner.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun getCurrentStateFlow() {
        val lifecycle = lifecycleOwner.lifecycle
        assertThat(lifecycle.currentStateFlow.value).isEqualTo(Lifecycle.State.INITIALIZED)

        lifecycleOwner.currentState = Lifecycle.State.CREATED
        assertThat(lifecycle.currentStateFlow.value).isEqualTo(Lifecycle.State.CREATED)

        lifecycleOwner.currentState = Lifecycle.State.DESTROYED
        assertThat(lifecycle.currentStateFlow.value).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun getCurrentStateFlowWithReentranceNoObservers() =
        testScope.runTest {
            val stateFlow = lifecycleOwner.lifecycle.currentStateFlow
            assertThat(lifecycleOwner.currentState).isEqualTo(Lifecycle.State.INITIALIZED)
            assertThat(stateFlow.value).isEqualTo(Lifecycle.State.INITIALIZED)

            backgroundScope.launch {
                stateFlow.collect { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE) }
            }

            assertThat(lifecycleOwner.currentState).isEqualTo(Lifecycle.State.CREATED)
            assertThat(stateFlow.value).isEqualTo(Lifecycle.State.CREATED)
        }

    @Test
    fun getCurrentStateFlowWithObserverReentrance() =
        testScope.runTest {
            val stateFlow = lifecycleOwner.lifecycle.currentStateFlow
            assertThat(lifecycleOwner.currentState).isEqualTo(Lifecycle.State.INITIALIZED)
            assertThat(stateFlow.value).isEqualTo(Lifecycle.State.INITIALIZED)

            lifecycleOwner.lifecycle.addObserver(
                LifecycleEventObserver { owner, _ ->
                    (owner.lifecycle as LifecycleRegistry).handleLifecycleEvent(
                        Lifecycle.Event.ON_RESUME
                    )
                }
            )

            backgroundScope.launch { stateFlow.collect {} }

            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            assertThat(lifecycleOwner.currentState).isEqualTo(Lifecycle.State.RESUMED)
            assertThat(stateFlow.value).isEqualTo(Lifecycle.State.RESUMED)
        }

    @Test
    fun getCurrentStateFlowWithObserverWithFlowReentrance() =
        testScope.runTest {
            val stateFlow = lifecycleOwner.lifecycle.currentStateFlow
            assertThat(lifecycleOwner.currentState).isEqualTo(Lifecycle.State.INITIALIZED)
            assertThat(stateFlow.value).isEqualTo(Lifecycle.State.INITIALIZED)

            lateinit var event: Lifecycle.Event
            lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, e -> event = e })

            backgroundScope.launch {
                stateFlow.collect { lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE) }
            }

            assertThat(lifecycleOwner.currentState).isEqualTo(Lifecycle.State.CREATED)
            assertThat(event).isEqualTo(Lifecycle.Event.ON_CREATE)
        }

    @Test
    fun observerCount() {
        lifecycleOwner.currentState = Lifecycle.State.STARTED
        assertThat(lifecycleOwner.observerCount).isEqualTo(0)
        lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, _ -> })
        assertThat(lifecycleOwner.observerCount).isEqualTo(1)
    }
}
